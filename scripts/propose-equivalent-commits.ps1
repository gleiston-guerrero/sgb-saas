Param()

$dp = 'docs/mediciones/DATA-PROVENANCE.md'
$out = 'docs/mediciones/proposed-commit-equivalents.md'

if (-not (Test-Path $dp)) { Write-Error "$dp not found"; exit 1 }

"# Propuesta de commits equivalentes" | Out-File $out -Encoding UTF8
"Generado automáticamente. Para cada fila con hash ausente se listan
los archivos mencionados en la misma fila y el último commit actual que
modificó ese archivo (si existe)." | Out-File $out -Append -Encoding UTF8
"" | Out-File $out -Append -Encoding UTF8

$content = Get-Content $dp -Raw

$rows = $content -split "\n" | Where-Object { $_ -match '^\|\s*\d+' }

foreach ($r in $rows) {
    # attempt to extract the commit token at end of row
    if ($r -match '\|\s*([0-9a-f]{7,40})\s*\|\s*$') {
        $commit = $matches[1]
    } else { $commit = '' }
    if ($commit -and (git rev-parse --verify --quiet "$commit"^0) -eq $null) {
        # extract filenames from the row (look for paths like docs/... or *.svg/*.pdf)
        $files = ([regex]::Matches($r, 'docs/[\w\-\./]+|[\w\-]+\.svg|[\w\-]+\.pdf')) | ForEach-Object { $_.Value } | Select-Object -Unique
        "## Fila: $r" | Out-File $out -Append -Encoding UTF8
        "- Hash citado: $commit (no presente)" | Out-File $out -Append -Encoding UTF8
        if ($files.Count -eq 0) {
            "- No se detectaron nombres de archivo en la misma fila." | Out-File $out -Append -Encoding UTF8
        } else {
            foreach ($f in $files) {
                if (Test-Path $f) {
                    $last = git log -n 1 --pretty=format:"%H %ci" -- $f 2>$null
                    if ($last) { "- Archivo: $f -> último commit: $last" | Out-File $out -Append -Encoding UTF8 } else { "- Archivo: $f -> sin historial local" | Out-File $out -Append -Encoding UTF8 }
                } else { "- Archivo: $f -> no existe en este árbol" | Out-File $out -Append -Encoding UTF8 }
            }
        }
        "" | Out-File $out -Append -Encoding UTF8
    }
}

Write-Output "Propuesta escrita en $out"
