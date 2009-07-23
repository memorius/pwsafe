#!/bin/bash

if [ $(id -u) != "0" ]; then
    echo "This install script must be run as root"
    exit 1
fi

PROG_NAME=pwsafe
DIST_JAR_DIR=dist

# JAVA_RUNTIME=java
JAVA_RUNTIME=/usr/lib/jvm/java-6-sun/bin/java

INSTALLED_BIN_DIR=/usr/local/bin
INSTALLED_JAR_DIR="/usr/local/share/${PROG_NAME}"

DIST_JAR="${DIST_JAR_DIR}/${PROG_NAME}.jar"
INSTALLED_BIN_SCRIPT="${INSTALLED_BIN_DIR}/${PROG_NAME}"

if [ ! -f "${DIST_JAR}" ]; then
    echo "File '${DIST_JAR}' does not exist - please run 'ant dist' first."
    exit 1
fi

mkdir -p "${INSTALLED_JAR_DIR}"
mkdir -p "${INSTALLED_BIN_DIR}"

cp -r "${DIST_JAR_DIR}"/* "${INSTALLED_JAR_DIR}"
chown -R root:root "${INSTALLED_JAR_DIR}"
chmod -R a=rX,u+w "${INSTALLED_JAR_DIR}"

cat > "${INSTALLED_BIN_SCRIPT}" <<-END
	#!/bin/bash
	
	exec "$JAVA_RUNTIME" -jar "${INSTALLED_JAR_DIR}/${PROG_NAME}.jar" "\$@"
	END

chown root:root "${INSTALLED_BIN_SCRIPT}"
chmod a=rx,u+w "${INSTALLED_BIN_SCRIPT}"

echo "Install finished ok."
echo "Run '${INSTALLED_BIN_SCRIPT}' (or just '${PROG_NAME}') to start the program."
