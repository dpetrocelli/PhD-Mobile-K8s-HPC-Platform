FROM adoptopenjdk/openjdk11
COPY dev-fileserver.jar /dev-fileserver.jar
#RUN mkdir /home/storage/
#RUN mkdir /home/storage/s3-bcra-splitted
#RUN mkdir /home/storage/s3-bcra-finished
#RUN mkdir /home/storage/s3-bcra-compressed
#RUN chmod 777 -R /home/storage/
EXPOSE 8080
CMD ["java", "-jar", "/dev-fileserver.jar"]