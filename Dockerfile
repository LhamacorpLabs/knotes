FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=build/libs/knotes.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-XX:MaxRAMPercentage=80","-XX:+UseSerialGC","-Xss512k","-XX:TieredStopAtLevel=1","-jar","/app.jar"]