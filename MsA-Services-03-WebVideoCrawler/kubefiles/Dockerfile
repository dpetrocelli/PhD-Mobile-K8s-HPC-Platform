FROM adoptopenjdk/openjdk11
COPY demo-0.0.1-SNAPSHOT.jar /dev-downloader.jar
#RUN  echo proxy = 192.168.1.231:80 > ~/.curlrc
#RUN  echo Acquire::http::Proxy "http://192.168.1.231:80"; > /etc/apt/apt.conf.d/02proxy
#RUN  echo Acquire::https::Proxy "http://192.168.1.231:80"; >> /etc/apt/apt.conf.d/02proxy
CMD ["java", "-jar", "dev-downloader.jar"]
