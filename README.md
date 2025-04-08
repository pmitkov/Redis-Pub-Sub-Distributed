### Installation and running the app

```
./mvnw clean install
./mvnw spring-boot:run
```

Alternatively if you want to run multiple instances of the application in order to simulate 
a larger than 1 consumer group size you can run separate jvm instances
from the .jar file in the target directory by running
`java -jar Redis-Pub-Sub-Distributed-1.0-SNAPSHOT.jar`

### Running unit tests separately

```
./mvnw test
```

### Configuration

Configuration data is located in `src/main/resources/`

The configuration file for configuring the connection 
properties is `src/main/resources/redis.properties`

