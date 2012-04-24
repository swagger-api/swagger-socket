#!/bin/bash

SCRIPT_DIR=`dirname $0`
SCRIPT_NAME=`basename $0`
CURRENT_DIR=`pwd`
BUILD_COMMON=${BUILD_COMMON:=NOPE}
unset NETTOSPHERE_OPTS

export NETTOSPHERE_OPTS="-Dlogback.configurationFile=conf/logback.xml "
JAVA_DEBUG_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,address=9009,server=y,suspend=n "
JAVA_CONFIG_OPTIONS="-Xms4096m -Xmx4096m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:PermSize=256m -XX:MaxPermSize=256m"
export JAVA_OPTS="-Duser.timezone=GMT ${JAVA_CONFIG_OPTIONS} ${JAVA_DEBUG_OPTIONS} "

echo "Starting NettoSphere:"
PARAMETERS="-classpath lib/*: com.wordnik.swaggersocket.samples.NettoSphere"
COMMAND="java -server ${NETTOSPHERE_OPTS} ${JAVA_OPTS} ${DEV_OPTS} ${PARAMETERS}"
echo $COMMAND
$COMMAND
