# Release notes Biopet version 0.4.0

Since our last release a couple of things are added, changed or fixed:

* A reporting framework is added for most pipelines
 * This framework produces a static HTML report which can be viewed either offline or online
 * The framework contains lots of quality control and downstream analyses plots (genome coverage, transcript coverage etc etc.)
* A problem with an Obscure `NullPointerException` this was thrown if `output_dir` was not set in config. This now gives a nice error message which points to the missing key in the config
* Pipelines now automatically right a config file if none is specified on commandline
* VariantContextWriterBuilder does not fail anymore when the input is not a block_compressed_VCF
* Pipelines now support passing config options directly into the commandline prompt
* Pipelines now support a more readable config file format [YAML](https://en.wikipedia.org/?title=YAML)
* Memlimit and vmem memory issues are solved by increasing the amount of available memory when a job fails. ```--retry 5``` should do the trick
* Samtools Mpileup option ```-l``` is changed from optional to input
* BamMetrics is now capable of working with the reporting framework
* BamMetrics pipeline is updated to work with newest version of Picard
* VCF stats has a stricter sample to sample overlap, since in the previous versions these settings were to loose
* VCF stats is now capable of summarizing stats per bin (bin size is changeable)
* There is now a module which checks for the present of correct reference files if not it automatically builds the appropriate ref files



Some pipelines where updated as well:

* Gentrap