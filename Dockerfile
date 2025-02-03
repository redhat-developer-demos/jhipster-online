FROM quay.io/devfile/universal-developer-image@sha256:1b84280bea96228affa7ecedd7347e6801f6e369bdfa7e40dabfc4fa99f9cad6

RUN npm install -y -g generator-jhipster-dotnetcore@4.2.0

RUN npm install -y -g generator-jhipster-micronaut@3.6.0

RUN npm install -y -g generator-jhipster-quarkus@3.4.0

RUN npm install -y -g generator-jhipster@8.8.0

RUN mkdir -p /projects/jhipster-online && chown -R user:user /projects/jhipster-online