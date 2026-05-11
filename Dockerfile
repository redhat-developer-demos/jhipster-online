FROM registry.redhat.io/devspaces/udi-rhel9:latest

USER root

# JHipster 9 requires Node ^22.18; UDI defaults may ship Node 20. Install official Node + yarn on PATH first.
ENV NODE_VERSION=22.19.0
ENV PATH=/usr/local/bin:/usr/bin:/bin

RUN INSTALL_PKGS="tar xz gzip curl" \
    && if command -v dnf >/dev/null 2>&1; then \
         dnf -y install $INSTALL_PKGS && dnf clean all; \
       elif command -v microdnf >/dev/null 2>&1; then \
         microdnf install -y $INSTALL_PKGS && microdnf clean all; \
       else \
         echo "Neither dnf nor microdnf found"; exit 1; \
       fi \
    && ARCH=$(uname -m) \
    && case "$ARCH" in x86_64) NODE_ARCH=x64 ;; aarch64) NODE_ARCH=arm64 ;; ppc64le) NODE_ARCH=ppc64le ;; s390x) NODE_ARCH=s390x ;; *) echo "unsupported arch: $ARCH"; exit 1 ;; esac \
    && curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz" -o /tmp/node.tar.xz \
    && tar -xJf /tmp/node.tar.xz -C /usr/local --strip-components=1 \
    && rm -f /tmp/node.tar.xz \
    && node -v \
    && npm install -g yarn \
    && npm install -g generator-jhipster-dotnetcore@4.5.0 \
    && npm install -g generator-jhipster-micronaut@3.9.0 \
    && npm install -g generator-jhipster-quarkus@3.6.0 \
    && npm install -g generator-jhipster@9.0.0 \
    && npm install -g generator-jhipster-azure-container-apps

RUN mkdir -p /projects/jhipster-online && chown -R user:user /projects/jhipster-online

USER user
