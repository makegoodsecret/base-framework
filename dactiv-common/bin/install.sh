echo [INFO] Install jar to local repository.
cd ..
exec mvn clean source:jar install -Dmaven.test.skip=true
cd bin