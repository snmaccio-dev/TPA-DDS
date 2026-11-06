# Keycloak pre-construido. Sin esto, `kc.sh start` rehace el build de Quarkus en cada
# arranque, y en un plan gratuito que duerme el servicio eso significa varios minutos en
# cada despertar. Aca ese trabajo se hace una sola vez, al construir la imagen.
#
#   docker build -f docker/keycloak.Dockerfile -t donatrack-keycloak .

FROM quay.io/keycloak/keycloak:24.0 AS build

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0

COPY --from=build /opt/keycloak/ /opt/keycloak/

# El realm viaja dentro de la imagen: al primer arranque contra una base vacia queda
# creado con sus roles, clients y service accounts. Los secrets van enmascarados, asi
# que despues de desplegar hay que regenerarlos desde la consola.
COPY keycloak/donatrack-realm.json /opt/keycloak/data/import/donatrack-realm.json

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--import-realm"]
