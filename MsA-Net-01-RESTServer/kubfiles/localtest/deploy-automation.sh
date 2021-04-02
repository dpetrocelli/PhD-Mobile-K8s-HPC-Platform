cd ..
cd ..
mvn package -Dmaven.test.skip=true 
cd kubfiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-restserver.jar
docker build -t dpetrocelli/dev-restserver:latest .
cd localtest
docker-compose -f dev-restserver.yaml down
docker-compose -f dev-restserver.yaml up
#docker login
#docker push dpetrocelli/dev-fileserver:latest