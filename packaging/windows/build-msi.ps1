# Windows kurulum paketi (.msi) üretir, isteğe bağlı Authenticode ile imzalar ve sessiz kurulumla dener.
# Builds the Windows .msi, optionally Authenticode-signs it, then smoke-tests a silent install.
#
# Önce / first:  mvn -B package   (esign-cli/target/esign.jar)
# Kullanım / usage (PowerShell):  packaging/windows/build-msi.ps1 [-SkipInstallTest]
#
# İmzalama yalnızca şu ortam değişkenleri doluysa yapılır (GitHub Secrets'tan gelir, depoya asla yazılmaz):
# Signing runs only when these variables are set (from GitHub Secrets; never committed):
#   WINDOWS_PFX_BASE64 + WINDOWS_PFX_PASSWORD   -> signtool ile PFX sertifikası / PFX via signtool
#   (Azure Artifact/Trusted Signing iş akışında ayrı bir adımdır / Azure signing is a separate workflow step.)
param([switch]$SkipInstallTest)
$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot/../.."
Set-Location $root

# 1) WiX 3 (jpackage/JDK 21 bunu kullanır). Yoksa Chocolatey ile kurulur.
if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    $wix = Get-ChildItem 'C:\Program Files (x86)\WiX Toolset v3*\bin' -Directory -ErrorAction SilentlyContinue |
        Sort-Object FullName | Select-Object -Last 1
    if (-not $wix) {
        choco install wixtoolset --no-progress -y | Out-Host
        $wix = Get-ChildItem 'C:\Program Files (x86)\WiX Toolset v3*\bin' -Directory | Sort-Object FullName | Select-Object -Last 1
    }
    $env:PATH = "$($wix.FullName);$env:PATH"
}
Write-Host "WiX: $((Get-Command candle.exe).Source)"

# 2) jpackage (ortak betik, Git Bash ile)
$bash = (Get-Command bash.exe -ErrorAction SilentlyContinue).Source
if (-not $bash) { $bash = 'C:\Program Files\Git\bin\bash.exe' }
& $bash packaging/jpackage.sh msi
if ($LASTEXITCODE -ne 0) { throw "jpackage başarısız / failed ($LASTEXITCODE)" }

$version = (Select-String -Path pom.xml -Pattern '<version>(.+)</version>' | Select-Object -First 1).Matches[0].Groups[1].Value
$msi = Join-Path $root "dist/esign-$version-windows-x64.msi"
Get-ChildItem dist/*.msi | Where-Object { $_.FullName -ne $msi } | Select-Object -First 1 | Move-Item -Destination $msi -Force
Write-Host "MSI: $msi"

# 3) İsteğe bağlı Authenticode imzası (PFX)
if ($env:WINDOWS_PFX_BASE64 -and $env:WINDOWS_PFX_PASSWORD) {
    $signtool = Get-ChildItem 'C:\Program Files (x86)\Windows Kits\10\bin\*\x64\signtool.exe' |
        Sort-Object FullName | Select-Object -Last 1
    $pfx = Join-Path ([IO.Path]::GetTempPath()) "esign-$([guid]::NewGuid()).pfx"
    try {
        [IO.File]::WriteAllBytes($pfx, [Convert]::FromBase64String($env:WINDOWS_PFX_BASE64))
        $ts = if ($env:WINDOWS_TIMESTAMP_URL) { $env:WINDOWS_TIMESTAMP_URL } else { 'http://timestamp.digicert.com' }
        & $signtool.FullName sign /fd SHA256 /f $pfx /p $env:WINDOWS_PFX_PASSWORD /tr $ts /td SHA256 /d 'esign' $msi
        if ($LASTEXITCODE -ne 0) { throw "signtool başarısız / failed" }
        & $signtool.FullName verify /pa $msi
    } finally {
        Remove-Item $pfx -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Host 'Authenticode sertifikası tanımlı değil; MSI imzasız kalır / no PFX secret, MSI left unsigned.'
}

if ($SkipInstallTest) { return }

# 4) Sessiz kurulum ve doğrulama / silent install smoke test
$log = Join-Path ([IO.Path]::GetTempPath()) 'esign-install.log'
$p = Start-Process msiexec.exe -ArgumentList "/i `"$msi`" /qn /norestart /l*v `"$log`"" -Wait -PassThru
if ($p.ExitCode -ne 0) { Get-Content $log -Tail 60; throw "msiexec çıkış kodu $($p.ExitCode)" }

$dir = Join-Path $env:ProgramFiles 'esign'
& "$dir\esign.exe" version
if ($LASTEXITCODE -ne 0) { throw 'esign.exe version başarısız' }
if (-not (Test-Path "$dir\esign-gui.exe")) { throw 'esign-gui.exe yok' }
if (-not (Test-Path "$dir\runtime\bin\java.exe") -and -not (Test-Path "$dir\runtime\bin\java.dll")) {
    Write-Host 'Uyarı: gömülü JRE yolu beklenenden farklı'
}

$machinePath = [Environment]::GetEnvironmentVariable('PATH', 'Machine')
if ($machinePath -notlike "*$dir*") { throw "Kurulum klasörü PATH'te değil: $machinePath" }
Write-Host 'PATH tamam / ok'

$lnk = Get-ChildItem "$env:ProgramData\Microsoft\Windows\Start Menu\Programs" -Recurse -Filter 'esign-gui*.lnk' -ErrorAction SilentlyContinue
if (-not $lnk) { throw 'Başlat menüsü kısayolu esign-gui bulunamadı' }
Write-Host "Kısayol / shortcut: $($lnk.FullName -join ', ')"
$cli = Get-ChildItem "$env:ProgramData\Microsoft\Windows\Start Menu\Programs" -Recurse -Filter 'esign.lnk' -ErrorAction SilentlyContinue
if ($cli) { throw "Komut satırı başlatıcısı için gereksiz kısayol: $($cli.FullName)" }
if (-not (Test-Path "$env:PUBLIC\Desktop\esign-gui.lnk")) { throw 'Masaüstü kısayolu yok / no desktop shortcut' }

# Kaldırma da denenir; PATH temizlenmeli / uninstall must remove PATH entry
$p = Start-Process msiexec.exe -ArgumentList "/x `"$msi`" /qn /norestart" -Wait -PassThru
if ($p.ExitCode -ne 0) { throw "kaldırma çıkış kodu $($p.ExitCode)" }
if ([Environment]::GetEnvironmentVariable('PATH', 'Machine') -like "*$dir*") { throw 'Kaldırınca PATH temizlenmedi' }
if (Test-Path "$env:PUBLIC\Desktop\esign-gui.lnk") { throw 'Kaldırınca masaüstü kısayolu kaldı' }
Write-Host 'Kurulum/kaldırma testi geçti / install+uninstall test passed'
