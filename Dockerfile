FROM openjdk:11-jdk

ARG JARFILE
ADD ${JARFILE} /test-rabbit-webservice.jar

ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

# Allow access to springboot built in services.
EXPOSE 8080

ENTRYPOINT java -cp app:app/lib/* ${JAVA_OPTS} au.com.bushlife.integration.test.utils.rabbit.Application