
@echo off
echo [INFO] create myeclipse project

cd %~dp0
cd ..
call mvn clean eclipse:myeclipse
cd bin
pause
