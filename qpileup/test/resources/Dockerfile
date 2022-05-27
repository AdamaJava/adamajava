
#please put this Dockerfile to parent folder of adamajava
FROM openjdk:8-jre-slim
FROM gradle:6-jdk8 AS build
COPY . /app

ENV PATH="/app/hdf-java/hdf-java-2.8.0/bin:${PATH}"
ENV LD_LIBRARY_PATH=/app/hdf-java/hdf-java-2.8.0/lib/linux
WORKDIR /app/adamajava

#run qpileup, current pwd is /app/adamajava
CMD bash $(pwd)/qpileup/test/resources/run.sh $(pwd)
