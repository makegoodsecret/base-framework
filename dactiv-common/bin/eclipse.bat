@echo off
echo [INFO] create eclipse project.

cd %~dp0
cd ..
call mvn eclipse:eclipse
cd bin
pause