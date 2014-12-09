#!/bin/bash
#
# Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
# @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
#

DIR=`readlink -f \`dirname $0\``
MODE=check

if [ $1 ] ; then
    MODE=$1
fi

mvn -f $DIR/public/pom.xml license:$MODE -Dlicense.header=$DIR/public/LICENSE

mvn -f $DIR/protected/pom.xml license:$MODE -Dlicense.header=$DIR/protected/LICENSE