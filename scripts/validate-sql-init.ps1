param(
    [string]$SqlDirectory = (Join-Path $PSScriptRoot '..\sql')
)

$ErrorActionPreference = 'Stop'
$sqlPath = (Resolve-Path -LiteralPath $SqlDirectory).Path
$files = Get-ChildItem -LiteralPath $sqlPath -File -Filter '*.sql' | Sort-Object Name
$errors = [System.Collections.Generic.List[string]]::new()

$numbered = foreach ($file in $files) {
    if ($file.Name -match '^(\d{2})_') {
        [pscustomobject]@{ Prefix = $Matches[1]; File = $file.Name }
    }
}

$numbered |
    Where-Object { $_.Prefix -ne '99' } |
    Group-Object Prefix |
    Where-Object Count -gt 1 |
    ForEach-Object {
        $names = ($_.Group.File -join ', ')
        $errors.Add("Duplicate migration prefix $($_.Name): $names")
    }

$tableColumns = @{}
foreach ($file in $files) {
    $content = Get-Content -Raw -LiteralPath $file.FullName

    if ($file.Name -match '^(\d{2})_' -and [int]$Matches[1] -ge 9 -and $content -match '(?i)DROP\s+TABLE') {
        $errors.Add("Late migration must not drop business tables: $($file.Name)")
    }

    foreach ($create in [regex]::Matches($content, '(?is)CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+`(?<table>[^`]+)`\s*\((?<body>.*?)\)\s*ENGINE')) {
        $table = $create.Groups['table'].Value
        if (-not $tableColumns.ContainsKey($table)) {
            $tableColumns[$table] = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        }
        foreach ($column in [regex]::Matches($create.Groups['body'].Value, '(?m)^\s*`(?<column>[^`]+)`')) {
            [void]$tableColumns[$table].Add($column.Groups['column'].Value)
        }
    }

    foreach ($alter in [regex]::Matches($content, '(?is)ALTER\s+TABLE\s+`(?<table>[^`]+)`(?<operations>.*?);')) {
        $table = $alter.Groups['table'].Value
        if (-not $tableColumns.ContainsKey($table)) {
            $tableColumns[$table] = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        }
        foreach ($add in [regex]::Matches($alter.Groups['operations'].Value, '(?i)ADD\s+COLUMN\s+`(?<column>[^`]+)`')) {
            $column = $add.Groups['column'].Value
            if (-not $tableColumns[$table].Add($column)) {
                $errors.Add("Column added more than once: $table.$column ($($file.Name))")
            }
        }
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Output "ERROR: $_" }
    exit 1
}

Write-Output "SQL initialization validation passed. Files checked: $($files.Count)."
