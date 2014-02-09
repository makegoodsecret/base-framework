set base_path=%cd%
echo [INFO] install jar to local m2 repository.
exec mvn clean source:jar install -Dmaven.test.skip=true
echo [INFO] create base curd project archetype.
cd %base_path%\showcase\base-curd
exec mvn archetype:create-from-project
echo [INFO] install base curd archetype to local m2 repository
cd %base_path%\showcase\base-curd\target\generated-sources\archetype
exec mvn clean install -Dmaven.test.skip=true
cd %base_path%\showcase\base-curd
rm -f -R target
echo [INFO] init h2 data
exec mvn antrun:run
echo [INFO] start base curd app
start "base curd" mvn clean jetty:run
echo [INFO] Please wait a moment. When you see "[INFO] Started Jetty Server", you can visit: http://localhost:8080/base-curd/ to view the demo
cd %base_path%