# Developer - Getting started

### Requirements
- Maven 3.3
- Installed Gatk to maven local repository (see below)
- Installed Biopet to maven local repository (see below)

To start to develop a biopet pipeline you should have installed Gatk and Biopet in your local maven repository. Do to this execute the follow command.
```bash
# Replace 'mvn' for to location of you maven executable or make put it in your PATH
git clone https://github.com/broadgsa/gatk-protected
cd gatk-protected
git checkout 3.4
# This version is bound to a version of Biopet, Biopet 0.5.0 using Gatk 3.4
mvn clean install

cd ..

git clone https://github.com/biopet/biopet.git
cd biopet
git checkout 0.5.0
mvn -DskipTests=true clean install

```

### Basic components

#### Qscript (pipeline)
A basic pipeline would look like this.

#### Extensions (wrappers)


#### Tools (scala programs)