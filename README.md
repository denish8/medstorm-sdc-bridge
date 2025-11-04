# sdc-bridge

Spring Boot + SDCri provider. Broadcasts Hello on UDP/3702 and exposes device endpoints.

## Build
.\gradlew clean bootJar -x test

## Run
java -Dsdc.nic=Wi-Fi -Dsdc.epr=urn:uuid:medstorm-sensor-1 -DDpws.HttpServerPort=53200 -jar .\build\libs\sdc-bridge-0.0.1-SNAPSHOT.jar
