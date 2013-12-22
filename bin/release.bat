
@echo off
echo [INFO] release project to github

cd %~dp0
cd ..

echo [INFO] release prepare.
call mvn release:clean release:prepare

cd bin
pause
