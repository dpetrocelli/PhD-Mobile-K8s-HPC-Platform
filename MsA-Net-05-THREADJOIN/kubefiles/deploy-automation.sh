cd ..
mvn package
cd kubefiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-threadunifier.jar
docker build -t dpetrocelli/dev-threadunifier:latest .
docker login
docker push dpetrocelli/dev-threadunifier
