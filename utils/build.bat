
@echo off
if not exist "bin" mkdir bin

::SETLOCAL EnableDelayedExpansion
cd src
set javaFiles=
for /r %%i in (*.java) do set javaFiles=!javaFiles! %%i
echo %javaFiles%
cd ..
::ENDLOCAL

::javac -cp %javaFiles% -d bin

jar cvMf C:\SmartFoxServer_2X\SFS2X-5000\extensions\__lib__\mmory-libs.jar -C bin .