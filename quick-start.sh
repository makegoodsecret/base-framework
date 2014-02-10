base_path=$(cd "$(dirname "$0")"; pwd);

echo "[INFO] install jar to local m2 repository";
mvn clean source:jar install -Dmaven.test.skip=true;

echo "[INFO] create base curd project archetype";
cd ${base_path}/showcase/base-curd;
mvn archetype:create-from-project;

echo "[INFO] install base curd archetype to local m2 repository";
cd ${base_path}/showcase/base-curd/target/generated-sources/archetype;
mvn clean install -Dmaven.test.skip=true;

echo "[INFO] delete base curd project generated-sources target";
cd ${base_path}/showcase/base-curd;
rm -f -R target;

echo "[INFO] init h2 data";
mvn antrun:run;

echo [INFO] "start base curd app"
cd ${base_path}/showcase/base-curd;
mvn clean jetty:run;