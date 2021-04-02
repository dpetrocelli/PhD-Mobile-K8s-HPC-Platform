cd ..
mvn package
cd kubefiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-joiner.jar
docker build -t dpetrocelli/dev-joiner:latest .
docker login
docker push dpetrocelli/dev-joiner
