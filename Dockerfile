FROM --platform=$BUILDPLATFORM amazoncorretto:8 AS builder

WORKDIR /tmp/

ENV APP_ID=heap-dump-tool
ENV APP_JAR=/opt/heap-dump-tool/$APP_ID.jar
COPY src/main/docker/docker-entrypoint.sh /
COPY target/$APP_ID.jar $APP_JAR

RUN chmod ugo+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
