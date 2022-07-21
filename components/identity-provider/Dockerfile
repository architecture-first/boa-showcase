FROM openjdk:17-jdk
VOLUME /tmp

RUN mkdir tasklists

COPY target/*.jar identity-provider.jar
#ENTRYPOINT ["java","-jar","/identity-provider.jar"]
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=n","-Djava.security.egd=file:/dev/./urandom","-jar","/identity-provider.jar"]