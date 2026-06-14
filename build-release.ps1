param(
    [switch]$Publish
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$propsFile = Join-Path $projectDir "app\version.properties"

# ---------- Auto-increment version ----------
$code = 1; $name = "1.0.0"
if (Test-Path $propsFile) {
    $lines = Get-Content $propsFile
    $code = [int]((($lines | Where-Object { $_ -match "^VERSION_CODE=(.+)" }) | ForEach-Object { $matches[1] }) -replace "`r|`n")
    $name = (($lines | Where-Object { $_ -match "^VERSION_NAME=(.+)" }) | ForEach-Object { $matches[1] }) -replace "`r|`n"
}
$newCode = $code + 1
$parts = $name.Split('.')
if ($parts.Count -eq 3) {
    $patch = [int]$parts[2] + 1
    $newName = "$($parts[0]).$($parts[1]).$patch"
} else {
    $newName = "1.0.1"
}
"VERSION_CODE=$newCode`r`nVERSION_NAME=$newName" | Set-Content $propsFile
Write-Host "v$newName (code $newCode)" -ForegroundColor Cyan

# ---------- Read password from .env ----------
$envFile = Join-Path $projectDir ".env"
if (-not (Test-Path $envFile)) { Write-Host ".env not found" -ForegroundColor Red; exit 1 }
$envContent = Get-Content $envFile -Raw
if ($envContent -match "KEYSTORE_PASSWORD=(.+)") {
    $storePassword = $matches[1].Trim()
} else {
    Write-Host "KEYSTORE_PASSWORD not found in .env" -ForegroundColor Red; exit 1
}
if ([string]::IsNullOrEmpty($storePassword)) { Write-Host "KEYSTORE_PASSWORD is empty in .env" -ForegroundColor Red; exit 1 }

# ---------- Keystore check ----------
$keystore = Join-Path $projectDir "app\fazenda-keystore.jks"
if (-not (Test-Path $keystore)) { Write-Host "Keystore not found" -ForegroundColor Red; exit 1 }

# ---------- Build (gradle handles signing automatically) ----------
$env:FAZENDA_STORE_PASSWORD = $storePassword
Set-Location $projectDir
$oldPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
cmd.exe /c "gradlew.bat assembleRelease -x lintVitalAnalyzeRelease -Dorg.gradle.java.home=""C:\Program Files\Android\Android Studio\jbr"""
$ErrorActionPreference = $oldPreference
Remove-Item Env:\FAZENDA_STORE_PASSWORD -ErrorAction SilentlyContinue
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed" -ForegroundColor Red; exit 1 }

# ---------- Copy signed APK ----------
$buildDir = Join-Path $projectDir "app\build\outputs\apk\release"
$signedApk = Join-Path $buildDir "app-release.apk"
$output = Join-Path $buildDir "Fazenda-v$newName.apk"
if (Test-Path $signedApk) {
    Copy-Item $signedApk $output -Force
    # Copy to dist directory
    $distDir = Join-Path $projectDir "dist"
    if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }
    $distOutput = Join-Path $distDir "Fazenda-v$newName-Release.apk"
    Copy-Item $signedApk $distOutput -Force
    Write-Host "Copied release APK to dist: $distOutput" -ForegroundColor Green
} else {
    Write-Host "Signed APK not found" -ForegroundColor Red; exit 1
}

# ---------- Verify ----------
Write-Host "Verifying..." -ForegroundColor Cyan
$apksigner = "C:\Users\lexus\AppData\Local\Android\Sdk\build-tools\35.0.0\apksigner.bat"
& $apksigner verify --print-certs "$output" 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host "Verification FAILED" -ForegroundColor Red; exit 1 }

Write-Host "`nOK: $output $(('{0:N0}' -f ((Get-Item $output).Length / 1KB))) KB" -ForegroundColor Green

# ---------- Publish to GitHub ----------
if ($Publish) {
    $gh = Get-Command "gh.exe" -ErrorAction SilentlyContinue
    if (-not $gh) { $gh = Get-Command "C:\Program Files\GitHub CLI\gh.exe" -ErrorAction SilentlyContinue }
    if (-not $gh) {
        Write-Host "GitHub CLI (gh) not found. Install from https://cli.github.com/" -ForegroundColor Red; exit 1
    }
    $tag = "v$newName"
    $releaseTitle = "Fazenda v$newName"
    $changeLog = "Changes for v$newName`n`n- Auto-generated release"

    & $gh.Path auth status 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Host "Not logged in to GitHub. Run 'gh auth login'" -ForegroundColor Red; exit 1 }

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    Write-Host "`nCreating GitHub Release..." -ForegroundColor Cyan
    git tag -d $tag 2>$null
    git tag -a $tag -m $releaseTitle
    git push origin $tag --force

    & $gh.Path release create $tag "$output" --title $releaseTitle --notes $changeLog
    
    $ErrorActionPreference = $oldPreference
    if ($LASTEXITCODE -ne 0) { Write-Host "Release failed" -ForegroundColor Red; exit 1 }
    Write-Host "Published: https://github.com/aviantcorellc-lang/Fazenda/releases/tag/$tag" -ForegroundColor Green
}