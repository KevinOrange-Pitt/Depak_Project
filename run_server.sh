#!/usr/bin/env bash
# Starts the chat server.  Run this once before launching any clients.
set -e

if [ ! -d out ]; then
    echo "Classes not found.  Run ./compile.sh first."
    exit 1
fi

java -cp out server.ChatServer
