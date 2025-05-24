#!/bin/bash

export WEBSERVER=http://webserver
export SMTPSERVER=sendriasmtp

rsync -ar --delete --exclude build --exclude .gradle /sources/ /xmlcalabash/src/
cd /xmlcalabash/src 
if [ "$1" = "clean" ]; then
  ./gradlew --console=plain clean
fi

rm -rf /output/reports/xmlcalabash

./gradlew --console=plain xmlcalabash:test

mkdir -p /output/reports/xmlcalabash

rsync -ar --delete xmlcalabash/build/reports/ /output/reports/xmlcalabash/
