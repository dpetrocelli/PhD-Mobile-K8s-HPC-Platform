cd ..
cd ..
mvn package -Dmaven.test.skip=true 
cd kubefiles
cp ../target/ex1-0.0.1-SNAPSHOT.jar dev-workerx86.jar
docker build -t dpetrocelli/dev-workerx86:latest .
#cd localtest
#docker-compose -f worker.yaml down
#docker-compose -f worker.yaml up
docker login
docker push dpetrocelli/dev-workerx86:latest

