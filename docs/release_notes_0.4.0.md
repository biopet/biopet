# Release notes Biopet version 0.4.0

* A reporting framework has been added for most pipelines
 * This framework produces a static HTML report which can be viewed in your browser
 * The framework contains lots of quality control and downstream analyses plots (genome coverage, transcript coverage etc etc.)
* An issue where a NullPointerException was being thrown when output_dir was not set in the config was fixed. This now gives a nice error message which points to the missing key in the config
* Pipelines now automatically write a log file if none is specified on command line
* Tools writing to VCF will no longer fail when the *output* is not a *gzipped* VCF
* Pipelines now support passing config options directly into the commandline prompt
* Pipelines now support a more readable config file format [YAML](https://en.wikipedia.org/?title=YAML)
* Memlimit and vmem memory issues are solved by automatically increasing the amount of available memory when a job fails. ```--retry 5``` should do the trick
* BamMetrics pipeline is updated to work with newest version of Picard
* A bug in VcfStats in comparing samples alleles is fixed. Now each allele can only be used once in the comparison.
* VcfStats is now capable of summarizing stats per bin (bin size is changeable)
* There is now a module which checks for the present of correct reference files if not it automatically builds the appropriate ref files

Some pipelines were updated as well:

* Gentrap
* Shiva