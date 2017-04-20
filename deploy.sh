#!/usr/bin/env bash
cd entrypoint
./gradlew clean shadowJar

cd ..
docker-compose build
docker-compose up -d
