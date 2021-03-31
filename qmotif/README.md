### qmotif
Tool that runs against a BAM file searching for user-specified motifs

## Requirements
* Java 1.8

## Building qmotif

* **To do a build of qmotif, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qmotif and its dependent jar files:
  ```
  ./gradlew :qmotif:build
  ```
  This creates the qmotif jar file along with dependent jars in the `qmotif/build/flat` folder

## Running qmotif
* **To run qmotif, you need to supply an ini file and two outputs:**
  ```
  java -jar flat/qmotif.jar \
            -n 8 \
            --bam /mydata/bamFile.bam \
            --bai /mydata/bamFile.bam.bai \
            --log /mydata/bamFile.bam.qmotif.log \
            --loglevel DEBUG \
            -ini /mydata/qmotif.ini \
            -o /mydata/bamFile.bam.qmotif.xml \
            -o /mydata/bamFile.telomere.bam
  ```

## More details
More details can be found in the [qmotif](https://github.com/AdamaJava/adamajava/blob/qmotif.doc/docs/qmotif/qmotif_1_3.md)  adamajava readthedocs page.
