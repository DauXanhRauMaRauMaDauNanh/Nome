@echo off
set PATH=C:\Program Files\Java\jdk-25.0.4\bin;C:\msys64\ucrt64\bin;%PATH%
java -XX:+UseZGC --enable-native-access=ALL-UNNAMED -jar Nome.jar
pause
