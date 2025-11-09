# Heap Dump Tool

This is a modified version of https://github.com/paypal/heap-dump-tool

This version of Heap Dump Tool cannot sanitize sensitive data from Java heap dumps!  
It is instead used to make comparisons of different heap dumps easier.  
It does so by clearing out selected fields in selected classes.  
This allows to hide objects in caches by making them unreachable.  

---

## TOC
  * [Examples](#examples)
  * [Usage](#usage)
  * [License](#license)
	
## Examples

#### Capture and normalized a heap dump

Simplest way to capture normalized heap dump of an app is to run:

```
# capture plain heap dump of Java process with given pid
$ jcmd {pid} GC.heap_dump /path/to/plain-heap-dump.hprof

# then normalize the heap dump
$ java -jar heap-dump-tool-all.jar normalize /path/to/plain-dump.hprof /path/to/normalized-dump.hprof
```

<br/>

<a name="usage"></a>

## Usage

```
java -jar heap-dump-tool.jar  help
Usage: heap-dump-tool [-hV] [COMMAND]
Tool for normalizing heap dumps
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  normalize  Normalize a heap dump for easier comparison by clearing some class fields
  help       Displays help information about the specified command
```

Additional usage for sub-commands can be found by running `help {sub-command}`.

<a name="license"></a>

## License

Heap Dump Tool is Open Source software released under the Apache 2.0 license.

