package ssh

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"encoding/pem"
	"io"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"golang.org/x/crypto/ssh"
)

// ---- pure helper tests ----

func TestExpandPath_Tilde(t *testing.T) {
	home, err := os.UserHomeDir()
	if err != nil {
		t.Skip("cannot determine home dir:", err)
	}

	got := expandPath("~/.ssh/id_rsa")
	want := filepath.Join(home, ".ssh/id_rsa")
	if got != want {
		t.Errorf("expandPath: got %q, want %q", got, want)
	}
}

func TestExpandPath_Absolute(t *testing.T) {
	path := "/etc/ssh/id_rsa"
	if got := expandPath(path); got != path {
		t.Errorf("expandPath absolute: got %q, want %q", got, path)
	}
}

func TestExpandPath_Relative(t *testing.T) {
	path := "keys/id_rsa"
	if got := expandPath(path); got != path {
		t.Errorf("expandPath relative: got %q, want %q", got, path)
	}
}

func TestEffectivePort_Zero(t *testing.T) {
	if got := effectivePort(0); got != 22 {
		t.Errorf("effectivePort(0): got %d, want 22", got)
	}
}

func TestEffectivePort_Negative(t *testing.T) {
	if got := effectivePort(-1); got != 22 {
		t.Errorf("effectivePort(-1): got %d, want 22", got)
	}
}

func TestEffectivePort_Custom(t *testing.T) {
	if got := effectivePort(2222); got != 2222 {
		t.Errorf("effectivePort(2222): got %d, want 2222", got)
	}
}

func TestBuildAuthMethods_Password(t *testing.T) {
	methods, err := buildAuthMethods("password", "secret", "", "")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(methods) != 1 {
		t.Fatalf("expected 1 auth method, got %d", len(methods))
	}
}

func TestBuildAuthMethods_PasswordCaseInsensitive(t *testing.T) {
	_, err := buildAuthMethods("PASSWORD", "secret", "", "")
	if err != nil {
		t.Fatalf("unexpected error for uppercase method: %v", err)
	}
}

func TestBuildAuthMethods_UnsupportedMethod(t *testing.T) {
	_, err := buildAuthMethods("kerberos", "", "", "")
	if err == nil {
		t.Error("expected error for unsupported auth method")
	}
	if !strings.Contains(err.Error(), "unsupported auth method") {
		t.Errorf("error should mention unsupported auth method, got: %v", err)
	}
}

func TestBuildAuthMethods_KeyMissingFile(t *testing.T) {
	_, err := buildAuthMethods("key", "", "/nonexistent/key", "")
	if err == nil {
		t.Error("expected error for missing key file")
	}
}

func TestBuildAuthMethods_KeyWithValidFile(t *testing.T) {
	keyPath := generateTestKey(t)

	methods, err := buildAuthMethods("key", "", keyPath, "")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(methods) != 1 {
		t.Fatalf("expected 1 auth method, got %d", len(methods))
	}
}

func TestBuildAuthMethods_AgentNoSocket(t *testing.T) {
	t.Setenv("SSH_AUTH_SOCK", "")

	_, err := buildAuthMethods("agent", "", "", "")
	if err == nil {
		t.Error("expected error when SSH_AUTH_SOCK is unset")
	}
	if !strings.Contains(err.Error(), "SSH_AUTH_SOCK") {
		t.Errorf("error should mention SSH_AUTH_SOCK, got: %v", err)
	}
}

// ---- integration test using an in-process SSH server ----

func TestNewTunnel_DirectPassword(t *testing.T) {
	// Start a minimal in-process SSH server
	sshAddr, stopSSH := startTestSSHServer(t, "testuser", "testpass", nil)

	// Start a simple TCP echo server to tunnel to
	echoAddr, stopEcho := startEchoServer(t)
	defer stopEcho()
	defer stopSSH()

	host, port := splitHostPort(t, sshAddr)
	echoHost, echoPort := splitHostPortInt(t, echoAddr)

	tunnel, err := NewTunnel(TunnelConfig{
		SSHHost:         host,
		SSHPort:         port,
		SSHUser:         "testuser",
		AuthMethod:      "password",
		Password:        "testpass",
		RemoteHost:      echoHost,
		RemotePort:      echoPort,
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	})
	if err != nil {
		t.Fatalf("NewTunnel: %v", err)
	}
	defer tunnel.Close() //nolint:errcheck

	if tunnel.LocalPort() == 0 {
		t.Error("expected non-zero local port")
	}
	if !strings.HasPrefix(tunnel.LocalAddr(), "127.0.0.1:") {
		t.Errorf("unexpected local addr: %s", tunnel.LocalAddr())
	}

	// Verify the tunnel actually forwards data
	conn, err := net.Dial("tcp", tunnel.LocalAddr())
	if err != nil {
		t.Fatalf("dial tunnel: %v", err)
	}
	defer conn.Close() //nolint:errcheck

	msg := []byte("hello tunnel")
	if _, err := conn.Write(msg); err != nil {
		t.Fatalf("write: %v", err)
	}

	buf := make([]byte, len(msg))
	if _, err := io.ReadFull(conn, buf); err != nil {
		t.Fatalf("read: %v", err)
	}
	if string(buf) != string(msg) {
		t.Errorf("echo mismatch: got %q, want %q", buf, msg)
	}
}

func TestNewTunnel_DirectKey(t *testing.T) {
	keyPath := generateTestKey(t)
	pubKey := loadPublicKey(t, keyPath)

	sshAddr, stopSSH := startTestSSHServer(t, "testuser", "", pubKey)
	defer stopSSH()

	echoAddr, stopEcho := startEchoServer(t)
	defer stopEcho()

	host, port := splitHostPort(t, sshAddr)
	echoHost, echoPort := splitHostPortInt(t, echoAddr)

	tunnel, err := NewTunnel(TunnelConfig{
		SSHHost:         host,
		SSHPort:         port,
		SSHUser:         "testuser",
		AuthMethod:      "key",
		KeyFile:         keyPath,
		RemoteHost:      echoHost,
		RemotePort:      echoPort,
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	})
	if err != nil {
		t.Fatalf("NewTunnel with key auth: %v", err)
	}
	defer tunnel.Close() //nolint:errcheck

	if tunnel.LocalPort() == 0 {
		t.Error("expected non-zero local port")
	}
}

func TestNewTunnel_Close(t *testing.T) {
	sshAddr, stopSSH := startTestSSHServer(t, "testuser", "testpass", nil)
	defer stopSSH()

	echoAddr, stopEcho := startEchoServer(t)
	defer stopEcho()

	host, port := splitHostPort(t, sshAddr)
	echoHost, echoPort := splitHostPortInt(t, echoAddr)

	tunnel, err := NewTunnel(TunnelConfig{
		SSHHost:         host,
		SSHPort:         port,
		SSHUser:         "testuser",
		AuthMethod:      "password",
		Password:        "testpass",
		RemoteHost:      echoHost,
		RemotePort:      echoPort,
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	})
	if err != nil {
		t.Fatalf("NewTunnel: %v", err)
	}

	localAddr := tunnel.LocalAddr()

	if err := tunnel.Close(); err != nil {
		t.Fatalf("Close: %v", err)
	}

	// After Close, new connections should fail
	if _, err := net.Dial("tcp", localAddr); err == nil {
		t.Error("expected dial to fail after tunnel closed")
	}
}

func TestNewTunnel_BadSSHHost(t *testing.T) {
	_, err := NewTunnel(TunnelConfig{
		SSHHost:    "127.0.0.1",
		SSHPort:    1, // port 1 won't have an SSH server
		SSHUser:    "user",
		AuthMethod: "password",
		Password:   "pass",
		RemoteHost: "127.0.0.1",
		RemotePort: 389,
	})
	if err == nil {
		t.Error("expected error connecting to non-existent SSH host")
	}
}

// ---- test helpers ----

// startTestSSHServer starts a minimal in-process SSH server and returns its address
// and a stop function. Pass either a non-empty password or a pubKey (but not both).
func startTestSSHServer(t *testing.T, user, password string, authorizedKey ssh.PublicKey) (addr string, stop func()) {
	t.Helper()

	serverKey := generateServerKey(t)
	signer, err := ssh.NewSignerFromKey(serverKey)
	if err != nil {
		t.Fatalf("new server signer: %v", err)
	}

	cfg := &ssh.ServerConfig{
		NoClientAuth: false,
	}
	cfg.AddHostKey(signer)

	if password != "" {
		cfg.PasswordCallback = func(c ssh.ConnMetadata, pass []byte) (*ssh.Permissions, error) {
			if c.User() == user && string(pass) == password {
				return nil, nil
			}
			return nil, &authError{}
		}
	}

	if authorizedKey != nil {
		cfg.PublicKeyCallback = func(c ssh.ConnMetadata, key ssh.PublicKey) (*ssh.Permissions, error) {
			if c.User() == user && authorizedKeysMatch(key, authorizedKey) {
				return nil, nil
			}
			return nil, &authError{}
		}
	}

	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}

	done := make(chan struct{})
	go func() {
		defer close(done)
		for {
			conn, err := listener.Accept()
			if err != nil {
				return
			}
			go handleTestSSHConn(conn, cfg)
		}
	}()

	return listener.Addr().String(), func() {
		listener.Close() //nolint:errcheck
		<-done
	}
}

type authError struct{}

func (e *authError) Error() string { return "authentication failed" }

func authorizedKeysMatch(a, b ssh.PublicKey) bool {
	return string(a.Marshal()) == string(b.Marshal())
}

func handleTestSSHConn(conn net.Conn, cfg *ssh.ServerConfig) {
	sshConn, chans, reqs, err := ssh.NewServerConn(conn, cfg)
	if err != nil {
		return
	}
	defer sshConn.Close() //nolint:errcheck
	go ssh.DiscardRequests(reqs)

	for newChan := range chans {
		if newChan.ChannelType() != "direct-tcpip" {
			newChan.Reject(ssh.UnknownChannelType, "unknown channel type") //nolint:errcheck
			continue
		}

		// Parse the forwarding destination from the channel extra data
		var fwd struct {
			DestAddr string
			DestPort uint32
			SrcAddr  string
			SrcPort  uint32
		}
		if err := ssh.Unmarshal(newChan.ExtraData(), &fwd); err != nil {
			newChan.Reject(ssh.ConnectionFailed, "bad extra data") //nolint:errcheck
			continue
		}

		ch, reqs, err := newChan.Accept()
		if err != nil {
			continue
		}
		go ssh.DiscardRequests(reqs)

		// Forward the channel to the target address
		target, err := net.Dial("tcp", net.JoinHostPort(fwd.DestAddr, itoa(fwd.DestPort)))
		if err != nil {
			ch.Close() //nolint:errcheck
			continue
		}

		go func() {
			defer ch.Close()       //nolint:errcheck
			defer target.Close()   //nolint:errcheck
			go io.Copy(target, ch) //nolint:errcheck
			io.Copy(ch, target)    //nolint:errcheck
		}()
	}
}

// startEchoServer starts a TCP echo server and returns its address and stop function
func startEchoServer(t *testing.T) (addr string, stop func()) {
	t.Helper()

	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("echo listen: %v", err)
	}

	done := make(chan struct{})
	go func() {
		defer close(done)
		for {
			conn, err := listener.Accept()
			if err != nil {
				return
			}
			go io.Copy(conn, conn) //nolint:errcheck
		}
	}()

	return listener.Addr().String(), func() {
		listener.Close() //nolint:errcheck
		<-done
	}
}

func generateTestKey(t *testing.T) string {
	t.Helper()
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatalf("generate key: %v", err)
	}

	block, err := ssh.MarshalPrivateKey(key, "")
	if err != nil {
		t.Fatalf("marshal private key: %v", err)
	}
	pemData := pem.EncodeToMemory(block)

	dir := t.TempDir()
	path := filepath.Join(dir, "id_ecdsa")
	if err := os.WriteFile(path, pemData, 0600); err != nil {
		t.Fatalf("write key: %v", err)
	}
	return path
}

func generateServerKey(t *testing.T) *ecdsa.PrivateKey {
	t.Helper()
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatalf("generate server key: %v", err)
	}
	return key
}

func loadPublicKey(t *testing.T, keyPath string) ssh.PublicKey {
	t.Helper()
	data, err := os.ReadFile(keyPath)
	if err != nil {
		t.Fatalf("read key file: %v", err)
	}
	signer, err := ssh.ParsePrivateKey(data)
	if err != nil {
		t.Fatalf("parse private key: %v", err)
	}
	return signer.PublicKey()
}

func splitHostPort(t *testing.T, addr string) (host string, port int) {
	t.Helper()
	h, p, err := net.SplitHostPort(addr)
	if err != nil {
		t.Fatalf("split host port %q: %v", addr, err)
	}
	return h, atoiPort(t, p)
}

func splitHostPortInt(t *testing.T, addr string) (host string, port int) {
	return splitHostPort(t, addr)
}

func atoiPort(t *testing.T, s string) int {
	t.Helper()
	n := 0
	for _, c := range s {
		if c < '0' || c > '9' {
			t.Fatalf("invalid port %q", s)
		}
		n = n*10 + int(c-'0')
	}
	return n
}

func itoa(n uint32) string {
	if n == 0 {
		return "0"
	}
	digits := make([]byte, 0, 5)
	for n > 0 {
		digits = append([]byte{byte('0' + n%10)}, digits...)
		n /= 10
	}
	return string(digits)
}
