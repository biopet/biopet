# Memory behaviour biopet

### Calculation

#### Values per core
- **Default memory per thread**: *core_memory* + (0.5 * *retries*)
- **Resident limit**: (*core_memory* + (0.5 * *retries*)) * *residentFactor*
- **Vmem limit**: (*core_memory* + (0.5 * *retries*)) * (*vmemFactor* + (0.5 * *retries*))

We assume here that the cluster will amplify those values by the number of threads. If this is not the case for your cluster please contact us.

#### Total values
- **Memory limit** (used for java jobs): (*core_memory* + (0.5 * *retries*)) * *threads*


### Defaults

- **core_memory**: 2.0 (in Gb)
- **threads**: 1
- **residentFactor**: 1.2
- **vmemFactor**: 1.4, 2.0 for java jobs

This are the defaults of biopet but each extension in biopet can set their own defaults. As example the *bwa mem* tools 
use by default 8 `threads` and `core_memory` of 6.0.

### Config

In the config there is the possibility to set the resources.

- **core_memory**: This overrides the default of the extension
- **threads**: This overrides the default of the extension
- **resident_factor**: This overrides the default of the extension
- **vmem_factor**: This overrides the default of the extension

- **vmem**: Sets a fixed vmem, **When this is set the retries won't raise the *vmem* anymore**
- **memory_limit**: Sets a fixed memory limit, **When this is set the retries won't raise the *memory limit* anymore**
- **resident_limit**: Sets a fixed resident limit, **When this is set the retries won't raise the *resident limit* anymore**

### Retry

In Biopet the number of retries is set to 5 on default. The first retry does not use an increased memory, starting from the 2nd 
retry the memory will automatically be increases, according to the calculations mentioned in [Values per core](#Values per core).
