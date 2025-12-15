FROM docker:24-dind

# Install required tools
RUN apk add --no-cache openjdk17 maven nodejs npm bash

# IMPORTANT: run container as root
USER root