FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends ffmpeg ca-certificates libasound2 libssl3 \
	&& rm -rf /var/lib/apt/lists/*

RUN groupadd --system arkive && useradd --system --gid arkive arkive

COPY --from=build /workspace/target/arkive-0.0.1-SNAPSHOT.jar /app/app.jar

USER arkive

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
