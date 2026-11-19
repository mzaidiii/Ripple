#!/usr/bin/env bash
# Launcher for Ripple CLI Chat Tool
if [ -z "$1" ]; then
    ./mvnw exec:java -Dexec.mainClass="org.personal.ripple.cli.RippleChatCli"
else
    ./mvnw exec:java -Dexec.mainClass="org.personal.ripple.cli.RippleChatCli" -Dexec.args="$*"
fi
