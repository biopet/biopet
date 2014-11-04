#!/bin/bash
#
# Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
# @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
# 
# Script used to change version in pom files for Biopet project
# 

DIR=`readlink -f \`dirname $0\``

if [ ! $1 ] ; then
    echo "usage: "`basename $0`" <version>"
    exit 1
fi

mvn -f $DIR/pom.xml versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
mvn -f $DIR/biopet-framework/pom.xml versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
