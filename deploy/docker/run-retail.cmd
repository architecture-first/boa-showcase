echo "Running ..."
echo PATH = $PATH
echo JAVA_HOME = $JAVA_HOME

# Note: path to mvn and JAVA_HOME need to be defined

# change directory to components
echo "build vicinity-platform"
cd ..\..\components\vicinity-platform
call mvn -DskipTests clean install
echo "go back"
cd ..\..\deploy\docker
echo "back"

echo "build business-retail"
cd ..\..\components\business-retail
call mvn -DskipTests clean install
cd ..\..\deploy\docker

echo "build actors"
cd ..\..
call mvn -DskipTests clean package
cd deploy\docker

echo "deploy docker dependencies"
cd ..\..\components
docker compose -p boaretail -f docker-compose-init.yml build
docker compose -p boaretail -f docker-compose-init.yml up -d

echo "wait for dependencies to start ..."
timeout 30

docker compose -p boaretail -f docker-compose-customer.yml build
docker compose -p boaretail -f docker-compose-customer.yml up -d
cd ..\deploy\docker
echo "wait for actors to start ..."
timeout 60
echo "done"


