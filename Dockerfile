FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_2.13.8

WORKDIR /app

# Copy build files
COPY build.sbt ./
COPY project/ ./project/

# Download dependencies
RUN sbt update

# Copy source code
COPY src/ ./src/

# Build the application
RUN sbt assembly

# The JAR will be in target/scala-2.13/