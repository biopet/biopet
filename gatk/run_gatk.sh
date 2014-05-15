#!/bin/bash

JAVA_EXE=java
QUEUE_JAR=/data/DIV5/SASC/common/programs/Queue-3.1-1/Queue.jar
JAR_FILE=`dirname $0`/target/Gatk*.jar
PIPELINE_SCRIPT=`dirname $0`/src/main/java/nl/lumc/sasc/biopet/pipelines/gatk/Gatk.scala

$JAVA_EXE -Xmx5g $JAVA_OPTIONS -jar $JAR_FILE -S $PIPELINE_SCRIPT $@
