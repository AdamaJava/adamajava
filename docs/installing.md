# Installing adamajava

## Building individual tools from source

To build adamajava tools, you will need to have `git` and `gradle`
installed. This example shows how to build an executable `jar` file for
one adamajava tool - `qprofiler2` in this instance.

These instructions will work for tools [qbamfilter](../qbamfilter/),
[qbasepileup](../qbasepileup/)
[qcnv](../qcnv/),
[qcoverage](../qcoverage/),
[qmito](../qmito/),
[qmotif](../qmotif/),
[qmule](../qmule/),
[qprofiler](../qprofiler/),
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

The wrapper compiles the `qprofiler2.jar` file and assembles it along with all
the jar files on which it depends in a single tar file:
`qprofiler2/build/distributions/qprofiler2.tar`.

If we look inside this tar file we see that the `lib`subdirectory holds
all of the required jar files including `qprofiler2.jar`, and the `bin` 
directory contains a shell
script `qprofiler2` which we will want to put into `$PATH`. 

~~~~{.bash}
drwxr-xr-x  0 0      0           0 May 30 14:51 qprofiler2/
drwxr-xr-x  0 0      0           0 May 30 14:51 qprofiler2/lib/
-rw-r--r--  0 0      0      124146 May 30 14:51 qprofiler2/lib/qprofiler2.jar
-rw-r--r--  0 0      0       53233 May 30 12:21 qprofiler2/lib/qpicard.jar
-rw-r--r--  0 0      0      122762 May 30 12:21 qprofiler2/lib/qio.jar
-rw-r--r--  0 0      0      223992 May 30 12:21 qprofiler2/lib/qcommon.jar
-rw-r--r--  0 0      0      812901 May 30 12:21 qprofiler2/lib/picard-1.130.jar
-rw-r--r--  0 0      0     1603567 May 30 12:21 qprofiler2/lib/htsjdk-2.14.1.jar
-rw-r--r--  0 0      0       62477 May 30 12:21 qprofiler2/lib/jopt-simple-4.6.jar
-rw-r--r--  0 0      0     1952352 May 30 12:21 qprofiler2/lib/commons-math3-3.3.jar
-rw-r--r--  0 0      0      434678 May 30 12:21 qprofiler2/lib/commons-lang3-3.4.jar
-rw-r--r--  0 0      0     2523218 May 30 12:21 qprofiler2/lib/trove4j-3.0.3.jar
-rw-r--r--  0 0      0      837129 May 30 12:21 qprofiler2/lib/aws-java-sdk-s3-1.11.241.jar
-rw-r--r--  0 0      0       31138 May 30 12:21 qprofiler2/lib/aws-v4-signer-java-1.3.jar
-rw-r--r--  0 0      0       15257 May 30 12:21 qprofiler2/lib/slf4j-simple-1.7.25.jar
-rw-r--r--  0 0      0       41203 May 30 12:21 qprofiler2/lib/slf4j-api-1.7.25.jar
-rw-r--r--  0 0      0      282549 May 30 12:21 qprofiler2/lib/config-1.3.1.jar
-rw-r--r--  0 0      0      102480 May 30 12:21 qprofiler2/lib/jaxb-api-2.2.12.jar
-rw-r--r--  0 0      0      267634 May 30 12:21 qprofiler2/lib/commons-jexl-2.1.1.jar
-rw-r--r--  0 0      0      403911 May 30 12:21 qprofiler2/lib/aws-java-sdk-kms-1.11.241.jar
-rw-r--r--  0 0      0      858022 May 30 12:21 qprofiler2/lib/aws-java-sdk-core-1.11.241.jar
-rw-r--r--  0 0      0      736658 May 30 12:21 qprofiler2/lib/httpclient-4.5.2.jar
-rw-r--r--  0 0      0       61829 May 30 12:21 qprofiler2/lib/commons-logging-1.2.jar
-rw-r--r--  0 0      0     1505728 May 30 12:21 qprofiler2/lib/snappy-java-1.1.4.jar
-rw-r--r--  0 0      0      241367 May 30 12:21 qprofiler2/lib/commons-compress-1.4.1.jar
-rw-r--r--  0 0      0       99555 May 30 12:21 qprofiler2/lib/xz-1.5.jar
-rw-r--r--  0 0      0       74133 May 30 12:21 qprofiler2/lib/ngs-java-1.2.4.jar
-rw-r--r--  0 0      0       26878 May 30 12:21 qprofiler2/lib/jmespath-java-1.11.241.jar
-rw-r--r--  0 0      0      836479 May 30 12:21 qprofiler2/lib/testng-6.8.8.jar
-rw-r--r--  0 0      0      565410 May 30 12:21 qprofiler2/lib/ion-java-1.0.2.jar
-rw-r--r--  0 0      0     1165323 May 30 12:21 qprofiler2/lib/jackson-databind-2.6.7.1.jar
-rw-r--r--  0 0      0       48468 May 30 12:21 qprofiler2/lib/jackson-dataformat-cbor-2.6.7.jar
-rw-r--r--  0 0      0      621931 May 30 12:21 qprofiler2/lib/joda-time-2.8.1.jar
-rw-r--r--  0 0      0      281694 May 30 12:21 qprofiler2/lib/bsh-2.0b4.jar
-rw-r--r--  0 0      0       55585 May 30 12:21 qprofiler2/lib/jcommander-1.27.jar
-rw-r--r--  0 0      0      326724 May 30 12:21 qprofiler2/lib/httpcore-4.4.4.jar
-rw-r--r--  0 0      0      263965 May 30 12:21 qprofiler2/lib/commons-codec-1.9.jar
-rw-r--r--  0 0      0       46968 May 30 12:21 qprofiler2/lib/jackson-annotations-2.6.0.jar
-rw-r--r--  0 0      0      258919 May 30 12:21 qprofiler2/lib/jackson-core-2.6.7.jar
drwxr-xr-x  0 0      0           0 May 30 14:51 qprofiler2/bin/
-rwxr-xr-x  0 0      0        4315 May 30 14:51 qprofiler2/bin/qprofiler2.bat
-rwxr-xr-x  0 0      0        6979 May 30 14:51 qprofiler2/bin/qprofiler2
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
`qprofiler2` shell script so add something like
