cd ..
cd ..
mvn package -Dmaven.test.skip=true 
cd kubefiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-joiner.jar
docker build -t dpetrocelli/dev-joiner:latest .
cd localtest
docker-compose -f joiner.yaml down
docker-compose -f joiner.yaml up
#docker login
#docker push dpetrocelli/dev-fileserver:latest

