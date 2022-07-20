FROM openjdk:17-jdk-alpine
VOLUME /tmp

COPY target/*.jar vendor.jar
ENTRYPOINT ["java","-jar","/vendor.jar"]