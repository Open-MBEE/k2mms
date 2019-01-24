#!/bin/bash
if [ -e `ls -1 ../mms-java-client/target/swagger*.jar | tail -n 1` ]; then
    echo "Already set up."
else
    zipFile=`pwd`"/"`ls -1 mms*.zip`
    echo $zipFile
    unzip "$zipFile"
    if [ -e ../mms-java-client ]; then
        rm -rf ../mms-java-client
    fi
    mv mms ../mms-java-client
    pushd ../mms-java-client
    mvn clean package
    popd
fi

