#!/bin/bash
#set -o pipefail -e
#NOTE: you may need to run this command as "sudo run-retail-k8s.sh" or change "docker" to "sudo docker"
echo "Running ..."
echo "Arg= $1"
MODE=${1:-"--install"}
echo MODE=$MODE

if [ $MODE = "-h" ] || [ $MODE = "--help" ]; then
  echo "usage: "
  echo "      --install: [Default] install pre-built images"
  echo "      --build-and-install: perform maven, npm and docker builds then install from local images"
  exit 1
fi

if [ $MODE = "--build-and-install" ]; then
  echo "build images"
  . ../docker/run-retail.sh build
fi

# create local registry if it does not exist
if [[ "$(docker images -q registry:2 2> /dev/null)" == "" ]]; then
  docker run -d -p 5000:5000 --restart=always --name local-registry registry:2
fi

# tag and push images
for img in $(docker image ls | cut -f1 -d ' ' | grep "^boaretail"); do
    echo "docker tag $img localhost:5000/$img"
    docker tag $img localhost:5000/$img:latest
    echo "docker push localhost:5000/$img"
    docker push localhost:5000/$img
done

#setup
kubectl apply -k kustomize/setup/overlays/local/namespaces
kubectl apply -k kustomize/setup/overlays/local/serviceaccounts/
kubectl apply -k kustomize/setup/overlays/local/roles/
kubectl apply -k kustomize/setup/overlays/local/configmaps/
kubectl apply -k kustomize/setup/overlays/local/secrets/
kubectl apply -k kustomize/setup/overlays/local/services/

#deployments
kubectl apply -k kustomize/deploy/overlays/local/deployments/

echo "done"

