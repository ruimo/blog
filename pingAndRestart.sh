#!/bin/sh
cd `dirname $0`
./ping.sh
rc=$?
if [ $rc = "0" ]; then
  exit 0
fi

./restart.sh
exit $rc
