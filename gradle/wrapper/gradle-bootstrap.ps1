$ErrorActionPreference = "Stop"

$propertiesPath = Join-Path $PSScriptRoot "gradle-wrapper.properties"
$properties = @{}
Get-Content $propertiesPath | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $properties[$matches[1]] = $matches[2]
    }
}

$distributionUrl = $properties['distributionUrl'].Replace('\:', ':')
$expectedHash = $properties['distributionSha256Sum'].ToLowerInvariant()
$archiveName = Split-Path $distributionUrl -Leaf
$distributionName = $archiveName -replace '-bin\.zip$', ''
$gradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $HOME '.gradle' }
$cacheDir = Join-Path $gradleUserHome "wrapper\dists\lifes\$distributionName"
$gradleHome = Join-Path $cacheDir $distributionName
$archive = Join-Path $cacheDir $archiveName
$gradleExecutable = Join-Path $gradleHome 'bin\gradle.bat'

if (-not (Test-Path $gradleExecutable)) {
    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
    if (-not (Test-Path $archive)) {
        Invoke-WebRequest -UseBasicParsing -Uri $distributionUrl -OutFile $archive
    }
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($archive)
        try {
            $bytes = $sha.ComputeHash($stream)
        } finally {
            $stream.Dispose()
        }
    } finally {
        $sha.Dispose()
    }
    $actualHash = ([System.BitConverter]::ToString($bytes) -replace '-', '').ToLowerInvariant()
    if ($actualHash -ne $expectedHash) {
        Remove-Item -Force $archive
        throw "Gradle distribution checksum mismatch."
    }
    $temporary = Join-Path $cacheDir ('.extract-' + [Guid]::NewGuid().ToString('N'))
    Expand-Archive -Path $archive -DestinationPath $temporary -Force
    if (Test-Path $gradleHome) { Remove-Item -Recurse -Force $gradleHome }
    Move-Item (Join-Path $temporary $distributionName) $gradleHome
    Remove-Item -Recurse -Force $temporary
}

& $gradleExecutable @args
exit $LASTEXITCODE
