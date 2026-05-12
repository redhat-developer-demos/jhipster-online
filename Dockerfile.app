FROM jhipster/jhipster:v8.7.0
USER jhipster
COPY --chown=jhipster:jhipster . /home/jhipster/jhipster-online/
RUN \
    cd /home/jhipster/jhipster-online/ && \
    rm -Rf target node_modules && \
    chmod +x mvnw && \
    sleep 1 && \
    ./mvnw package -Pgcp -DskipTests && \
    mv /home/jhipster/jhipster-online/target/*.war /home/jhipster && \
    rm -Rf /home/jhipster/jhipster-online/ /home/jhipster/.m2 /home/jhipster/.cache /tmp/* /var/tmp/*

USER root
RUN set -eux; \
    ARCH="$(uname -m)"; \
    case "$ARCH" in x86_64) HELM_ARCH=amd64 ;; aarch64) HELM_ARCH=arm64 ;; *) HELM_ARCH=amd64 ;; esac; \
    curl -fsSL "https://get.helm.sh/helm-v3.14.4-linux-${HELM_ARCH}.tar.gz" -o /tmp/helm.tgz; \
    tar -xzf /tmp/helm.tgz -C /tmp; \
    mv "/tmp/linux-${HELM_ARCH}/helm" /usr/local/bin/helm; \
    chmod +x /usr/local/bin/helm; \
    rm -rf /tmp/helm.tgz "/tmp/linux-${HELM_ARCH}"; \
    helm version

USER root
RUN \
    npm install -g generator-jhipster-azure-spring-apps
    
RUN mkdir /projects && chown -R jhipster:jhipster /projects
USER jhipster

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS=""
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /home/jhipster/jhonline*.war
EXPOSE 8080
