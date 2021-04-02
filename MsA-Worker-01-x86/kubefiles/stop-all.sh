#kubectl delete -f dev-workerx86-deployment.yaml --namespace=dp-microservices
kubectl delete -f internet-worker-deployment.yaml --namespace=dp-microservices
kubectl delete -f ../../DEV-JOINERv1/kubefiles/dev-joiner-deployment.yaml --namespace=dp-microservices
kubectl delete -f ../../DEV-RESTServer/kubfiles/dev-restserver-deployment.yaml --namespace=dp-microservices
kubectl delete hpa autoscaler-devrestserver --namespace=dp-microservices
kubectl delete hpa autoscaler-devworkerx86 --namespace=dp-microservices
kubectl delete -f defining-dp-microservices-quota.yaml --namespace=dp-microservices
#kubectl delete namespace dp-microservices