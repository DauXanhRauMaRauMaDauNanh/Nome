@echo off
set PATH=C:\Program Files\Java\jdk-25.0.4\bin;C:\msys64\ucrt64\bin;%PATH%
echo [Nome] Running ultra-lean Java bootstrap with 16MB-32MB JVM heap limit...
java -Xms16m -Xmx32m -XX:+UseSerialGC --enable-native-access=ALL-UNNAMED -jar Nome.jar %*
pause
