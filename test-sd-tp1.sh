#!/bin/bash

[ ! "$(docker network ls | grep sdnet )" ] && \
	docker network create --driver=bridge --subnet=172.20.0.0/16 sdnet


if [  $# -le 1 ] 
then 
		echo "usage: $0 -image <img> [ -test <num> ] [ -log OFF|ALL|FINE ] [ -sleep <seconds> ]"
		exit 1
fi

case "$1" in
  --build|-b)
      echo "Building project..."
      mvn clean compile assembly:single  docker:build
      ;;
esac
#execute the client with the given command line parameters
docker pull nunopreguica/sd2223-tester-tp1:latest
docker run --rm --network=sdnet -it -v /var/run/docker.sock:/var/run/docker.sock nunopreguica/sd2223-tester-tp1 $*

