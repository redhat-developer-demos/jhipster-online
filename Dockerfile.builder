FROM registry.redhat.io/ubi8/openjdk-21:latest
LABEL authors="Maximiliano Pizarro"
LABEL description="JHipster pipeline builder: JDK 21 + Maven 3.9 + Node 22"

USER root
RUN microdnf install -y unzip tar xz gzip curl git && microdnf clean all

# Maven 3.9.15
WORKDIR /usr/share/maven
RUN rm -rf /usr/share/maven/* /usr/bin/mvn && \
    curl -L -o maven.zip https://dlcdn.apache.org/maven/maven-3/3.9.15/binaries/apache-maven-3.9.15-bin.zip && \
    unzip maven.zip && \
    ln -s /usr/share/maven/apache-maven-3.9.15/bin/mvn /usr/bin/mvn && \
    rm maven.zip

# Node.js 22
ENV NODE_VERSION=22.19.0
RUN ARCH=$(uname -m) && \
    case "$ARCH" in x86_64) NODE_ARCH=x64;; aarch64) NODE_ARCH=arm64;; ppc64le) NODE_ARCH=ppc64le;; s390x) NODE_ARCH=s390x;; *) echo "unsupported arch: $ARCH"; exit 1;; esac && \
    curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz" \
      -o /tmp/node.tar.xz && \
    tar -xJf /tmp/node.tar.xz -C /usr/local --strip-components=1 && \
    rm /tmp/node.tar.xz

ENV MAVEN_VERSION=3.9.15
ENV M2_HOME=/usr/share/maven/apache-maven-3.9.15
ENV PATH="${M2_HOME}/bin:/usr/local/bin:${PATH}"

USER 1001
WORKDIR /workspace
