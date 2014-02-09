echo [INFO] archetype create from project;

if [ ! -e generated-sources ] ; then
    mkdir generated-sources
fi
cd generated-sources
exec mvn archetype:generate -DarchetypeCatalog=local -DarchetypeGroupId=com.github.dactiv.showcase -DarchetypeArtifactId=base-curd-archetype -DarchetypeVersion=1.0.0-SNAPSHOT
cd ..