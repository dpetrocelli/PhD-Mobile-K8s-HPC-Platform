#Create namespace and quota
# [STEP 1] - CONFIGURATION for fixed Workload and 2 Worker nodes 
kubectl create namespace dp-microservices
# NO SERVICE QUOTA #kubectl apply -f defining-dp-microservices-quota.yaml --namespace=dp-microservices
#Create REST SERVER Structure (scaler - service - deployment)

# NO AUTOSCALER #kubectl apply -f ../../DEV-RESTServer/kubfiles/defining-autoscaler-deployment.yaml --namespace=dp-microservices
kubectl create -f ../../DEV-RESTServer/kubfiles/dev-restserver-service.yaml --namespace=dp-microservices
kubectl create -f ../../DEV-RESTServer/kubfiles/dev-restserver-deployment.yaml --namespace=dp-microservices
# Create Joiner Deployment
kubectl create -f ../../DEV-JOINERv1/kubefiles/dev-joiner-deployment.yaml --namespace=dp-microservices
# Create Worker x 86 Structure (Autoscaler - Deployment)
#bash deploy-automation.sh
# NO AUTOSCALE #kubectl apply -f defining-autoscaler-deployment.yaml --namespace=dp-microservices
kubectl create -f internet-worker-deployment.yaml --namespace=dp-microservices