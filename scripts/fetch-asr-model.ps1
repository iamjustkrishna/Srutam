# Downloads the on-device speech model into app/src/main/assets.
#
# Model: NVIDIA Parakeet-TDT-CTC-110M (English), int8 ONNX export published by the sherpa-onnx project.
# It is not committed to git because the model file (131 MB) is over GitHub's 100 MB file limit.
# Safe to re-run: files that already match the expected SHA-256 are left alone.

$ErrorActionPreference = "Stop"

$archiveUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8.tar.bz2"
$archiveDir = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8"
$assets = Join-Path (Split-Path -Parent $PSScriptRoot) "app/src/main/assets"

# asset name -> file inside the archive, expected SHA-256
$files = @(
    @{ Name = "parakeet-110m.int8.onnx"; Source = "model.int8.onnx"; Sha256 = "9177a9146cf32ee0cc8152276ef95116f312018d316be37ccf57f7efea81fc1a" },
    @{ Name = "parakeet-110m-tokens.txt"; Source = "tokens.txt"; Sha256 = "450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10" }
)

function Test-Asset($file) {
    $path = Join-Path $assets $file.Name
    (Test-Path $path) -and ((Get-FileHash $path -Algorithm SHA256).Hash -ieq $file.Sha256)
}

if (-not ($files | Where-Object { -not (Test-Asset $_) })) {
    Write-Host "Speech model is already up to date."
    return
}

$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("srutam-asr-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $temp | Out-Null
try {
    $archive = Join-Path $temp "model.tar.bz2"
    Write-Host "Downloading $archiveUrl (about 100 MB)..."
    Invoke-WebRequest -Uri $archiveUrl -OutFile $archive -UseBasicParsing
    tar -xjf $archive -C $temp
    if ($LASTEXITCODE -ne 0) { throw "Could not extract the archive (is tar available?)." }

    New-Item -ItemType Directory -Path $assets -Force | Out-Null
    foreach ($file in $files) {
        $source = Join-Path (Join-Path $temp $archiveDir) $file.Source
        $actual = (Get-FileHash $source -Algorithm SHA256).Hash
        if ($actual -ine $file.Sha256) {
            throw "Checksum mismatch for $($file.Source): expected $($file.Sha256) but got $actual."
        }
        Copy-Item $source (Join-Path $assets $file.Name) -Force
        Write-Host "Installed $($file.Name)"
    }
} finally {
    Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
}
