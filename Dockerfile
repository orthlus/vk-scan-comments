FROM openjdk:17-alpine
ENV TZ=Europe/Moscow
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY target/yana-vk-scan-comments.jar .
ENTRYPOINT ["java", "-Dspring.profiles.active=production", "-jar", "yana-vk-scan-comments.jar"]