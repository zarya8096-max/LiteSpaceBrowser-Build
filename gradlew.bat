@rem LiteSpace Browser Self-Bootstrapping Gradle Wrapper for Windows
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    powershell -NoProfile -Command "New-Item -ItemType Directory -Force -Path '%APP_HOME%\gradle\wrapper' | Out-Null; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_JAR%'"
)
if defined JAVA_HOME (
    set JAVA_EXE="%JAVA_HOME%/bin/java.exe"
) else (
    set JAVA_EXE="java.exe"
)
%JAVA_EXE% -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
