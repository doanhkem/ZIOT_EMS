FROM eclipse-temurin:21-jre

WORKDIR /opt/openems-edge

RUN mkdir -p /opt/openems-edge/config /opt/openems-edge/data

COPY build/openems-edge.jar /opt/openems-edge/openems-edge.jar

EXPOSE 8080 8084 8085

CMD ["java", \
  "-Dosgi.clean=true", \
  "-Dorg.apache.felix.eventadmin.Timeout=0", \
  "-Dorg.apache.felix.http.host=0.0.0.0", \
  "-Dorg.apache.felix.http.port=8080", \
  "-Dfelix.cm.dir=/opt/openems-edge/config", \
  "-Dopenems.data.dir=/opt/openems-edge/data", \
  "-Djava.net.preferIPv4Stack=true", \
  "-jar", "/opt/openems-edge/openems-edge.jar"]