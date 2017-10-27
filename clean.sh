#!/bin/sh

NAME=bigdata-play

docker stop $NAME &&
docker rm $NAME &&
docker rmi $NAME

