#!/bin/bash

cd /xmlcalabash/src && rsync -ar --delete --exclude build --exclude .gradle /sources/ ./
pwd
./gradlew --console=plain clean
./gradlew --console=plain stage
