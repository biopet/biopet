Biopet Project
==============

Framework build on top of GATK Queue for building bioinformatic pipelines.


Installation
============

Note: all installation procedures require Maven.

### If you are in SHARK or have access to SHARK directories

Run the `mvn_install_queue.sh` script

### Non-SHARK installs

#### If you want to build Queue via the repository

1. Clone the GATK protected repository. We need to use the protected repository because some pipelines use the GATK walkers.

    git clone git@github.com:broadgsa/gatk-protected.git

2. In the root directory:

    mvn install

3. Go to the Biopet root directory and run:

    mvn install

#### If you want to use the prebuilt Queue JAR downloaded from the website

1. Install the Queue JAR in your local maven repository:

    mvn install:install-file -Dfile={your_queue_jar} -DgroupId=org.broadinstitute.sting -DartifactId=queue-package -Dversion={your_queue_version} -Dpackaging=jar

2. Go to the Biopet root directory and run:

    mvn install


License
=======

A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL license; For commercial users or users who do not want to follow the AGPL license, please contact [sasc@lumc.nl](mailto:sasc@lumc.nl) to purchase a separate license.
