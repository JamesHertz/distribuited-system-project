@echo off

REM #!/usr/bin/env bash
REM set -e
REM 
REM JAR_FILE=sd2223.jar
REM IMAGE=sd2223-trab1-60198-61177
REM NETWORK=sd-proj1
REM PORT=8080
REM EXPOSED="--expose $PORT -P"
REM 
REM if [[ "$1" = "-b" ||  "$1" = "--build" ]]; then
REM     echo "building project"
REM     mvn compile assembly:single docker:build
REM     shift
REM fi
REM 
REM if [ "$1" = "-p" ]; then
REM     [[ "$2" =~ ^[0-9]+$ ]] && IN_PORT=$2 && shift || IN_PORT=$PORT
REM     echo "using port $IN_PORT"
REM     EXPOSED="-p $IN_PORT:$PORT"
REM     shift
REM fi
REM 
REM 
REM if [ $# -lt 2 ] ; then
REM     echo "usage: $0 <domain> [ users | feeds ]"
REM     echo "ERROR: wrong number of arguments"
REM     exit 1
REM fi
REM 
REM #CMD="java -cp "$JAR_FILE" sd2223.trab1.server.RestServer $@"
REM CMD="java -cp "$JAR_FILE" sd2223.trab1.servers.rest.RestServer $@"
REM 
REM echo "running: $@"
REM docker run --rm -it --network "$NETWORK" $EXPOSED  "$IMAGE" $CMD
REM 
REM 

set JAR_FILE=sd2223.jar
set NETWORK=sd-proj1
set PORT=8080
set EXPOSED=--expose %PORT% -P

if "%1%"=="-build" (
    echo "building project"
    mvn compile assembly:single docker:build
    shift
)

if "%1%"=="-p" (
    set IN_PORT ="%2%"
    set EXPOSED=-p %IN_PORT%:8080
    shift
    shift
)

if %1%=="" goto error
if %2%=="" goto error

set CMD=java -cp %JAR_FILE% sd2223.trab1.servers.rest.RestServer %*
echo "running: %*"
docker run --rm -it --network %NETWORK% %EXPOSE%  %$IMAGE% %CMD%

:error
echo usage: %0 <domain> [ users | feeds ]
echo ERROR: wrong number of arguments
exit /b 1