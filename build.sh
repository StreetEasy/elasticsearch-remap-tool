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

if cd .. && java $SBT_OPTIONS -jar sbt-launch.jar assembly && cd target
then
    cp scala-*/*.jar elasticsearch-remap-tool.jar
else
    echo 'Failed to build Elasticsearch Remap Tool'
    exit 1
fi

echo "##teamcity[publishArtifacts '$(pwd)/elasticsearch-remap-tool.jar => .']"
