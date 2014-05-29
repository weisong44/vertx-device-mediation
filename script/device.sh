#!/bin/sh

echo "Entering Maven quiet mode ..."
cd `dirname $0`/..
mvn -q exec:java \
    -Dexec.mainClass="com.weisong.test.verticle.device.WebsocketDevice" \
    -Dexec.args="$*"

