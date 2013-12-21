
@echo off
echo [INFO] release project to github

cd %~dp0
cd ..

echo [INFO] clean.
call mvn release:clean

echo [INFO] prepare.
call mvn release:prepare

cd bin
pause
