FROM openjdk:17-jdk-alpine
VOLUME /tmp

COPY target/*.jar cashier.jar
ENTRYPOINT ["java","-jar","/cashier.jar"]