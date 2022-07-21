FROM openjdk:17-jdk
VOLUME /tmp

RUN mkdir logic
COPY ./external/merchant-calls.json logic

RUN mkdir tasklists
COPY tasklists/ViewProduct.json tasklists
COPY tasklists/ViewProducts.json tasklists
COPY tasklists/CheckoutAsRegisteredCustomer.json tasklists
COPY tasklists/ReviewInventory.json tasklists

COPY target/*.jar merchant.jar
ENTRYPOINT ["java","-jar","/merchant.jar"]