#!/usr/bin/env bash

SBT_OPTIONS="-Xmx1G \
    -XX:MaxPermSize=250m \
    -XX:+UseCompressedOops \
    -Dsbt.log.noformat=true \
    -Dbuild.number=$BUILD_NUMBER \
    -Dbuild.vcs.number=$BUILD_VCS_NUMBER"

[ -d target ] && rm -rf target
mkdir target
cd $(dirname $0)/target

mkdir downloads
mkdir -p packages/elasticsearch-remap-tool

# concierge
if [ -z "$JDK_HOME" ]; then
    JAVA=java
else
    JAVA=$JDK_HOME/bin/java 
fi

if cd .. && $JAVA $SBT_OPTIONS -jar sbt-launch.jar dist && cd target
then
    cp scala-*/*.jar downloads/es-utils.jar
else
    echo 'Failed to build Elasticsearch Remap Tool'
    exit 1
fi

tar czfv packages/elasticsearch-remap-tool/es-utils.tar.gz -C downloads es-utils.jar
zip -rv artifacts.zip packages/

echo "##teamcity[publishArtifacts '$(pwd)/artifacts.zip => .']"
