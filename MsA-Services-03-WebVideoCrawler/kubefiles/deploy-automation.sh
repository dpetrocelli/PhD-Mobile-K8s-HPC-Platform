cd ..
mvn package -Dmaven.test.skip=true 
cd kubefiles
cp ../target/demo-0.0.1-SNAPSHOT.jar demo-0.0.1-SNAPSHOT.jar
#docker build -t dpetrocelli/dev-downloader.jar:latest .
#docker login
#docker push dpetrocelli/dev-downloader.jar:latest
