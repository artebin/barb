#!/usr/bin/env sh
MYSELF="$(readlink -f $0)"
if [ $? -ne 0 ] | [ ! -f "${MYSELF}" ]; then
	printf "Cannot retrieve path to script[%s]\n" "${MYSELF}" >&2
	exit 1
fi
JAVA_PATH=java
if test -n "${JAVA_HOME}"; then
	JAVA_PATH="${JAVA_HOME}/bin/java"
fi
exec "${JAVA_PATH}" -jar "${MYSELF}" $@
exit 1
