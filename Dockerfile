FROM bigdata-registry.local:5043/openjdk_8u191-jre-alpine3.8:v2
ENV TZ=Asia/Ho_Chi_Minh

RUN mkdir -p /opt/
WORKDIR /opt/
ARG PLAYNAME
ENV PLAYNAME=bigdata-ftel-play-userprofile-1.0-SNAPSHOT.zip
COPY target/universal/$PLAYNAME /opt/$PLAYNAME
RUN unzip /opt/$PLAYNAME \
  && rm -f /opt/$PLAYNAME \
  && mv /opt/bigdata-ftel-play-userprofile-1.0-SNAPSHOT /opt/bigdata-play
# RUN ls /opt/bigdata-play/bin
WORKDIR /opt/bigdata-play
CMD ["bin/bigdata-ftel-play-userprofile","-Dconfig.file=conf/prod/application.conf"]