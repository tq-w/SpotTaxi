FROM openjdk:17-jdk-slim
LABEL authors="wtq"

# 设置工作目录
WORKDIR /app

# 将 JAR 文件复制到容器中
ADD target/service-mq.jar app.jar

# 暴露应用程序端口（如果需要）
EXPOSE 8282

# 定义容器启动时要运行的命令
ENTRYPOINT ["java", "-jar","-Dspring.profiles.active=prod", "app.jar"]