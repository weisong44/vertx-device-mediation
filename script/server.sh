#!/bin/sh

cd `dirname $0`
. functions
$VERTX_HOME/bin/vertx runmod com.weisong.test~vertx-device-mediation~0.99

