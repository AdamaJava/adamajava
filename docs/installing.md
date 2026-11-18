# Installing adamajava

## Building individual tools from source

To build adamajava tools, you will need to have `git` and `gradle`
installed. This example shows how to build an executable `jar` file for
one adamajava tool - `qprofiler2` in this instance.

These instructions will work for tools [qannotate](../qannotate/),
[qbamfilter](../qbamfilter/),
[qbasepileup](../qbasepileup/),
[qcnv](../qcnv/),
[qcoverage](../qcoverage/),
[qmito](../qmito/),
[qmotif](../qmotif/),
[qmule](../qmule/),
[qprofiler](../qprofiler/),
[qprofiler2](../qprofiler2/),
[qsignature](../qsignature/),
[qsnp](../qsnp/),
[qsv](../qsv/),
and [qvisualise](../qvisualise/).
These instructions will _not_ work for [qpileup](../qpileup/) which has 
non-java dependencies and needs to be compiled with reference to that code.

If you want to make the adamajava tools available on a shared system such
as a server or cluster, you should probably ask your sysadmin to do the
install as they will have established processes for how and 
where software is installed.

The basic install process is

1. get a copy of adamajava
2. compile a tool
3. move the compiled tool to an install directory
4. edit `$PATH` so it can see the tool execution script

### Details
 
* Step 1: get a copy of adamajava

The easiest way to do this is to get a fresh clean copy of the adamajava
code using `git clone`:

~~~~{.bash}
git clone https://github.com/AdamaJava/adamajava.git
~~~~

* Step 2: compile a tool

We are going to compile `qprofiler2` as our example tool.  Change
directory into the directory you cloned from git and use the gradle
wrapper `gradlew` to build a target:

~~~~{.bash}
cd adamajava
./gradlew :qprofiler2:build
~~~~

If you don't already have the required version of `gradle` installed,
the wrapper will install it for you.

The wrapper compiles the `qprofiler2.jar` file and copies it, along with
all the jar files on which it depends, into the `qprofiler2/build/flat`
folder. For tools that are packaged as applications (such as `qprofiler2`),
it also assembles them into a single tar file:
`qprofiler2/build/distributions/qprofiler2.tar`.

If we look inside this tar file we see that the `lib` subdirectory holds
all of the required jar files including `qprofiler2.jar` and the jars of
the adamajava libraries it uses (`qcommon.jar`, `qio.jar`, `qpicard.jar`)
plus their third-party dependencies (eg. `htsjdk`, `jopt-simple`), and the
`bin` directory contains a shell script `qprofiler2` (and a Windows
`qprofiler2.bat`) which we will want to put into `$PATH`.

~~~~{.bash}
qprofiler2/
qprofiler2/lib/
qprofiler2/lib/qprofiler2.jar
qprofiler2/lib/qcommon.jar
qprofiler2/lib/qio.jar
qprofiler2/lib/qpicard.jar
qprofiler2/lib/...            (third-party dependency jars)
qprofiler2/bin/
qprofiler2/bin/qprofiler2.bat
qprofiler2/bin/qprofiler2
~~~~

* Step 3: move the compiled tool to an install directory

Assuming that you have a directory called `/usr/local/adamajava` where
you would like to install the `qprofiler2` tool and you have permissions
to write to the install directory, use this command:

~~~~{.bash}
tar -xvf qprofiler2/build/distributions/qprofiler2.tar --cd /usr/local/adamajava
~~~~

* Step 4: edit `$PATH` so it can see the tool execution script

In order to use `qprofiler2` you need your shell to be able to find the
`qprofiler2` shell script so add something like the following to your
shell startup file (eg. `~/.bashrc`):

~~~~{.bash}
export PATH=/usr/local/adamajava/qprofiler2/bin:$PATH
~~~~
