#!/bin/bash

DIR=`readlink -f \`dirname $0\``


mvn -f $DIR/pom.xml versions:set -DnewVersion=$1
mvn -f $DIR/biopet-framework/pom.xml versions:set -DnewVersion=$1
