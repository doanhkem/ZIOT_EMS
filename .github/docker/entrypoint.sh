#!/bin/sh
set -e

OUTPUTS_DIR="/opt/openems-edge/outputs"
DEFAULT_OUTPUTS_DIR="/opt/openems-edge/defaults/outputs"

if [ ! -f "${OUTPUTS_DIR}/deviceConfig_openems_fields.conf" ]; then
	mkdir -p "${OUTPUTS_DIR}"
	cp -a "${DEFAULT_OUTPUTS_DIR}/." "${OUTPUTS_DIR}/"
fi

exec java ${JAVA_OPTS:-} \
	-Dfelix.cm.dir=/opt/openems-edge/config \
	-Dopenems.data.dir=/opt/openems-edge/data \
	-Dorg.osgi.service.http.port=8080 \
	-jar /opt/openems-edge/ziot-edge.jar
