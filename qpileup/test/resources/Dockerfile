

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
CMD bash run.sh
