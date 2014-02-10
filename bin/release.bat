
@echo off

echo [INFO] release project

cd ..

set base_path=%cd%

echo [INFO] release clean project
call mvn release:clean
call mvn release:prepare
call mvn release:perform

cd %base_path%
echo [INFO] push to github
call git push origin master

echo [INFO] push tag to github
call git push --tags

pause
