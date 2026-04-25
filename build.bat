@echo off
REM Build script for ANTLR project

echo Downloading ANTLR...
if not exist antlr-4.11.1-complete.jar (
    powershell -Command "Invoke-WebRequest -Uri 'https://www.antlr.org/download/antlr-4.11.1-complete.jar' -OutFile 'antlr-4.11.1-complete.jar'"
)

where javac >nul 2>nul
if errorlevel 1 (
    echo javac not found. Install JDK and add it to PATH.
    exit /b 1
)

echo Compiling...
javac -cp "antlr-4.11.1-complete.jar" -d out gen\grammar\*.java src\SymbolTable\*.java src\interpreter\*.java src\player\*.java

if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)

echo Build successful!
echo.
echo To run:
echo   java -cp "out;antlr-4.11.1-complete.jar" interpreter.Start cat_video.first