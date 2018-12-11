FROM bigdata-registry.local:5043/java:8-alpine-bash

ENV TZ=Asia/Ho_Chi_Minh
#RUN mkdir -p /opt/bigdata-play
WORKDIR /var/opt/
COPY target/universal/bigdata-play-1.0-SNAPSHOT.zip /var/opt/bigdata-play-1.0-SNAPSHOT.zip
RUN unzip /var/opt/bigdata-play-1.0-SNAPSHOT.zip \
  && rm -f /var/opt/bigdata-play-1.0-SNAPSHOT.zip \
  && mv /var/opt/bigdata-play-1.0-SNAPSHOT /var/opt/bigdata-play
#RUN ls /opt/bigdata-play-fplay/bin
WORKDIR /var/opt/bigdata-play
CMD ["bin/bigdata-play","-Dconfig.file=conf/prod/application.conf"]
