#!/bin/bash

# This is a fairly naive shell script that constructs a classpath for
# running XML Calabash. The idea is that it puts jar files from the
# "extra" directory ahead of jar files from the "lib" directory.
# This should support overriding jars. And supports running steps
# that require extra libraries.

# Try to be careful about paths with spaces in them!

FQPATH=`readlink -f "$0"`
ROOT=`dirname "$FQPATH"`

if [ ! -f "$ROOT/xmlcalabash-app-@@VERSION@@.jar" ]; then
    echo "XML Calabash script did not find the @@VERSION@@ distribution jar"
    exit 1
fi

jarsArray=()
if [ -d "$ROOT/extra" ]; then
    for jar in "$ROOT/extra"/*.jar; do
        jarsArray+=( "$jar" )
    done
fi

for jar in "$ROOT/lib"/*.jar; do
    jarsArray+=( "$jar" )
done

CP="$ROOT/xmlcalabash-app-@@VERSION@@.jar"
for ((idx = 0; idx < ${#jarsArray[@]}; idx++)); do
    CP="$CP:${jarsArray[$idx]}"
done

if [ -z "$JAVA_HOME" ]; then
    # I hope java is on the PATH
    java -cp "$CP" com.xmlcalabash.app.Main "$@"
else
    "$JAVA_HOME/bin/java" -cp "$CP" com.xmlcalabash.app.Main "$@"
fi
