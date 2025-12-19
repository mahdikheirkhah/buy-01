# 1. Your Docker Hub Username
export DOCKER_USERNAME="mahdikheirkhah"

# 2. The Personal Access Token you generated (RECOMMENDED over using your main password)

# 3. Registry URL:
# If you are using Docker Hub, you can omit this, as the script defaults to 'docker.io'.
# If you are using a different service (like GitHub's registry), you would set it to that URL.
# Since you likely use Docker Hub, we can skip setting DOCKER_REGISTRY,
# or explicitly set it like this (optional):
export DOCKER_REGISTRY="docker.io"

export IMAGE_TAG="v1.0.0"