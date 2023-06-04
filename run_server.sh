#!/usr/bin/env bash
set -e

IMAGE=sd2223-trab2-60198-61177
NETWORK=sdnet
PORT=8080
HOST=
PASSWORD=

while true
do
  case $1 in
    -b|--build)
      echo "building project"
      mvn compile assembly:single docker:build
      clear
      echo "project built..."
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
   -h|--host)
      HOST="$2"
      shift
      ;;
    --pass)
      PASSWORD=$2
      shift
      ;;
    *)
      break ;;
  esac
    shift
done

if [ -z "$HOST" ] || [ -z "$PASSWORD" ] ; then
  echo "Password of hostname not set."
  exit 1
fi

if  ! [ -f "tls/keystore/$HOST.jks" ] ; then
  echo -n "Invalid hostname: $HOST"
  exit 1
fi

echo "running: "
docker run --rm -it --network "$NETWORK" $EXPOSED  -h "$HOST" "$IMAGE" \
  java -Djavax.net.ssl.keyStore="keystore/$HOST.jks" \
      -Djavax.net.ssl.keyStorePassword=$PASSWORD     \
      -Djavax.net.ssl.trustStore=keystore/users-keystore.jks \
      -Djavax.net.ssl.trustStorePassword=changeit  \
      -cp sd2223.jar sd2223.trab2.servers.Main \
      $*
