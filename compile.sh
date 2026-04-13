#!/usr/bin/env bash
# Compiles both the server and client into the 'out/' directory.
set -e

mkdir -p out

echo "Compiling server..."
javac -d out src/server/ChatServer.java src/server/ClientHandler.java

echo "Compiling client..."
javac -d out src/client/ChatClient.java

echo "Compiling tests..."
javac -cp out -d out test/ChatIntegrationTest.java

echo ""
echo "Build successful.  Compiled classes are in: out/"
echo ""
echo "To run:"
echo "  Start the server:  ./run_server.sh"
echo "  Start a client:    ./run_client.sh"
echo ""
echo "To run integration tests:"
echo "  java -ea -cp out ChatIntegrationTest"
