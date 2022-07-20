FROM openjdk:17-jdk-alpine
VOLUME /tmp

RUN mkdir logic
COPY ./external/advertiser-calls.json logic
COPY ./external/advertiser-logic.json logic

COPY target/*.jar advertiser.jar
ENTRYPOINT ["java","-jar","/advertiser.jar"]