cd ..
cd ..
mvn package -Dmaven.test.skip=true 
cd kubefiles
cp ../target/file-0.0.1-SNAPSHOT.jar dev-fileserver.jar
docker build -t dpetrocelli/dev-fileserver:latest .
cd localtest
#docker-compose -f fileserver.yaml down
#docker-compose -f fileserver.yaml up
docker login
docker push dpetrocelli/dev-fileserver:latest