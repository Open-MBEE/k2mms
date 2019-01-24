if [ -e ../mms-java-client ]; then
    echo "Already set up."
else
    zipFile=`pwd`"/"`ls -1 mms*.zip`
    echo $zipFile
    unzip $zipFile
    mv mms ../mms-java-client
    pushd ../mms-java-client
    mvn clean package
    popd
fi

