FROM adoptopenjdk/openjdk11
COPY dev-workerx86.jar /dev-workerx86.jar
#RUN  echo proxy = 192.168.1.231:80 > ~/.curlrc
COPY ffmpeg /tmp/ffmpeg
CMD ["java", "-jar", "/dev-workerx86.jar"]
