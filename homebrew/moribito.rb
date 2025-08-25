class Moribito < Formula
  desc "LDAP CLI Explorer - Interactive terminal-based LDAP client with TUI"
  homepage "https://github.com/ericschmar/moribito"
  version "0.0.1"
  license "MIT"

  on_macos do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/ldap-cli-darwin-amd64"
      sha256 "c7702359e5bf0abce0b0f30925a25325fda592d6a5855bffe329f752a29e44df"
    end
    if Hardware::CPU.arm?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/ldap-cli-darwin-arm64"  
      sha256 "85cdbb5dbeae72400eafd74c635280a5a8f631a0228945ba2631f0afd15f1497"
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/ldap-cli-linux-amd64"
      sha256 "59b142fcd3b1ac7398efffc62349fb4262cb71e28220ec01e82672294180fde8"
    end
    if Hardware::CPU.arm? && Hardware::CPU.is_64_bit?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/ldap-cli-linux-arm64"
      sha256 "14c3592a5d1c8808733d6f0d4a6e1e72cb9555913f856499ae5d56fcb46bf013"
    end
  end

  def install
    # For v0.0.1, binaries are named ldap-cli-*
    # For future versions, they should be named moribito-*
    if version == "0.0.1"
      bin.install "ldap-cli-#{OS.kernel_name.downcase}-#{Hardware::CPU.arch}" => "moribito"
    else
      bin.install "moribito-#{OS.kernel_name.downcase}-#{Hardware::CPU.arch}" => "moribito"
    end
  end

  test do
    # Test version output
    output = shell_output("#{bin}/moribito --version")
    assert_match "moribito version #{version}", output
    
    # Test help output  
    output = shell_output("#{bin}/moribito --help")
    assert_match "LDAP CLI Explorer", output
  end
end
