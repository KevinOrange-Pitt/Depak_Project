#!/usr/bin/env bash
# Starts a chat client GUI.  Each teammate runs this on their own machine.
set -e

if [ ! -d out ]; then
    echo "Classes not found.  Run ./compile.sh first."
    exit 1
fi

java -cp out client.ChatClient
