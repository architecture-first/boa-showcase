FROM openjdk:17-jdk
VOLUME /tmp

COPY target/*.jar cashier.jar
ENTRYPOINT ["java","-jar","/cashier.jar"]