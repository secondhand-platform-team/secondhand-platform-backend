# Stage 1: Build application
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper & config
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cache layer)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build jar
RUN ./mvnw package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-jar","app.jar"]