$ErrorActionPreference = "Stop"
cd $PSScriptRoot

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  Write-Host "Installing JDK 17 via winget..."
  winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-package-agreements --accept-source-agreements
}

if (-not (Test-Path .\MedStormSDC.jar)) {
  .\gradlew.bat bootJar -x test
  $jar = Get-ChildItem .\build\libs\*.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  Copy-Item $jar.FullName .\MedStormSDC.jar -Force
}

$MDIB     = "C:\MedStorm\sdc-bridge\mdib\medstorm-mdib-draeger-v2.xml"
$KEYSTORE = "C:\MedStorm\sdc-bridge\certs\medstorm.p12"
$KSPASS   = "changeit"

& (Get-Command java).Path -jar ".\MedStormSDC.jar" `
  --bridge.mdibPath="$MDIB" `
  --bridge.keystorePath="$KEYSTORE" `
  --bridge.keystorePassword="$KSPASS" `
  --server.port=8080
