FROM openjdk:17-jdk
VOLUME /tmp

COPY target/*.jar vendor.jar
ENTRYPOINT ["java","-jar","/vendor.jar"]