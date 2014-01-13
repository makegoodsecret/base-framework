
@echo off
echo [INFO] create eclipse project

cd %~dp0
cd ..
call mvn clean eclipse:eclipse
cd bin
pause
