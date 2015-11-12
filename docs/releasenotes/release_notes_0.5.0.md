# Release notes Biopet version 0.5.0

* Our QC and mapping pipeline now use piping for the most used aligners and QC tools
 * This decreases the disk usage and run time
* Improvements in the reporting framework
* Added metagenomics pipeline: [Gears](../pipelines/gears.md)
* Development envoirment within the LUMC now get tested with Jenkins
 * Added integration tests Flexiprep
 * Added integration tests Mapping
 * Added integration tests Shiva
 * Added integration tests Toucan
* Added version command for Star
* Added single sample variantcalling with bcftools
* Splitting the Framework into: Core, Extensions, Tools and Utils
* Fixed reports when Metrics of Flexiprep is skipped
* Upgrade to Queue 3.4, with this also the htsjdk library to 1.132
* Added key support for GATK jobs
* Optimizing unit testing
* Unit test coverage on Tools increased
* Retry is now default enabled
* Added more debug information in the `.log` directory when `-l debug` is enabled
* Shiva: added support for GenotypeConcordance tool to check against a Golden Standard
* Workaround: Added Rscript files of picard to biopet to fix picard jobs (files are not packaged in maven dependency)
* Shiva: fixed a lot of small bugs when developing integration tests
* Gentrap: Better error handeling on missing annotation files
* Shiva: Workaround: Fixed a dependency on rerun, with this change there can be 2 bam files in the samples folder
* Added external example for developers
