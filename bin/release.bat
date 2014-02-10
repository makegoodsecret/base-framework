
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

echo [INFO] create base curd project archetype
cd %base_path%\showcase\base-curd
call mvn archetype:create-from-project

echo [INFO] release base curd project archetype
cd %base_path%\showcase\base-curd\target\generated-sources\archetype
call mvn clean install -Dmaven.test.skip=true
call mvn release:clean
call mvn release:prepare
call mvn release:perform

echo [INFO] delete base curd project generated-sources target
cd %base_path%\showcase\base-curd
rd /S /Q target

pause
