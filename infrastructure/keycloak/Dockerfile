FROM quay.io/keycloak/keycloak:24.0.1

# Copy realm config
COPY realm-export.json /opt/keycloak/data/import/realm-export.json

# Build optimized image
RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", \
  "--import-realm", \
  "--http-enabled=true", \
  "--hostname-strict=false", \
  "--hostname-strict-https=false"]
