
@echo off
echo [INFO] create myeclipse project

cd %~dp0
cd ..
call mvn eclipse:myeclipse
cd bin
pause
