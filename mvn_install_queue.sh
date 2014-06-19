JAR=/data/DIV5/SASC/common/programs/Queue-3.1-1/Queue-shark.jar
ID=queue-package
VERSION=3.1
GROUP=org.broadinstitute.sting
mvn install:install-file -Dfile=$JAR -DgroupId=$GROUP -DartifactId=$ID -Dversion=$VERSION -Dpackaging=jar

JAR=/data/DIV5/SASC/common/programs/Queue-3.1-1/queue-framework-3.1.jar
ID=queue-framework
mvn install:install-file -Dfile=$JAR -DgroupId=$GROUP -DartifactId=$ID -Dversion=$VERSION -Dpackaging=jar

#gatk-package

JAR=/data/DIV5/SASC/common/programs/Queue-3.1-1/gatk-package-3.1.jar
ID=gatk-package
mvn install:install-file -Dfile=$JAR -DgroupId=$GROUP -DartifactId=$ID -Dversion=$VERSION -Dpackaging=jar

#gatk-framework

JAR=/data/DIV5/SASC/common/programs/Queue-3.1-1/gatk-framework-3.1.jar
ID=gatk-framework
mvn install:install-file -Dfile=$JAR -DgroupId=$GROUP -DartifactId=$ID -Dversion=$VERSION -Dpackaging=jar

