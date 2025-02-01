#!/usr/bin/env sh
MYSELF="$(readlink -f "${BASH_SOURCE}")"
if [ "${?}" -ne 0 ] | [ ! -f "${MYSELF}" ]; then
	printf "!ERROR! Cannot find %s\n" "${MYSELF}" >&2
	exit 1
fi
JAVA_PATH=java
if test -n "${JAVA_HOME}"; then
	JAVA_PATH="${JAVA_HOME}/bin/java"
fi
exec "${JAVA_PATH}" -jar $MYSELF "$@"
exit 1
