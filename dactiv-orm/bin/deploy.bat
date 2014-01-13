@echo off
echo [INFO] deploy jar to remote repository.

cd %~dp0
cd ..
call mvn clean deploy
cd bin
pause