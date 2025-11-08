# Heap Dump Tool

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.paypal/heap-dump-tool/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.paypal/heap-dump-tool)

Heap Dump Tool can sanitize sensitive data from Java heap dumps. Sanitization is accomplished
by replacing field values in the heap dump file with zero values. Heap dump can then be more freely shared freely and analyzed.

A typical scenario is when a heap dump needs to be sanitized before it can be given to another person or moved to a different
environment. For example, an app running in production environment may contain sensitive data (passwords, credit card
numbers, etc) which should not be viewable when the heap dump is copied to a development environment for analysis with a
graphical program.

<img src="https://github.com/paypal/heap-dump-tool/raw/statics/heap-dump-file.png"/>

---

<img src="https://github.com/paypal/heap-dump-tool/raw/statics/sanitized-heap-dump-file.png"/>

## TOC
  * [Examples](#examples)
  * [Usage](#usage)
  * [License](#license)
	
## Examples

The tool can be run in several ways depending on tool's packaging and where the target to-be-captured app is running.

#### [Jar] Capture sanitized heap dump manually

Simplest way to capture sanitized heap dump of an app is to run:

```
# capture plain heap dump of Java process with given pid
$ jcmd {pid} GC.heap_dump /path/to/plain-heap-dump.hprof

# then sanitize the heap dump
$ wget -O heap-dump-tool.jar https://repo1.maven.org/maven2/com/paypal/heap-dump-tool/1.3.3/heap-dump-tool-1.3.3-all.jar
$ java -jar heap-dump-tool.jar sanitize /path/to/plain-dump.hprof /path/to/sanitized-dump.hprof
```

<br/>


### [Library] Embed within an app

To use it as a library and embed it within another app, you can declare it as dependency in maven:

```
<dependency>
  <groupId>com.paypal</groupId>
  <artifactId>heap-dump-tool</artifactId>
  <version>1.3.3</version>
</dependency>
```

<a name="usage"></a>

## Usage

```
java -jar heap-dump-tool.jar  help
Usage: heap-dump-tool [-hV] [COMMAND]
Tool for sanitizing heap dumps
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  sanitize  Sanitize a heap dump by replacing byte and char array contents
  help      Displays help information about the specified command
```

Additional usage for sub-commands can be found by running `help {sub-command}`.

<a name="license"></a>

## Whitepaper

See [whitepaper (pdf)](https://github.com/paypal/heap-dump-tool/blob/statics/whitepaper.pdf)

## License

Heap Dump Tool is Open Source software released under the Apache 2.0 license.

