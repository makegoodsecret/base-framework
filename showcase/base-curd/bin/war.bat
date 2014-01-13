@echo off
echo [INFO] Use maven war-plugin exprot war.

cd %~dp0
cd ..

call mvn war:war

cd bin
pause