FROM registry.redhat.io/devspaces/udi-rhel9:latest

USER root

RUN npm install -y -g generator-jhipster-dotnetcore@4.5.0 && \
    npm install -y -g generator-jhipster-micronaut@3.9.0 && \
    npm install -y -g generator-jhipster-quarkus@3.6.0 && \
    npm install -y -g generator-jhipster@9.0.0 && \
    npm install -y -g generator-jhipster-azure-container-apps

RUN mkdir -p /projects/jhipster-online && chown -R user:user /projects/jhipster-online

USER user
