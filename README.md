## How to build
`./mvnw clean install`

## How to run
`./mvnw spring-boot:run`

## For Intellij idea users
*This project needs enabled annotation processor cause of lombok dependency

## Additional instructions
At first you need create environment `docker pull mongo`,<br>
Run mongo  `docker run mongo (docker run mongo --port 27017)` ,<br>
or `./tools/test_cli env`

##execution example
* `./tools/test_cli env_start`
* `./tools/test_cli mongo_fill`


