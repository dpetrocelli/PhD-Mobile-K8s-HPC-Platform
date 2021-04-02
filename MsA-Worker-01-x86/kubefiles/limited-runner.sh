initTime="10:00"
endTime="23:00"
cpus="4.0"
cpusetCpus="0,1,2,3"
memory="8000m"
 
containerName="nginx2"
while true; do 
	currenttime=$(date +%H:%M)
	status=$(docker ps -a --filter "name=$containerName" --filter "status=running"| grep Up)
	length=${#status}
	if [[ $currenttime>=$endTime ]] || [[ $currenttime<$initTime ]]; then
	    if [[ $length>0 ]]; then
	        docker container stop nginx2
	        echo "Worker Process has been stopped"
	    fi
	fi
	if [[ $currenttime>=$initTime ]] || [[ $currenttime<$endTime ]]; then
	    if [[ $length<1 ]]; then
	        docker container start nginx2 --cpus=$cpus --cpuset-cpus=$cpusetCpus --memory=$memory
	        echo "Worker Process has been started" 
	    fi
	fi
	sleep 100
done