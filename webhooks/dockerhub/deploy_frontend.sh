#!/usr/bin/env bash
docker rm -f frontend-$DOCKER_HUB_TAG
docker run -d \
  --name frontend-$DOCKER_HUB_TAG \
  -e VIRTUAL_HOST=frontend-${DOCKER_HUB_TAG}.ide-ci.codelife.me \
  -e VIRTUAL_PORT=80 \
  webide/frontend:$DOCKER_HUB_TAG
