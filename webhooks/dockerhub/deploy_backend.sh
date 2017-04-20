#!/usr/bin/env bash
docker rm -f backend-$DOCKER_HUB_TAG
docker run -d \
  --name backend-$DOCKER_HUB_TAG \
  -e VIRTUAL_HOST=backend-${DOCKER_HUB_TAG}.ide-ci.codelife.me \
  -e VIRTUAL_PORT=8080 \
  -e ALLOWED_ORIGINS=* \
  -v coding-ide-home:/root/.coding-ide \
  webide/backend:$DOCKER_HUB_TAG
