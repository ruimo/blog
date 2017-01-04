#!/bin/sh
cd `dirname $0`
. ./settings.conf
netstat -an | grep LISTEN | grep 9010
