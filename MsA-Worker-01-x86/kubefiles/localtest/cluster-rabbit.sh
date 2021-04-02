mkdir /tmp/rabbit; mkdir /tmp/rabbit/stats ; mkdir /tmp/rabbit/node1 ; mkdir /tmp/rabbit/node2 ; sudo chmod 777 -R /tmp/rabbit
docker-compose -f cluster-rabbit.yaml down
docker-compose -f cluster-rabbit.yaml up