FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests
EXPOSE 8080
CMD ["java","-jar","target/backend-odonto-0.0.1-SNAPSHOT.jar"]
