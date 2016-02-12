
import java.io.File

import nl.lumc.sasc.biopet.pipelines.flexiprep.Fastqc

val fastqcStats = new File("/exports/sasc/ethoen-smallrna/analysis/production_strandspecific_mirbase_ensembl_test/samples/102722-001-001/lib_HLF5JBGXX_ATCACG_L004/flexiprep/HLF5JBGXX_102722-001-001_ATCACG_L004_R1.fastq.fastqc/HLF5JBGXX_102722-001-001_ATCACG_L004_R1_fastqc/fastqc_data.txt")

val fastqc = new Fastqc(null)
fastqc.output = fastqcStats
fastqc.foundAdapters
