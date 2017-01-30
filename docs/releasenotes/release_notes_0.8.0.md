# Release notes Biopet version 0.8.0

### Highlights
    * Biopet is now fully hosted at github.
    * Several major improvements on using Centrifuge for metagenomics data analysis
    * Added XHMM as a tool for copy number analysis
    * Added support for SLURM
    * Added tool to create a graphical representation of pipeline progress.
    * Added soft clipping metrics to HTML report
    
## Full change list

### Task

    * [BIOPET-508] - Move Biopet completly to github

### Bug

    * [BIOPET-337] - Rscript summary from Queue doesn't work
    * [BIOPET-353] - Log outputs doesn't contain classname ( ..out issue)
    * [BIOPET-369] - cn.mops fails on small chromosomes
    * [BIOPET-379] - Biopet error when json file not correctly formated
    * [BIOPET-383] - Sample json not checked for invalid libraries
    * [BIOPET-384] - Select proper values in VcfWithVcf when when number=A
    * [BIOPET-385] - Cannot combine doNotRemove in VepNormalizer with chunked Toucan
    * [BIOPET-386] - VcfFilter's mustHaveVariant option ignores certain genotypes
    * [BIOPET-394] - Flanking on pysvtools is required
    * [BIOPET-403] - Report centrifuge does not display separate library plots when enabled
    * [BIOPET-405] - Toucan with custom fields fails
    * [BIOPET-409] - CatVariants failing after Delly
    * [BIOPET-410] - Krona plots in GearsSingle are not correct for centrifuge
    * [BIOPET-415] - SortVcf extensions does not see .tbi file as output
    * [BIOPET-418] - Nullpointer when config file is empty
    * [BIOPET-450] - Base counts tests are failing
    * [BIOPET-458] - Pipeline help status no longer given
    * [BIOPET-473] - Dustbin pipeline does not show centrifuge report
    * [BIOPET-481] - Chunksize can't go higher then 2G because of limitions a INT
    * [BIOPET-485] - files from fifo's from paired-end flexiprep are in the graph
    * [BIOPET-486] - BreakdancerCaller does not depend on bam file in graph
    * [BIOPET-488] - Bastats does not depend on index of bam file
    * [BIOPET-491] - Summary of flexiprep not depens on qc_cmd 
    * [BIOPET-493] - FastqSplitter is disconnected from the graph
    * [BIOPET-495] - Fix XHMM 
    * [BIOPET-496] - Alignment plot does not show stats
    * [BIOPET-504] - Validate vcf step does not have an output file
    * [BIOPET-507] - To much file handles for .out files
    * [BIOPET-509] - VcfFilter MustHaveVariant does not check if sample exist
    * [BIOPET-516] - Config value is not correct for skip_trim and skip_clip
    * [BIOPET-526] - bammetrics summary fails with empty histogram array
    * [BIOPET-544] - Link to assembly report is broken

### Improvement

    * [BIOPET-309] - Colapse output files of vcfstats into 1 file
    * [BIOPET-317] - Remove unnecessary intermediate bams to free up more space when pipeline finishes successfully
    * [BIOPET-359] - Enable htseq to count multiple-alignments
    * [BIOPET-374] - Add clipping stats to html report
    * [BIOPET-387] - Convenience methods for semantic versions in Version
    * [BIOPET-389] - Shiva is overzealous with sorting amplicon bed files
    * [BIOPET-395] - Lazy dict cache in reference module
    * [BIOPET-396] - Add support for multiple versions of annotations in config file
    * [BIOPET-398] - Change default in Gears to centrifuge
    * [BIOPET-406] - Add a better error / exception when output dir is not writable 
    * [BIOPET-411] - Option to send full fastq file to gears instead of only unmapped reads
    * [BIOPET-412] - Make files intermediate in Gears
    * [BIOPET-413] - Implementing piping for centrifuge
    * [BIOPET-414] - Add all arguments to centrifuge
    * [BIOPET-416] - Add stats output of centrifuge
    * [BIOPET-426] - Make deps.json run in normal mode
    * [BIOPET-463] - Test and update docs GEARS for Centrifuge
    * [BIOPET-464] - Implement skip_flexiprep in gears
    * [BIOPET-469] - Adding functional testing on XHMM
    * [BIOPET-471] - Add agregated stats to BamStats
    * [BIOPET-472] - Documentation for XHMM feature
    * [BIOPET-475] - Reorganize .log dir
    * [BIOPET-477] - Adding refcalls to MpileupToVcf
    * [BIOPET-480] - Remove duplicate jobs in bam2wig
    * [BIOPET-483] - Add fa.gz / samtools faidx on fa.gz to IndexReference
    * [BIOPET-484] - Update documentation for biopet developper
    * [BIOPET-489] - Fix compile warnings
    * [BIOPET-498] - Add testing for PipelineStatus
    * [BIOPET-503] - Colapse output files of bamstats into 1 file
    * [BIOPET-506] - Add check if R1 and R2 are the same
    * [BIOPET-525] - Adding unassigned reads to Krona plot
    * [BIOPET-532] - Add jenkins setup to documentation and README

### New Feature

    * [BIOPET-399] - Add walltime to core
    * [BIOPET-402] - Tool for filtering fastq files based on read names
    * [BIOPET-417] - Config template tools
    * [BIOPET-419] - Create Tsv to Samples.yml converter for sample sheet 
    * [BIOPET-425] - Add main jobs to deps.json
    * [BIOPET-427] - Crompress deps.json with only main jobs
    * [BIOPET-428] - Generate a dot file with only main jobs
    * [BIOPET-460] - Vcf/dbsnp validate step
    * [BIOPET-466] - Write XHMM wrappers 
    * [BIOPET-467] - Write XHMMMethod in Shiva 
    * [BIOPET-468] - XCNV to BED conversion tool
    * [BIOPET-470] - Implement DepthOfCoverage wrapper 
    * [BIOPET-476] - Tool to check for status of pipeline
    * [BIOPET-497] - Add status to compressed plot
    * [BIOPET-500] - Add support for Slurm
    * [BIOPET-421] - Implement XHMM
