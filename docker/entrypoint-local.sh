#!/bin/sh
set -e
if [ -n "${JHIPSTER_SLEEP:-}" ]; then
  echo "Waiting ${JHIPSTER_SLEEP}s for dependencies..."
  sleep "${JHIPSTER_SLEEP}"
fi
exec java ${JAVA_OPTS:-} -jar /app/jhonline.war --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod,local}"
