cd ..
mvn package -Dmaven.test.skip=true 
cd kubfiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-restserver.jar
docker build -t dpetrocelli/dev-restserver2:latest .
docker login
docker push dpetrocelli/dev-restserver2:latest
