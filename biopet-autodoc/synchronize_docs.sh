#!/bin/bash

#Script to document not yet documented tools in biopet

#Declare variables
BIOPET_DIR=~/biopet
TOOLS_DIR=$BIOPET_DIR/biopet-tools/src/main/scala/nl/lumc/sasc/biopet/tools
DOCS_DIR=$BIOPET_DIR/docs/tools
TEMPLATE=$BIOPET_DIR/biopet-autodoc/tools_doc_template.j2
BIOPET="java -jar $BIOPET_JAR tool" #Make sure the BIOPET_JAR environment variable is defined!


for file in $TOOLS_DIR/*.scala #For loop checking all scala files in the tools section
do
  file_no_path=${file##*/}
  file_no_extension=${file_no_path%.scala}
  file_with_md=${file_no_extension}.md
  destfile=$DOCS_DIR/$file_with_md
  if [ ! -f $destfile ] #Checks if (tool_name).md is already present. Existing files are skipped
    then
      eval "$BIOPET $file_no_extension -h 2> help.txt"
      if grep -Fxq "ERROR: command '$file_no_extension' does not exist in module 'tool'" help.txt
        then
          echo "The tool $file_no_extension does not exist in the latest compiled version of biopet"
        else
          ./templateparser.py -t "$TEMPLATE" -o "destfile" -N "$file_no_extension" -L "help.txt"
          echo "$destfile created"
      
      fi
      rm help.txt
  fi  
done
