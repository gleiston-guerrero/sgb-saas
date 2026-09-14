Param()

$md = 'docs/mediciones/DATA-PROVENANCE.md'
$out = 'docs/mediciones/hashes-verification-report.md'

if (-not (Test-Path $md)) {
    "Data provenance file not found: $md" | Out-File -FilePath $out -Encoding UTF8
    exit 1
}

"# Hashes verification report" | Out-File -FilePath $out -Encoding UTF8
"Source: $md" | Out-File -FilePath $out -Encoding UTF8 -Append
"" | Out-File -FilePath $out -Encoding UTF8 -Append

$content = Get-Content $md -Raw
$matches = [regex]::Matches($content, '\b[0-9a-f]{7,40}\b') | ForEach-Object { $_.Value } | Sort-Object -Unique

foreach ($h in $matches) {
    # Try to resolve object using git
    $proc = Start-Process -FilePath git -ArgumentList "rev-parse --verify --quiet $h^0" -NoNewWindow -PassThru -Wait -ErrorAction SilentlyContinue
    if ($proc.ExitCode -eq 0) {
        "- $($h): OK" | Out-File -FilePath $out -Encoding UTF8 -Append
    } else {
        "- $($h): MISSING" | Out-File -FilePath $out -Encoding UTF8 -Append
    }
}

"Report written to $out" | Write-Output
