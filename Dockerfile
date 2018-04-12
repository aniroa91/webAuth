FROM bigdata-registry.local:5043/java:latest

RUN mkdir -p /opt/bigdata-play
COPY ./dist /opt/bigdata-play
WORKDIR /opt/bigdata-play
CMD ["bin/bigdata-play","-Dconfig.file=conf/prod/application.conf"]

