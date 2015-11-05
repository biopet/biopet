# Pipeable commands

## Introduction

Since the release of Biopet v0.5.0 we support piping of programs/tools to decrease disk usage and run time. Here we make use of
 [fifo piping](http://www.gnu.org/software/libc/manual/html_node/FIFO-Special-Files.html#FIFO-Special-Files). Which enables a 
 developer to very easily implement piping for most pipeable tools.
 
## Example

``` scala
    val pipe = new BiopetFifoPipe(this, (zcatR1._1 :: (if (paired) zcatR2.get._1 else None) ::
      Some(gsnapCommand) :: Some(ar._1) :: Some(reorderSam) :: Nil).flatten)
    pipe.threadsCorrection = -1
    zcatR1._1.foreach(x => pipe.threadsCorrection -= 1)
    zcatR2.foreach(_._1.foreach(x => pipe.threadsCorrection -= 1))
    add(pipe)
    ar._2
```

* In the above example we define the variable ***pipe***. This is the place to define which jobs should be piped together. In 
this case
 we perform a zcat on the input files. After that GSNAP alignment and Picard reordersam is performed. The final output of this 
 job will be a SAM file all intermediate files will be removed as soon as the job finished completely without any error codes.
* With the second command pipe.threadsCorrection = -1 we make sure the total number of assigned cores is not to high. This 
ensures that the job can still be scheduled to the compute cluster.
* So we hope you can appreciate in the above example that we decrease the total number of assigned cores with 2. This is done 
by the command ***zcatR1._1.foreach(x => pipe.threadsCorrection -= 1)***
 