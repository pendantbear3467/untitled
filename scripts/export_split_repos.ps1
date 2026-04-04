param(
    [string]$OutputRoot = 'workspace/repo-split',
    [switch]$KeepExisting
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $repoRoot $OutputRoot

function Assert-SafeOutputRoot {
    param([string]$PathToCheck)

    $fullOutput = [System.IO.Path]::GetFullPath($PathToCheck)
    $fullRepo = [System.IO.Path]::GetFullPath($repoRoot)

    if (-not $fullOutput.StartsWith($fullRepo, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Output root must be inside repository. Got: $fullOutput"
    }

    if ($fullOutput -eq $fullRepo) {
        throw 'Output root cannot be repository root.'
    }
}

function Write-RootBuildGradle {
    param(
        [string]$TargetPath,
        [string]$ArchiveName
    )

    $content = @"
plugins {
    id 'java-library'
    id 'maven-publish'
}

group = 'com.extremecraft'
version = '1.2.0'

base {
    archivesName = '$ArchiveName'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
}
"@
    Set-Content -Path (Join-Path $TargetPath 'build.gradle') -Value $content -Encoding UTF8
}

function Export-ModuleRepo {
    param(
        [string]$RepoName,
        [string]$ModuleDir,
        [string]$ArchiveName
    )

    $sourcePath = Join-Path $repoRoot $ModuleDir
    if (-not (Test-Path $sourcePath)) {
        throw "Module source path missing: $sourcePath"
    }

    $targetPath = Join-Path $outputPath $RepoName
    if ((Test-Path $targetPath) -and -not $KeepExisting) {
        Remove-Item -Path $targetPath -Recurse -Force
    }

    New-Item -ItemType Directory -Path $targetPath -Force | Out-Null

    Copy-Item -Path (Join-Path $sourcePath 'src') -Destination $targetPath -Recurse -Force
    if (Test-Path (Join-Path $sourcePath 'README.md')) {
        Copy-Item -Path (Join-Path $sourcePath 'README.md') -Destination (Join-Path $targetPath 'README.md') -Force
    }

    Set-Content -Path (Join-Path $targetPath 'settings.gradle') -Value "rootProject.name = '$RepoName'" -Encoding UTF8
    Write-RootBuildGradle -TargetPath $targetPath -ArchiveName $ArchiveName

    if (Test-Path (Join-Path $repoRoot 'gradle.properties')) {
        Copy-Item -Path (Join-Path $repoRoot 'gradle.properties') -Destination (Join-Path $targetPath 'gradle.properties') -Force
    }

    foreach ($launcher in @('gradlew', 'gradlew.bat')) {
        if (Test-Path (Join-Path $repoRoot $launcher)) {
            Copy-Item -Path (Join-Path $repoRoot $launcher) -Destination (Join-Path $targetPath $launcher) -Force
        }
    }

    $wrapperTarget = Join-Path $targetPath 'gradle\wrapper'
    New-Item -ItemType Directory -Path $wrapperTarget -Force | Out-Null
    Copy-Item -Path (Join-Path $repoRoot 'gradle\wrapper\*') -Destination $wrapperTarget -Recurse -Force

    return $targetPath
}

Assert-SafeOutputRoot -PathToCheck $outputPath
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$apiTarget = Export-ModuleRepo -RepoName 'extremecraft-api' -ModuleDir 'api' -ArchiveName 'extremecraft-api'
$coreTarget = Export-ModuleRepo -RepoName 'extremecraft-core' -ModuleDir 'core' -ArchiveName 'extremecraft-core'

$report = @"
# Repo Split Export Report

Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ssK')
Output Root: $outputPath

Created repositories:
- extremecraft-api -> $apiTarget
- extremecraft-core -> $coreTarget

Notes:
- Export is non-destructive: host repo source remains unchanged.
- Re-run this script to refresh split artifacts.
- By default, existing export folders are replaced. Use -KeepExisting to keep them.
"@

Set-Content -Path (Join-Path $outputPath 'EXPORT_REPORT.md') -Value $report -Encoding UTF8
Write-Host "Split exports generated under: $outputPath"
