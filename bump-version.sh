#!/bin/bash

DIR=`readlink -f \`dirname $0\``

mvn -f $DIR/pom.xml versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
mvn -f $DIR/biopet-framework/pom.xml versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
