FROM openjdk:21-jdk-slim
RUN useradd -m -s /bin/bash appuser
WORKDIR /app
COPY /target/demo-0.0.1-SNAPSHOT.jar /app/demo.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
CMD ["java", "-jar", "demo.jar"]