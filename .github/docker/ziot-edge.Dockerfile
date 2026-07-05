FROM eclipse-temurin:21-jre

WORKDIR /opt/openems-edge

COPY build/ziot-edge.jar /opt/openems-edge/ziot-edge.jar
COPY outputs/ /opt/openems-edge/defaults/outputs/
COPY .github/docker/entrypoint.sh /usr/local/bin/ziot-edge-entrypoint.sh

RUN mkdir -p /opt/openems-edge/config /opt/openems-edge/data /opt/openems-edge/outputs \
	&& sed -i 's/\r$//' /usr/local/bin/ziot-edge-entrypoint.sh \
	&& chmod +x /usr/local/bin/ziot-edge-entrypoint.sh

EXPOSE 8080

VOLUME ["/opt/openems-edge/config", "/opt/openems-edge/data", "/opt/openems-edge/outputs"]

ENTRYPOINT ["/usr/local/bin/ziot-edge-entrypoint.sh"]
