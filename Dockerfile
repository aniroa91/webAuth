FROM bigdata-registry.local:5043/java:latest

RUN mkdir -p /opt/bigdata-play
COPY target/universal/bigdata-play-1.0-SNAPSHOT.zip /opt/bigdata-play/bigdata-play-1.0-SNAPSHOT.zip
RUN unzip /opt/bigdata-play/bigdata-play-1.0-SNAPSHOT.zip
WORKDIR /opt/bigdata-play/bigdata-play-1.0-SNAPSHOT
CMD ["bin/bigdata-play","-Dconfig.file=conf/prod/application.conf"]

