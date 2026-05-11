FROM registry.redhat.io/devspaces/udi-rhel9:latest

RUN npm install -y -g generator-jhipster-dotnetcore@4.5.0

RUN npm install -y -g generator-jhipster-micronaut@3.9.0

RUN npm install -y -g generator-jhipster-quarkus@3.6.0

RUN npm install -y -g generator-jhipster@8.10.0

RUN mkdir -p /projects/jhipster-online && chown -R user:user /projects/jhipster-online