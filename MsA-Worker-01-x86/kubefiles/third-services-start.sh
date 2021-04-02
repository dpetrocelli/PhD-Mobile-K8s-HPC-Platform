# Basic services
helm install mariadb-cluster --set rootUser.password=Osito1104**,db.name=distributedProcessing,db.user=david,db.password=david,replicaCount=1,service.type=LoadBalancer bitnami/mariadb-galera
helm install rabbitmq-cluster --set rabbitmq.username=admin,rabbitmq.password=admin,rabbitmq.erlangCookie=erlangCookie,replicas=1,service.type=LoadBalancer bitnami/rabbitmq