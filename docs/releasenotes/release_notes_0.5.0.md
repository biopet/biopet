# Release notes Biopet version 0.5.0

## General Code changes

* Upgrade to Queue 3.4, with this also the htsjdk library to 1.132
* Our `QC` and `Mapping` pipeline now use piping for the most used aligners and QC tools
    * Reducing I/O over the network
    * Reducing the disk usage (storage) and run time
* Added version command for Star
* Seperation of the `biopet`-framework into: `Core`, `Extensions`, `Tools` and `Utils`
* Optimized unit testing
* Unit test coverage on `Tools` increased
* Workaround: Added R-script files of Picard to biopet to fix picard jobs (files are not packaged in maven dependency)
* Added external example for developers

## Functionality

* Retries of pipeline and tools is now enabled by default
* Improvements in the reporting framework, allowing custom reporting elements for specific pipelines.
* Fixed reports when metrics of Flexiprep is skipped
* Added metagenomics pipeline: [Gears](../pipelines/gears.md)
* Added single sample variantcalling with bcftools
* Added ET + key support for GATK job invocation, disable phone-home feature when key is supplied
* Added more debug information in the `.log` directory when `-l debug` is enabled
* [Shiva](../pipelines/multisample/shiva.md): added support for `GenotypeConcordance` tool to check against a Golden Standard
* [Shiva](../pipelines/multisample/shiva.md): fixed a lot of small bugs when developing integration tests
* [Shiva](../pipelines/multisample/shiva.md): Workaround: Fixed a dependency on rerun, with this change there can be 2 bam files in the samples folder
* [Gentrap](../pipelines/multisample/gentrap.md): Improved error handling on missing annotation files

## Infrastructure changes

* Development environment within the LUMC now get tested with Jenkins
    * Added integration tests Flexiprep
    * Added integration tests Gears
    * Added integration tests Mapping
    * Added integration tests Shiva
    * Added integration tests Toucan
