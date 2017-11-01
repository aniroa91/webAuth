#!/bin/sh

NAME=bigdata-play

docker build --no-cache -t $NAME .
docker stop $NAME
docker rm $NAME
#docker run -d --net=host --name $NAME -p 80:9000 -v /public/images/:/opt/bigdata-play/public/ $NAME
docker run -d --net=$NAME -h $NAME --name $NAME -v /public/images/:/opt/bigdata-play/public/ $NAME
