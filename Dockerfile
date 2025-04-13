FROM eclipse-temurin:22-jdk
MAINTAINER petko.mitkov
COPY target/Redis-Pub-Sub-Distributed-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]