#!/usr/bin/env bash
cd backend
./gradlew clean shadowJar

cd ..
docker-compose build
docker-compose up -d
