#!/bin/sh

NAME=bigdata-play

#docker build --no-cache -t $NAME .
docker stop $NAME
docker rm $NAME
docker run -d \
           --net=host \
           --name $NAME \
           -p 80:9000 \
           -v /public/images/:/opt/bigdata-play/public/ \
           -v ${PWD}/dist:/opt/bigdata-play \
           bigdata-registry.local:5043/java /opt/bigdata-play/bin/bigdata-play -Dconfig.file=conf/prod/application.conf
