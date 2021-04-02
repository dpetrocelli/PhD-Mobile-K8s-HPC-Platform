cd ..
mvn package
cd kubefiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-workerx86-internet.jar
docker build -t dpetrocelli/dev-workerx86-internet:latest .
docker login
docker push dpetrocelli/dev-workerx86-internet