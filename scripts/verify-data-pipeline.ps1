param(
    [string]$ApiBase = 'http://localhost:8080/api',
    [string]$MysqlContainer = 'nextstep-mysql',
    [string]$RedisContainer = 'nextstep-redis',
    [string]$BackendContainer = 'nextstep-backend'
)

$ErrorActionPreference = 'Stop'
$failed = [System.Collections.Generic.List[string]]::new()

function Check([string]$name, [scriptblock]$action) {
    try {
        & $action
        Write-Output "[OK] $name"
    } catch {
        $failed.Add("$($name): $($_.Exception.Message)")
        Write-Output "[FAIL] $($name): $($_.Exception.Message)"
    }
}

Check 'Docker CLI' { docker info | Out-Null }
Check "MySQL container $MysqlContainer" {
    $status = docker inspect --format '{{.State.Status}}' $MysqlContainer
    if ($status -ne 'running') { throw "status=$status" }
}
Check "Redis container $RedisContainer" {
    $status = docker inspect --format '{{.State.Status}}' $RedisContainer
    if ($status -ne 'running') { throw "status=$status" }
}
Check "Backend container $BackendContainer" {
    $status = docker inspect --format '{{.State.Status}}' $BackendContainer
    if ($status -ne 'running') { throw "status=$status" }
}
Check 'Backend HTTP endpoint' {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$ApiBase/auth/me" -TimeoutSec 8
        if ($response.StatusCode -notin 200, 401, 403) { throw "HTTP $($response.StatusCode)" }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -notin 401, 403) { throw }
    }
}
Check 'Swagger endpoint' {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$ApiBase/v3/api-docs" -TimeoutSec 8
        if ($response.StatusCode -ne 200) { throw "HTTP $($response.StatusCode)" }
    } catch {
        throw
    }
}

if ($failed.Count -gt 0) {
    Write-Output "Pipeline check failed: $($failed.Count) item(s)"
    $failed | ForEach-Object { Write-Output "- $_" }
    exit 1
}

Write-Output "Pipeline smoke check passed. Next call /admin/data-import/sources and /admin/data-import/batches with an admin token."
