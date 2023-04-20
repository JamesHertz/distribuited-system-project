#!/usr/bin/env bash
set -e

JAR_FILE=sd2223.jar
IMAGE=sd2223-trab1-60198-61177
NETWORK=sd-proj1
PORT=8080
REST_SERVER="sd2223.trab1.servers.rest.RestServer"
SOAP_SERVER="sd2223.trab1.servers.soap.SoapServer"
SERVER=

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
    --soap)
      echo "server: SOAP"
      SERVER=$SOAP_SERVER
      ;;
    --rest) ;;
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

if [ -z "$SERVER" ] ; then
  echo "server: REST"
  SERVER=$REST_SERVER
fi

CMD="java -cp "$JAR_FILE" $SERVER $*"
echo "running: $*"
docker run --rm -it --network "$NETWORK" $EXPOSED  "$IMAGE" $CMD

