#!/bin/bash

shopt -s nullglob

export WEBSERVER=http://webserver
export SMTPSERVER=sendriasmtp

rsync -ar --delete --exclude build --exclude .gradle /sources/ /xmlcalabash/src/
cd /xmlcalabash/src 
if [ "$1" = "clean" ]; then
  ./gradlew --console=plain clean
  shift
fi

compgen -W test-driver/build/*.xml > /dev/null 2>&1
if [ $? -lt 2 ]; then
    rm -f test-driver/build/*.xml
fi

./gradlew --console=plain -PWHOST=$WEBSERVER -PSHOST=$SMTPSERVER ${1:-test-report}

compgen -W test-driver/build/*.xml > /dev/null 2>&1
if [ $? -lt 2 ]; then
    cp test-driver/build/*.xml /output/
fi
