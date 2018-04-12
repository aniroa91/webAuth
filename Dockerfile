FROM bigdata-registry.local:5043/java:latest

#RUN mkdir -p /opt/bigdata-play
WORKDIR /opt/
COPY target/universal/bigdata-play-1.0-SNAPSHOT.zip /opt/bigdata-play-1.0-SNAPSHOT.zip
RUN unzip /opt/bigdata-play-1.0-SNAPSHOT.zip \
  && rm -f /opt/bigdata-play-1.0-SNAPSHOT.zip \
  && mv /opt/bigdata-play-1.0-SNAPSHOT /opt/bigdata-play
RUN ls /opt/bigdata-play
RUN chmod +x /opt/bigdata-play/bin/bigdata-play
CMD ["bin/bigdata-play","-Dconfig.file=conf/prod/application.conf"]

