# BastyGenerateFasta

This tool generates Fasta files out of variant (SNP) alignments or full alignments (consensus). 
It can be very useful to produce the right input needed for follow up tools, for example phylogenetic tree building.

## Example

To get the help menu:

~~~bash
java -jar Biopet-0.2.0-DEV-801b72ed.jar tool BastyGenerateFasta -h

Usage: BastyGenerateFasta [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -V <file> | --inputVcf <file>
        vcf file, needed for outputVariants and outputConsensusVariants
  --bamFile <file>
        bam file, needed for outputConsensus and outputConsensusVariants
  --outputVariants <file>
        fasta with only variants from vcf file
  --outputConsensus <file>
        Consensus fasta from bam, always reference bases else 'N'
  --outputConsensusVariants <file>
        Consensus fasta from bam with variants from vcf file, always reference bases else 'N'
  --snpsOnly
        Only use snps from vcf file
  --sampleName <value>
        Sample name in vcf file
  --outputName <value>
        Output name in fasta file header
  --minAD <value>
        min AD value in vcf file for sample
  --minDepth <value>
        min detp in bam file
  --reference <value>
        Indexed reference fasta file
~~~

To run the tool please use:
~~~bash
# Minimal example for option: outputVariants (VCF based)
java -jar Biopet-0.2.0.jar tool BastyGenerateFasta --inputVcf myVCF.vcf \
--outputName NiceTool --outputVariants myVariants.fasta

# Minimal example for option: outputConsensus (BAM based)
java -jar Biopet-0.2.0.jar tool BastyGenerateFasta --bamFile myBam.bam \
--outputName NiceTool --outputConsensus myConsensus.fasta

# Minimal example for option: outputConsensusVariants
java -jar Biopet-0.2.0.jar tool BastyGenerateFasta --inputVcf myVCF.vcf --bamFile myBam.bam \
--outputName NiceTool --outputConsensusVariants myConsensusVariants.fasta
~~~

For LUMC/researchSHARK users there is a module available that sets all your environment settings and default executables/settings.

~~~
module load Biopet/0.2.0

# Minimal example for option: outputVariants (VCF based)

biopet tool BastyGenerateFasta --inputVcf myVCF.vcf \
--outputName NiceTool --outputVariants myVariants.fasta

# Minimal example for option: outputConsensus (BAM based)
biopet tool BastyGenerateFasta --bamFile myBam.bam \
--outputName NiceTool --outputConsensus myConsensus.fasta

# Minimal example for option: outputConsensusVariants
biopet tool BastyGenerateFasta --inputVcf myVCF.vcf --bamFile myBam.bam \
--outputName NiceTool --outputConsensusVariants myConsensusVariants.fasta
~~~

## Output

* FASTA containing variants only
* FASTA containing all the consensus sequences based on a minimal coverage (default:8) but can be modified in the settings config

