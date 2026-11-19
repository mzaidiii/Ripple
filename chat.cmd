@echo off
rem Launcher for Ripple CLI Chat Tool
if "%1"=="" (
    .\mvnw.cmd exec:java -Dexec.mainClass="org.personal.ripple.cli.RippleChatCli"
) else (
    .\mvnw.cmd exec:java -Dexec.mainClass="org.personal.ripple.cli.RippleChatCli" -Dexec.args="%*"
)
