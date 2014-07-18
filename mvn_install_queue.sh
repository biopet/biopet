JAR=/data/DIV5/SASC/common/programs/Queue-3.2-2/Queue-shark.jar
ID=queue-package
VERSION=3.2-2
GROUP=org.broadinstitute.gatk
mvn install:install-file -Dfile=$JAR -DgroupId=$GROUP -DartifactId=$ID -Dversion=$VERSION -Dpackaging=jar

