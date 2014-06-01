#!/bin/sh

export MAVEN_OPTS="-Xms2048m -Xmx2048m"

echo "Entering Maven quiet mode ..."
cd `dirname $0`/..
mvn -q exec:java \
    -Dexec.mainClass="com.weisong.test.verticle.device.WebsocketDevice" \
    -Dexec.args="$*"

