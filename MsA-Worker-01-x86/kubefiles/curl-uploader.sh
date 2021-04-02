# Curl uploader 
kubectl delete -f ../../docker-uploader-cmd/my-curl-deployment.yaml
../../docker-uploader-cmd/deploy-automation.sh 
kubectl create -f ../../docker-uploader-cmd/my-curl-deployment.yaml