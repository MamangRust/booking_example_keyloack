FROM eclipse-temurin:21 as build-env

ADD . /app

WORKDIR /app

RUN ./mvnw clean install -Dmaven.test.skip

FROM eclipse-temurin:21-ubi9-minimal

COPY --from=build-env /app/target/booking_keyclock-0.0.1-SNAPSHOT.jar /app/

WORKDIR /app

ENTRYPOINT [ "java", "-jar", "booking_keyclock-0.0.1-SNAPSHOT.jar" ]