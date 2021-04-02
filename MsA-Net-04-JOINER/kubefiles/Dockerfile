FROM adoptopenjdk/openjdk11
COPY dev-joiner.jar /dev-joiner.jar
COPY ffmpeg /tmp/ffmpeg
#RUN  echo proxy = 192.168.1.231:80 > ~/.curlrc
CMD ["java", "-jar", "/dev-joiner.jar"]
