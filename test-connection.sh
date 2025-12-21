#!/bin/bash
# Test script to run Moribito with detailed logging

echo "🔍 Running Moribito with detailed logging..."
echo "📋 Logs will show the complete connection flow:"
echo "   1. ConfigView - Connection attempt"
echo "   2. BrowserView - Initialization check"
echo "   3. TreeView - Tree reload"
echo "   4. LdapClient - LDAP search"
echo ""

cd "$(dirname "$0")/moribito-rs"

# Run with INFO level logging (default)
# Use RUST_LOG=debug for even more detailed output
RUST_LOG=debug cargo run --release --bin moribito

# To run with debug logging instead:
# RUST_LOG=debug cargo run --release --bin moribito
