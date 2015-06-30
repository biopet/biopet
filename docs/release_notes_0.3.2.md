# Release notes Hotfix 0.3.2

* There was a bug found in our RNA seq pipeline ( Gentrap )
 * The merged count table missed out on 1 gene consistently. The separate count files per sample where not affected by this bug
* This bug involved all previous runs from Gentrap of the last 2 months, all customers where informed and if requested a new file is delivered.
 * we manually checked all the runs and found that the first gene was almost never expressed in our data sets
 