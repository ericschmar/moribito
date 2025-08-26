class Moribito < Formula
  desc "LDAP CLI Explorer - Interactive terminal-based LDAP client with TUI"
  homepage "https://github.com/ericschmar/moribito"
  version "0.2.3"
  license "MIT"

  on_macos do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-amd64"
      sha256 "eba3e298d6a9a7f43b005ec9bcaf603323f3ca67f6143791e4ed335fa29c94f5"
    end
    if Hardware::CPU.arm?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-arm64"  
      sha256 "3032b3b75c3cf3390afb70a5627a584d4f339f7d694dc5c811049dbc24bdff93"
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-amd64"
      sha256 "c317fe56fc2fd01b34f9f25ea2cee513c7cb49600184226e9a982ac8f62b4c5a"
    end
    if Hardware::CPU.arm? && Hardware::CPU.is_64_bit?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-arm64"
      sha256 "993250f43f8a5b94d1298bb7aae322b7b10afe39eabde4b6e22fa8e767878078"
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