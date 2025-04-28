@echo off
REM Quick build script for MainServlet.java and PickGreeting.java

setlocal
set JAVA_FILES=MainServlet.java PickGreeting.java
set OUT_DIR=out
set JAR_NAME=MyServlets.jar

REM Create output directory
if not exist %OUT_DIR% mkdir %OUT_DIR%

REM Compile Java files
javac -d %OUT_DIR% %JAVA_FILES%
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

REM Package into a jar
cd %OUT_DIR%
jar cf ../%JAR_NAME% *
cd ..

REM Done
echo Built %JAR_NAME% in %CD%\%JAR_NAME%
