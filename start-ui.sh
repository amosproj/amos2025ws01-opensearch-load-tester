#/bin/sh

#TODO argument to restart/rebuild UI

if [ -d "LoadTesterUI" ]; then
    ./LoadTesterUI/bin/LoadTesterUI
else
    # If ./LoadTesterUI is not found, try to build it
    mvn clean install -DskipTests
    cd ./ui
    mvn clean javafx:jlink
    cd ..
    cp -r ./ui/target/LoadTesterUI .
    ./LoadTesterUI/bin/LoadTesterUI
fi
