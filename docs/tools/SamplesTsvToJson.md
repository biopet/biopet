# SamplesTsvToJson

This tool enables a user to create a full sample sheet in JSON format, suitable for all our Queue pipelines, from TSV file(s).
The tool can be called as follows:

~~~ bash
biopet tool SamplesTsvToJson
~~~

To open the help:

~~~ bash
biopet tool SamplesTsvToJson -h
Usage: SamplesTsvToJson [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <file> | --inputFiles <file>
        Input must be a tsv file, first line is seen as header and must at least have a 'sample' column, 'library' column is optional, multiple files allowed
  -t <file> | --tagFiles <file>

  -o <file> | --outputFile <file>

~~~

A user provides a TAB seperated file (TSV) with sample specific properties which are parsed into JSON format by the tool.
For example, a user wants to add certain properties to the description of a sample, such as the treatment a sample received. Then a TSV file with an extra column called treatment is provided. 
The resulting JSON file will have the 'treatment' property in it as well. The order of the columns is not relevant to the end result 

The tag files works the same only the value is prefixed in the key `tags`.

#### Example

~~~ json
{
  "samples" : {
    "Sample_ID_1" : {
      "treatment" : "heatshock",
      "libraries" : {
        "Lib_ID_1" : {
          "bam" : "MyFirst.bam"
        }
      }
    },
    "Sample_ID_2" : {
      "treatment" : "heatshock",
      "libraries" : {
        "Lib_ID_2" : {
          "bam" : "MySecond.bam"
        }
      }
    }
  }
}
~~~

#### Sample definition

To get the above example out of the tool one should provide 2 TSV files as follows:

----

| sample        | library | bam         |
| -------       | ------- | ---------   |
|Sample_ID_1    |Lib_ID_1 |MyFirst.bam  |
|Sample_ID_2    |Lib_ID_2 |MySecond.bam |

----

#### Library definition

The second TSV file can contain as much properties as you would like. Possible option would be: gender, age and family.
Basically anything you want to pass to your pipeline is possible.

----

| sample      | treatment |
| ----------- | --------- |
| Sample_ID_1 | heatshock |
| Sample_ID_2 | heatshock |

