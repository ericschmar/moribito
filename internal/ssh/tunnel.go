package ssh

import (
	"errors"
	"fmt"
	"io"
	"net"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"golang.org/x/crypto/ssh"
	"golang.org/x/crypto/ssh/agent"
	"golang.org/x/crypto/ssh/knownhosts"

	"github.com/ericschmar/moribito/internal/debug"
)

// HostKeyUnknownError is returned when the SSH host key is not in known_hosts.
// The TUI uses this to show a helpful error dialog instead of a raw error string.
type HostKeyUnknownError struct {
	Host string
}

func (e *HostKeyUnknownError) Error() string {
	return fmt.Sprintf("SSH host key for %q is not in known_hosts", e.Host)
}

// TunnelConfig contains the parameters needed to establish an SSH tunnel
type TunnelConfig struct {
	SSHHost       string
	SSHPort       int
	SSHUser       string
	AuthMethod    string // "password", "key", "agent"
	Password      string
	KeyFile       string
	KeyPassphrase string

	// The remote endpoint to forward to (the LDAP server as seen from the SSH host)
	RemoteHost string
	RemotePort int

	// InsecureIgnoreHostKey disables host key verification entirely (equivalent to
	// StrictHostKeyChecking=no). Only set this when you trust the network path.
	InsecureIgnoreHostKey bool

	// HostKeyCallback overrides the default known_hosts-based verification when set.
	// Intended for testing; leave nil for production behaviour.
	HostKeyCallback ssh.HostKeyCallback
}

// Tunnel manages an SSH tunnel that forwards local connections to a remote endpoint
type Tunnel struct {
	listener  net.Listener
	sshClient *ssh.Client
	done      chan struct{}
	wg        sync.WaitGroup
}

// NewTunnel establishes an SSH tunnel and returns a Tunnel whose LocalAddr()
// can be used to reach the remote endpoint.
func NewTunnel(cfg TunnelConfig) (*Tunnel, error) {
	debug.Log("ssh/tunnel: NewTunnel starting — ssh=%s:%d user=%s auth=%s key=%q",
		cfg.SSHHost, effectivePort(cfg.SSHPort), cfg.SSHUser, cfg.AuthMethod, cfg.KeyFile)

	hkc := cfg.HostKeyCallback
	if hkc == nil {
		if cfg.InsecureIgnoreHostKey {
			hkc = ssh.InsecureIgnoreHostKey()
		} else {
			hkc = hostKeyCallback()
		}
	}

	sshAuth, err := buildAuthMethods(cfg.AuthMethod, cfg.Password, cfg.KeyFile, cfg.KeyPassphrase)
	if err != nil {
		debug.Log("ssh/tunnel: buildAuthMethods error: %v", err)
		return nil, fmt.Errorf("SSH auth: %w", err)
	}

	sshAddr := fmt.Sprintf("%s:%d", cfg.SSHHost, effectivePort(cfg.SSHPort))
	debug.Log("ssh/tunnel: dialing ssh host %s as %s", sshAddr, cfg.SSHUser)
	sshConfig := &ssh.ClientConfig{
		User:            cfg.SSHUser,
		Auth:            sshAuth,
		HostKeyCallback: hkc,
	}

	sshClient, err := ssh.Dial("tcp", sshAddr, sshConfig)
	if err != nil {
		debug.Log("ssh/tunnel: ssh dial error: %v", err)
		return nil, fmt.Errorf("connect to SSH host %s: %w", sshAddr, err)
	}
	debug.Log("ssh/tunnel: ssh host connected")

	// Open a local listener on a random port
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		sshClient.Close() //nolint:errcheck
		debug.Log("ssh/tunnel: local listener error: %v", err)
		return nil, fmt.Errorf("open local listener: %w", err)
	}

	t := &Tunnel{
		listener:  listener,
		sshClient: sshClient,
		done:      make(chan struct{}),
	}

	remoteAddr := fmt.Sprintf("%s:%d", cfg.RemoteHost, cfg.RemotePort)
	debug.Log("ssh/tunnel: listening on %s, forwarding to %s", t.LocalAddr(), remoteAddr)
	t.wg.Add(1)
	go t.accept(remoteAddr)

	return t, nil
}

// LocalAddr returns the local address of the tunnel listener (e.g. "127.0.0.1:54321")
func (t *Tunnel) LocalAddr() string {
	return t.listener.Addr().String()
}

// LocalPort returns the port number of the local listener
func (t *Tunnel) LocalPort() int {
	return t.listener.Addr().(*net.TCPAddr).Port
}

// Close tears down the tunnel, closing the listener and SSH connections
func (t *Tunnel) Close() error {
	close(t.done)
	t.listener.Close()  //nolint:errcheck
	t.sshClient.Close() //nolint:errcheck
	t.wg.Wait()
	return nil
}

func (t *Tunnel) accept(remoteAddr string) {
	defer t.wg.Done()
	for {
		conn, err := t.listener.Accept()
		if err != nil {
			select {
			case <-t.done:
				return
			default:
				return
			}
		}
		t.wg.Add(1)
		go t.forward(conn, remoteAddr)
	}
}

func (t *Tunnel) forward(local net.Conn, remoteAddr string) {
	defer t.wg.Done()
	defer local.Close() //nolint:errcheck

	remote, err := t.sshClient.Dial("tcp", remoteAddr)
	if err != nil {
		return
	}
	defer remote.Close() //nolint:errcheck

	var wg sync.WaitGroup
	wg.Add(2)

	// Each goroutine closes both sides when done so the peer goroutine unblocks.
	go func() {
		defer wg.Done()
		io.Copy(remote, local) //nolint:errcheck
		remote.Close()         //nolint:errcheck
		local.Close()          //nolint:errcheck
	}()
	go func() {
		defer wg.Done()
		io.Copy(local, remote) //nolint:errcheck
		local.Close()          //nolint:errcheck
		remote.Close()         //nolint:errcheck
	}()

	wg.Wait()
}

func buildAuthMethods(method, password, keyFile, keyPassphrase string) ([]ssh.AuthMethod, error) {
	switch strings.ToLower(method) {
	case "password":
		return []ssh.AuthMethod{ssh.Password(password)}, nil
	case "key":
		signer, err := loadKey(keyFile, keyPassphrase)
		if err != nil {
			return nil, err
		}
		return []ssh.AuthMethod{ssh.PublicKeys(signer)}, nil
	case "agent":
		agentClient, err := sshAgent()
		if err != nil {
			return nil, err
		}
		return []ssh.AuthMethod{ssh.PublicKeysCallback(agentClient.Signers)}, nil
	default:
		return nil, fmt.Errorf("unsupported auth method: %q (use \"password\", \"key\", or \"agent\")", method)
	}
}

func loadKey(keyFile, passphrase string) (ssh.Signer, error) {
	path := expandPath(keyFile)
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read key file %s: %w", path, err)
	}

	if passphrase != "" {
		signer, err := ssh.ParsePrivateKeyWithPassphrase(data, []byte(passphrase))
		if err != nil {
			return nil, fmt.Errorf("parse key file %s with passphrase: %w", path, err)
		}
		return signer, nil
	}

	signer, err := ssh.ParsePrivateKey(data)
	if err != nil {
		return nil, fmt.Errorf("parse key file %s: %w", path, err)
	}
	return signer, nil
}

func sshAgent() (agent.Agent, error) {
	sock := os.Getenv("SSH_AUTH_SOCK")
	if sock == "" {
		return nil, fmt.Errorf("SSH_AUTH_SOCK not set; ssh-agent is not available")
	}
	conn, err := net.Dial("unix", sock)
	if err != nil {
		return nil, fmt.Errorf("connect to ssh-agent at %s: %w", sock, err)
	}
	return agent.NewClient(conn), nil
}

func expandPath(path string) string {
	if strings.HasPrefix(path, "~/") {
		if home, err := os.UserHomeDir(); err == nil {
			return filepath.Join(home, path[2:])
		}
	}
	return path
}

func effectivePort(port int) int {
	if port <= 0 {
		return 22
	}
	return port
}

func hostKeyCallback() ssh.HostKeyCallback {
	if home, err := os.UserHomeDir(); err == nil {
		knownHostsPath := filepath.Join(home, ".ssh", "known_hosts")
		if cb, err := knownhosts.New(knownHostsPath); err == nil {
			return func(hostname string, remote net.Addr, key ssh.PublicKey) error {
				err := cb(hostname, remote, key)
				if err == nil {
					return nil
				}
				// Empty Want slice = host not in known_hosts at all; wrap as
				// HostKeyUnknownError so the TUI can show a helpful dialog.
				var ke *knownhosts.KeyError
				if errors.As(err, &ke) && len(ke.Want) == 0 {
					debug.Log("ssh/tunnel: host %s not in known_hosts", hostname)
					return &HostKeyUnknownError{Host: hostname}
				}
				return err
			}
		}
	}
	return ssh.InsecureIgnoreHostKey()
}
