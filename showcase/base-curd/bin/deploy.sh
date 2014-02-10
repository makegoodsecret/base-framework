echo "[INFO] deploy jar to remote repository.";
cd ..;
exec mvn clean deploy;
cd bin;