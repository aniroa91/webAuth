FROM java

RUN mkdir -p /opt/bigdata-play
COPY ./dist/bigdata-play-1.0-SNAPSHOT /opt/bigdata-play
WORKDIR /opt/bigdata-play
CMD ["bin/bigdata-play","-Dconfig.file=conf/prod/application.conf"]

