#!/bin/sh

NAME=bigdata-play

docker build --no-cache -t $NAME .
docker stop $NAME
docker rm $NAME
docker run --net=host --name $NAME -p 80:9000 -v /public/images/:/opt/bigdata-play/public/ -d $NAME
