# Sử dụng Maven bản chuẩn cho Java 21 để build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Ép Maven dùng bảng mã UTF-8 để không bị lỗi khi đọc tiếng Việt
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Môi trường chạy Java 21
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Ép Java dùng UTF-8 lúc chạy để không bị lỗi font khi insert vào Database
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]