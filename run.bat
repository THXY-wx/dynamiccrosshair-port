@echo off
setlocal
set "HERE=%~dp0"
set "GRADLE_USER_HOME=%HERE%..\classicbar-port\gradle-home"
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "GRADLE=%HERE%..\classicbar-port\gradle-home\wrapper\dists\gradle-9.7.1-bin\brhrg0o7dy6fa4wpk3ipunw7x\gradle-9.7.1\bin\gradle.bat"
cd /d "%HERE%"
call "%GRADLE%" %*
