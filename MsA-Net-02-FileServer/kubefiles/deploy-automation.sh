cd ..
mvn package -Dmaven.test.skip=true 
cd kubefiles
cp ../target/file-0.0.1-SNAPSHOT.jar dev-fileserver.jar
docker build -t dpetrocelli/dev-fileserver:latest .
docker login
docker push dpetrocelli/dev-fileserver:latest