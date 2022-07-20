FROM openjdk:17-jdk-alpine
VOLUME /tmp

RUN mkdir tasklists
COPY tasklists tasklists

COPY target/*.jar customer.jar
#ENTRYPOINT ["java","-jar","/customer.jar"]
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,address=*:5006,server=y,suspend=n","-Djava.security.egd=file:/dev/./urandom","-jar","/customer.jar"]