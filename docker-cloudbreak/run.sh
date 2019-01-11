#!/bin/bash

VERSION=$(date "+%m%d-%H%M")
IMAGE_NAME="cloudbreak"

echo "Docker build image: $IMAGE_NAME:$VERSION"

docker build -t ${IMAGE_NAME}:${VERSION} .
