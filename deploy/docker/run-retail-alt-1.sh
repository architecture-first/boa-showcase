#!/bin/bash
#set -o pipefail -e
#NOTE: you may need to run this command as "sudo run-retail.sh" or change "docker" to "sudo docker"
echo "Running ... "
MODE=${1:-"build"}
echo MODE=$MODE

# change directory to components
echo "build vicinity-platform"
pushd ../../components/vicinity-platform
mvn -DskipTests clean install
popd

echo "build business-retail"
pushd ../../components/business-retail
mvn -DskipTests clean install
popd

echo "build actors"
pushd ../..
mvn -DskipTests clean package
popd

echo "deploy docker dependencies"
pushd ../../components

if [ $MODE = "build" ] || [ $MODE = "up" ]; then
  docker-compose -p boaretail -f docker-compose.yml build
  docker tag redis boaretail_redis
  docker tag mongo boaretail_mongo
fi
if [ $MODE = "up" ]; then
  docker-compose -p boaretail -f docker-compose.yml up --build -d
  echo "wait for dependencies to start ..."
  sleep 30
fi

popd
echo "done"

