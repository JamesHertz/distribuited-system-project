#!/usr/bin/env bash

JAR_FILE=sd2223.jar
IMAGE=sd2223-trab1-60198-61177
NETWORK=sd-proj1
PORT=8080

if [[ "$1" = "-b" ||  "$1" = "--build" ]]; then
    mvn compile assembly:single docker:build
    shift
fi

if [ $# -lt 2 ] ; then
    echo "usage: $0 <domain> [ users | feeds ]"
    echo "ERROR: wrong number of arguments"
    exit 1
fi

CMD="java -cp "$JAR_FILE" sd2223.trab1.server.RestServer $@"

echo "running: $@"
docker run --rm -it --network "$NETWORK" --expose "$PORT" -P  "$IMAGE" $CMD

