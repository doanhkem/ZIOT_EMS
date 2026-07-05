FROM eclipse-temurin:21-jre

WORKDIR /opt/ziot-edge

COPY build/ziot-edge.jar /opt/ziot-edge/ziot-edge.jar

RUN mkdir -p /opt/ziot-edge/config /opt/ziot-edge/data

EXPOSE 8080

VOLUME ["/opt/ziot-edge/config", "/opt/ziot-edge/data"]

ENTRYPOINT ["java", "-Dfelix.cm.dir=/opt/ziot-edge/config", "-Dopenems.data.dir=/opt/ziot-edge/data", "-Dorg.osgi.service.http.port=8080", "-jar", "/opt/ziot-edge/ziot-edge.jar"]
