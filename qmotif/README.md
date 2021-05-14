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
* **To run qmotif, you need to supply an ini file**
  ```
  java -jar qmotif.jar \
            --threads 8 \
            --input-bam /mydata/bamFile.bam \
            --input-bai /mydata/bamFile.bam.bai \
            --log /mydata/bamFile.bam.qmotif.log \
            --loglevel DEBUG \
            -ini /mydata/qmotif.ini \
            -output-xml /mydata/bamFile.bam.qmotif.xml \
            -output-bam /mydata/bamFile.telomere.bam
  ```

## More details
More details can be found in the [qmotif](https://github.com/AdamaJava/adamajava/blob/qmotif.doc/docs/qmotif/qmotif.md)  adamajava readthedocs page.
