#Requires -RunAsAdministrator
# pgvector v0.8.2 for PostgreSQL 18 (Windows x64)
# Source: https://github.com/andreiramani/pgvector_pgsql_windows/releases/tag/0.8.2_18.0.2

$ErrorActionPreference = "Stop"

$pgRoot = "C:\Program Files\PostgreSQL\18"
$serviceName = "postgresql-x64-18"
$zipUrl = "https://github.com/andreiramani/pgvector_pgsql_windows/releases/download/0.8.2_18.0.2/vector.v0.8.2-pg18.zip"
$zipPath = Join-Path $env:TEMP "vector.v0.8.2-pg18.zip"
$extractPath = Join-Path $env:TEMP "pgvector-pg18"

Write-Host "Downloading pgvector for PostgreSQL 18..."
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath
Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force

Write-Host "Stopping PostgreSQL service..."
Stop-Service $serviceName -Force
Start-Sleep -Seconds 2

Write-Host "Installing pgvector files to $pgRoot ..."
Copy-Item "$extractPath\lib\vector.dll" "$pgRoot\lib\" -Force
Copy-Item "$extractPath\share\extension\*" "$pgRoot\share\extension\" -Force
New-Item -ItemType Directory -Path "$pgRoot\include\server\extension\vector" -Force | Out-Null
Copy-Item "$extractPath\include\server\extension\vector\*" "$pgRoot\include\server\extension\vector\" -Force

Write-Host "Starting PostgreSQL service..."
Start-Service $serviceName
Start-Sleep -Seconds 3

$pgBin = Join-Path $pgRoot "bin\psql.exe"
if (-not $env:PGPASSWORD) {
    $password = Read-Host "Enter PostgreSQL postgres user password" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
}

Write-Host "Creating pgvector extension in ragdoc database..."
& $pgBin -h localhost -p 5432 -U postgres -d ragdoc -c "CREATE EXTENSION IF NOT EXISTS vector;"
& $pgBin -h localhost -p 5432 -U postgres -d ragdoc -c "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';"

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
Write-Host "Done. Restart the backend without DB_PORT=5433."
