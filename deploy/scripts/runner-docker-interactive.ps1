#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Switch GitHub Actions runners from Windows Service to interactive mode for Docker Desktop access.

.DESCRIPTION
  Docker Desktop on Windows exposes the API via npipe to the logged-in user only.
  Runners installed with --runasservice run as NETWORK SERVICE and cannot use Docker.

  This script stops/uninstalls the runner Windows services and prints commands to start
  runners interactively under your user account.

.PARAMETER RunnerRoot
  Base directory containing backend/ and frontend/ runner installs.
#>
param(
    [string]$RunnerRoot = 'C:\actions-runner'
)

$ErrorActionPreference = 'Stop'

$runners = @(
    @{
        Name    = 'backend'
        Service = 'actions.runner.ehdrlf25-eng-RAG_DOC_BACKEND.ragdoc-backend'
        Dir     = Join-Path $RunnerRoot 'backend'
    },
    @{
        Name    = 'frontend'
        Service = 'actions.runner.ehdrlf25-eng-RAG_DOC_FRONTEND.ragdoc-frontend'
        Dir     = Join-Path $RunnerRoot 'frontend'
    }
)

foreach ($runner in $runners) {
    $serviceName = $runner.Service
    $dir = $runner.Dir
    $serviceExe = Join-Path $dir 'bin\RunnerService.exe'

    if (Get-Service -Name $serviceName -ErrorAction SilentlyContinue) {
        Write-Host "Stopping service: $serviceName"
        Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
        Set-Service -Name $serviceName -StartupType Disabled
        Write-Host "Removing service registration: $serviceName"
        sc.exe delete $serviceName | Out-Null
        Start-Sleep -Seconds 2
    }

    if (Test-Path $serviceExe) {
        Write-Host "Runner service helper ($($runner.Name)): optional cleanup via sc.exe delete above."
    } else {
        Write-Warning "RunnerService.exe not found at $serviceExe"
    }
}

Write-Host ''
Write-Host '=== Done ===' -ForegroundColor Green
Write-Host 'Start Docker Desktop, then run each runner in a separate terminal (same Windows user):'
Write-Host ''
foreach ($runner in $runners) {
    Write-Host "  cd $($runner.Dir)"
    Write-Host "  .\run.cmd"
    Write-Host ''
}
Write-Host 'Optional: set repository variable DEPLOY_PATH = C:\Users\ehdrl\rag-doc-platform\backend\deploy'
Write-Host 'and keep a real .env there (not auto-generated from .env.example).'
