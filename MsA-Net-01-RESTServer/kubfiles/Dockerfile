FROM adoptopenjdk/openjdk11
COPY dev-restserver.jar /dev-restserver.jar
COPY ffmpeg /tmp/ffmpeg
#RUN  echo proxy = 192.168.1.231:80 > ~/.curlrc
#RUN  apt update -y
#RUN  apt install ffmpeg -y
EXPOSE 8081
CMD ["java", "-jar", "/dev-restserver.jar"]
