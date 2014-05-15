#!/bin/bash

JAVA_EXE=java
JAR_FILE=`dirname $0`/target/Flexiprep*.jar
PIPELINE_SCRIPT=`dirname $0`/../flexiprep/src/main/java/nl/lumc/sasc/biopet/pipelines/flexiprep/Flexiprep.scala

$JAVA_EXE $JAVA_OPTIONS -jar $JAR_FILE -S $PIPELINE_SCRIPT $@
