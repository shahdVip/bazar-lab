# 🐳 Base image with JDK and Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build

# نسخ الملفات للمجلد الداخلي داخل الكونتينر
WORKDIR /app
COPY . .

# بناء المشروع
RUN mvn clean package

# ============================
# المرحلة الثانية: تشغيل التطبيق فقط
FROM eclipse-temurin:17-jdk

WORKDIR /app

# نسخ الجار (JAR) الجاهز من مرحلة البناء
COPY --from=build /app/target/catalog-service.jar app.jar

# تشغيل التطبيق
CMD ["java", "-jar", "app.jar"]
