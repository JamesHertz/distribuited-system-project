#!/usr/bin/env bash
set -e

JAR_FILE=sd2223.jar
IMAGE=sd2223-trab1-60198-61177
NETWORK=sd-proj1
PORT=8080
EXPOSED="--expose $PORT -P"

if [[ "$1" = "-b" ||  "$1" = "--build" ]]; then
    echo "building project"
    mvn compile assembly:single docker:build
    shift
fi

if [ "$1" = "-p" ]; then
    [[ "$2" =~ ^[0-9]+$ ]] && IN_PORT=$2 && shift || IN_PORT=$PORT
    echo "using port $IN_PORT"
    EXPOSED="-p $IN_PORT:$PORT"
    shift
fi


if [ $# -lt 2 ] ; then
    echo "usage: $0 <domain> [ users | feeds ]"
    echo "ERROR: wrong number of arguments"
    exit 1
fi

#CMD="java -cp "$JAR_FILE" sd2223.trab1.servers.rest.RestServer $@"
CMD="java -cp "$JAR_FILE" sd2223.trab1.servers.soap.SoapServer $@"

echo "running: $*"
docker run --rm -it --network "$NETWORK" $EXPOSED  "$IMAGE" $CMD

