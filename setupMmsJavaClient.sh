#!/bin/bash
jarFile=`ls -1 ../mms-java-client/target/swagger*.jar | tail -n 1`
if [ -e "$jarFile" ]; then
    echo "Already set up. $jarFile"
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

