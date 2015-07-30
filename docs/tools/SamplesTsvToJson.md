# SamplesTsvToJson

This tool enables a user to create a full sample sheet in JSON format suitable for all our Queue pipelines.
The tool can be started as follows:

~~~
java -jar <Biopet.jar> tool SamplesTsvToJson
~~~

To open the help:

~~~
java -jar Biopet-0.2.0.jar tool SamplesTsvToJson -h
Usage: SamplesTsvToJson [options]

  -l <value> | --log_level <value>
        Log level
  -h | --help
        Print usage
  -v | --version
        Print version
  -i <file> | --inputFiles <file>
        Input must be a tsv file, first line is seen as header and must at least have a 'sample' column, 'library' column is optional, multiple files allowed
~~~

The tool is designed in such a way that a user can provide a TAB seperated file (TSV) with sample specific properties and even those will be parsed by the tool.
For example: a user wants to have certain properties e.g. which treatment a sample got than the user should provide a extra columns called treatment and then the 
JSON file is parsed with those properties inside it as well. The order of columns does not matter.

#### Example

~~~
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

| samples       | library | bam         |
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

