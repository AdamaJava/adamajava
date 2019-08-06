### qannotate
Tool that annotates vcf files. It has a number of modes that perform various annotations. 

## Requirements
* Java 1.8

## Building qannotate

* **To build qannotate, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qannotate and its dependent jar files:
  ```
  ./gradlew :qannotate:build
  ```
  This creates the qannotate jar file along with dependent jars in the `qannotate/build/flat` folder

## Running qannotate

* **Run qannotate with the `--help` option to see a list of modes and required inputs:**
  ```
  java -jar qannotate/build/flat/qannotate-2.1.2.jar -i-help
  ```
  
