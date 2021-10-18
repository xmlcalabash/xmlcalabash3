#!/bin/bash

if [ "$XPROC_TEST_SUITE" = "" ]; then
  XPROC_TEST_SUITE=/Users/ndw/Projects/xproc/test-suite/test-suite/tests
fi

sbt -DsaxonEdition=EE -Dlog4j.configurationFile=src/test/resources/log4j2.xml test \
"Test / fgRunMain com.xmlcalabash.drivers.Test -j xml-calabash.xml -c src/test/resources/config.xml $XPROC_TEST_SUITE"
