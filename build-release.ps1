param(
    [string]$VersionName = "",
    [int]$VersionCode = 0,
    [switch]$Release = $false
)

$ErrorActionPreference = "Stop"

$gradleFile = "app\build.gradle.kts"
$content = Get-Content $gradleFile -Raw

if ($VersionName -eq "" -or $VersionCode -eq 0) {
    $currentVersionName = if ($content -match 'versionName\s*=\s*"([^"]+)"') { $Matches[1] } else { "0.0.0" }
    $currentVersionCode = if ($content -match 'versionCode\s*=\s*(\d+)') { [int]$Matches[1] } else { 0 }

    $parts = $currentVersionName.Split('.')
    if ($parts.Count -eq 3) {
        $major = [int]$parts[0]
        $minor = [int]$parts[1]
        $patch = [int]$parts[2] + 1
        if ($VersionName -eq "") { $VersionName = "$major.$minor.$patch" }
    } else {
        if ($VersionName -eq "") { $VersionName = "1.0.0" }
    }
    if ($VersionCode -eq 0) { $VersionCode = $currentVersionCode + 1 }
}

Write-Host "=== Fazenda App Build ===" -ForegroundColor Cyan
Write-Host "VersionName: $VersionName" -ForegroundColor Yellow
Write-Host "VersionCode: $VersionCode" -ForegroundColor Yellow
Write-Host "Release: $Release" -ForegroundColor Yellow

$content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$VersionName`""
$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $VersionCode"
Set-Content $gradleFile $content -NoNewline

Write-Host "`nUpdated build.gradle.kts" -ForegroundColor Green

$buildType = if ($Release) { "Release" } else { "Debug" }
Write-Host "`nBuilding $buildType APK..." -ForegroundColor Cyan

if ($Release) {
    $result = & .\gradlew.bat :app:assembleRelease --no-daemon 2>&1
} else {
    $result = & .\gradlew.bat :app:assembleDebug --no-daemon 2>&1
}

$output = $result -join "`n"
Write-Host $output

if ($LASTEXITCODE -ne 0) {
    Write-Host "`nBUILD FAILED" -ForegroundColor Red
    exit 1
}

$buildDir = "app\build\outputs\apk\$($buildType.ToLower())"
$outputDir = "dist"
if (-not (Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir | Out-Null }

$apkFiles = Get-ChildItem -Path $buildDir -Filter "*.apk"
$sourceApk = $apkFiles | Where-Object { $_.Name -match "$($buildType.ToLower())" } | Select-Object -First 1
if (-not $sourceApk) { $sourceApk = $apkFiles | Select-Object -First 1 }

if ($sourceApk) {
    $destName = "Fazenda-v$VersionName-$buildType.apk"
    Copy-Item $sourceApk.FullName "$outputDir\$destName"
    Write-Host "`nAPK: $outputDir\$destName" -ForegroundColor Green
    Write-Host "Size: $([math]::Round((Get-Item "$outputDir\$destName").Length / 1MB, 2)) MB" -ForegroundColor Green
} else {
    Write-Host "`nAPK not found in $buildDir" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Build Complete ===" -ForegroundColor Cyan
