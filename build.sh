echo "==========Stop other build=========="
sh ./gradlew.sh --stop
echo "==========Clean temp================"
sh ./gradlew.sh clean
echo "==========Start build==============="
sh ./gradlew.sh assembleDebug --no-daemon 2>&1 | tee build.log
echo "==========Build end================="