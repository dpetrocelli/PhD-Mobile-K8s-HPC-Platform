FROM adoptopenjdk/openjdk11
COPY dev-threadunifier.jar /dev-threadunifier.jar
COPY ffmpeg /tmp/ffmpeg
#RUN  echo proxy = 192.168.1.231:80 > ~/.curlrc
CMD ["java", "-jar", "/dev-threadunifier.jar"]
