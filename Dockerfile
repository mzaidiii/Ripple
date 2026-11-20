# Stage 1: Build application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and descriptor
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Cache dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and package jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Production JRE runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}

# Optimize JVM memory allocation for containers (e.g. Render 512MB RAM limit)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
