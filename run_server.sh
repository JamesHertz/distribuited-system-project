#!/usr/bin/env bash
set -e

IMAGE=sd2223-trab2-60198-61177
NETWORK=sd-proj1
PORT=8080

while true
do
  case $1 in
    -b|--build)
      echo "building project"
      mvn compile assembly:single docker:build
    ;;
    -p|--port)
      echo "exposing port... "
      if  [[ "$2" =~ ^[0-9]+$ ]]  ; then
        EXPOSED="-p $2:$PORT"
        echo "using port: $2"
        shift
      else
        EXPOSED="--expose $PORT -P"
      fi
     ;;
    *)
      break ;;
  esac
    shift
done

if [ $# -lt 2 ] ; then
    echo "usage: $0 <domain> [ users | feeds ]"
    echo "ERROR: wrong number of arguments"
    exit 1
fi

echo "running: "
docker run --rm -it --network "$NETWORK" $EXPOSED  "$IMAGE"

