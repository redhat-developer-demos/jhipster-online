FROM registry.redhat.io/devspaces/udi-rhel9:latest

USER root

# JHipster 9 requires Node ^22.18; UDI may ship Node 20. Official Linux tarball + gzip
# avoids dnf/microdnf (subscription / lock issues in CI).
ENV NODE_VERSION=22.19.0
ENV PATH=/usr/local/bin:/usr/bin:/bin

RUN ARCH=$(uname -m) \
    && case "$ARCH" in x86_64) NODE_ARCH=x64 ;; aarch64) NODE_ARCH=arm64 ;; ppc64le) NODE_ARCH=ppc64le ;; s390x) NODE_ARCH=s390x ;; *) echo "unsupported arch: $ARCH"; exit 1 ;; esac \
    && curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.gz" -o /tmp/node.tar.gz \
    && tar -xzf /tmp/node.tar.gz -C /usr/local --strip-components=1 \
    && rm -f /tmp/node.tar.gz \
    && node -v \
    && npm install -g generator-jhipster@9.0.0 \
    && npm install -g generator-jhipster-quarkus@4.0.0 \
    && npm install -g generator-jhipster-micronaut@4.0.0 \
    && npm install -g generator-jhipster-rust@1.0.0

RUN mkdir -p /projects/jhipster-online && chown -R 1001:0 /projects/jhipster-online

USER user
