@echo off
echo [INFO] create idea project

cd %~dp0
cd ..
call mvn clean idea:idea
cd bin
pause