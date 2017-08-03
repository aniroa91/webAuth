#!/bin/bash

start() {
  docker run -d -p 9000:9000 \
           -h domainservice \
           --name domainservice \
           -v /redis01/dungvc/restapi/public/:/play/public \
           -v /build/scripts/restapi/domain-service/conf:/play/conf \
           -v /home/dungvc2/deploy/restapi/bigdata-play-1.0-SNAPSHOT:/play \
           java /play/bin/bigdata-play
}

stop() {
  docker stop domainservice
  docker rm domainservice
}

case "$1" in
  start)
    start
  ;;
  stop)
    stop
  ;;
  restart)
    stop
    start
  ;;
  *)
    echo "$(pwd)/kibana.sh [start|stop|restart]"
  ;;
esac
