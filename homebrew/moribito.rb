class Moribito < Formula
  desc "LDAP CLI Explorer - Interactive terminal-based LDAP client with TUI"
  homepage "https://github.com/ericschmar/moribito"
  version "1.0.0"
  license "MIT"

  on_macos do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-amd64"
      sha256 ""
    end
    if Hardware::CPU.arm?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-arm64"  
      sha256 ""
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-amd64"
      sha256 ""
    end
    if Hardware::CPU.arm? && Hardware::CPU.is_64_bit?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-arm64"
      sha256 ""
    end
  end

  def install
    bin.install "moribito-#{OS.kernel_name.downcase}-#{Hardware::CPU.arch}" => "moribito"
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