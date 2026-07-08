#!/bin/sh
set -e

OUTPUTS_DIR="/opt/openems-edge/outputs"
DEFAULT_OUTPUTS_DIR="/opt/openems-edge/defaults/outputs"
SYNC_OUTPUTS="${ZIOT_SYNC_OUTPUTS:-true}"

if [ "${SYNC_OUTPUTS}" = "true" ]; then
	mkdir -p "${OUTPUTS_DIR}"
	if [ -f "${OUTPUTS_DIR}/deviceConfig_openems_fields.conf" ] \
		&& ! cmp -s "${DEFAULT_OUTPUTS_DIR}/deviceConfig_openems_fields.conf" "${OUTPUTS_DIR}/deviceConfig_openems_fields.conf"; then
		cp -a "${OUTPUTS_DIR}/deviceConfig_openems_fields.conf" \
			"${OUTPUTS_DIR}/deviceConfig_openems_fields.conf.bak.$(date +%Y%m%d%H%M%S)"
	fi
	cp -a "${DEFAULT_OUTPUTS_DIR}/." "${OUTPUTS_DIR}/"
fi

exec java ${JAVA_OPTS:-} \
	-Dfelix.cm.dir=/opt/openems-edge/config \
	-Dopenems.data.dir=/opt/openems-edge/data \
	-Dorg.osgi.service.http.port=8080 \
	-jar /opt/openems-edge/ziot-edge.jar
