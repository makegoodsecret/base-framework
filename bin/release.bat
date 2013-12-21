
@echo off
echo [INFO] create project to eclipse

cd %~dp0
cd ..

echo [INFO] clean.
call release:clean

echo [INFO] prepare.
call release:prepare

cd bin
pause
