### System Requirements

Biopet is build on top of GATK Queue, which requires having `java` installed on the analysis machine(s).

For end-users:

 * [Java 7 JVM](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or [OpenJDK 7](http://openjdk.java.net/install/)
 * [Cran R 2.15.3](http://cran.r-project.org/)
 * It strongly advised to run Biopet pipelines on a compute cluster since the amount of resources needed can not be achieved on
  a local machine. Note that this does not mean it is not possible!

For developers:

 * [OpenJDK 7](http://openjdk.java.net/install/) 
 * Minimum of 4 GB RAM {todo: provide more accurate estimation on building}
 * Maven 3
 * Compiled and installed version 3.4 of [GATK + Queue](https://github.com/broadgsa/gatk-protected/) in your maven repository.
 * IntelliJ or Netbeans 8.0 for development

