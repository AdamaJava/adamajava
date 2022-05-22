

FROM openjdk:8-jre-slim
FROM gradle:6-jdk8 AS build
COPY . /app
#CMD: pwd is /home/gradle, because after download, workdir go to /home/gradle
#CMD echo "CMD: pwd is $PWD"; ls -alh $PWD; gradle build; ls -alh /app

#change workdir, then output: CMD: pwd is /app
#WORKDIR /app
#CMD echo "CMD: pwd is $PWD"; ls -alh $PWD; gradle build

#change workdir, then output: CMD: pwd is /app/adamajava
#WORKDIR /app/adamajava
#CMD echo "CMD: pwd is $PWD"; ls -alh $PWD; gradle build

ENV PATH="/app/hdf-java/hdf-java-2.8.0/bin:${PATH}"
ENV LD_LIBRARY_PATH=/app/hdf-java/hdf-java-2.8.0/lib/linux
WORKDIR /app/adamajava


#run qpileup
CMD echo "CMD: pwd is $PWD"; ls -alh $PWD; gradle build;\
	 echo "debug af: PATH=$PATH; LD_LIBRARY_PATH=$LD_LIBRARY_PATH";\
	 ls -alh /app;\
	 pileup=/app/adamajava/qpileup/build/flat/qpileup.jar;\
	
#adamajava/qpileup/test/resources/test.bootstrap.ini:hdf=/app/adamajava/qpileup/test/resources/test-reference.boostrap.h5;\
	ini=/app/adamajava/qpileup/test/resources/test.bootstrap.ini;\
	java -jar $pileup --ini $ini;\
	hdf=/app/adamajava/qpileup/test/resources/test-reference.boostrap.h5;\
	java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.header;\

#adamajava/qpileup/test/resources/test.addA.ini:hdf=/app/adamajava/qpileup/test/resources/testA.h5
	ini=/app/adamajava/qpileup/test/resources/test.addA.ini;\
	hdfa=/app/adamajava/qpileup/test/resources/testA.h5;\
	cp $hdf $hdfa; \
	java -jar $pileup --ini $ini;\
	java  -jar $pileup  --view --hdf-header --hdf $hdfa > ${hdfa}.add.header;\

#adamajava/qpileup/test/resources/test.addB.ini:hdf=/app/adamajava/qpileup/test/resources/testB.h5
	ini=/app/adamajava/qpileup/test/resources/test.addB.ini;\
	hdfb=/app/adamajava/qpileup/test/resources/testB.h5;\
	cp $hdf $hdfb; \
	java -jar $pileup --ini $ini;\
	java  -jar $pileup  --view --hdf-header --hdf $hdfb > ${hdfb}.add.header;\

#adamajava/qpileup/test/resources/test.remove.ini:hdf=/app/adamajava/qpileup/test/resources/testA.h5
	ini=/app/adamajava/qpileup/test/resources/testA.remove.ini;\
	java -jar $pileup --ini $ini;\
	java  -jar $pileup  --view --hdf-header --hdf $hdfa > ${hdfa}.remove.header;\


#adamajava/qpileup/test/resources/test.merge.ini:hdf=/app/adamajava/qpileup/test/resources/test.merge.h5
	ini=/app/adamajava/qpileup/test/resources/test.merge.ini;\
	java -jar $pileup --ini $ini;\
	hdf=/app/adamajava/qpileup/test/resources/test.merge.h5;\
	java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.header;

