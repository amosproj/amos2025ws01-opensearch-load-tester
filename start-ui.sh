#/bin/sh

if [ "$1" = "rebuild" ]; then
    echo "Rebuilding LoadTesterUI..."
    sudo rm -r ./LoadTesterUI
fi

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
