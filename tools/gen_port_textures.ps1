# Generates gas/fluid port textures (casing base + symbol).
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

$base = C 74 85 104; $hi = C 113 128 150; $lo = C 45 55 72; $rivet = C 160 174 192

function Paint-Casing($bmp) {
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = $base
            if ($x -eq 0 -or $y -eq 0) { $c = $hi }
            if ($x -eq 15 -or $y -eq 15) { $c = $lo }
            if ((($x + $y) % 8) -eq 0 -and $x -gt 1 -and $x -lt 14 -and $y -gt 1 -and $y -lt 14) { $c = C 66 76 94 }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    foreach ($p in @(@(2,2), @(13,2), @(2,13), @(13,13))) { $bmp.SetPixel($p[0], $p[1], $rivet) }
}

function New-Port($name, $ring, $screen, $symbolPixels, $symbolColor) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-Casing $bmp
    for ($x = 4; $x -le 11; $x++) {
        for ($y = 4; $y -le 11; $y++) {
            if ($x -eq 4 -or $x -eq 11 -or $y -eq 4 -or $y -eq 11) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, $screen) }
        }
    }
    foreach ($p in $symbolPixels) { $bmp.SetPixel($p[0], $p[1], $symbolColor) }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "port $name"
}

# gas port: purple ring, cloud puffs
New-Port 'gas_port' (C 106 76 148) (C 26 32 44) @(
    @(6,7),@(7,7),@(8,7),@(9,7),
    @(5,8),@(6,8),@(7,8),@(8,8),@(9,8),@(10,8),
    @(6,6),@(8,6),
    @(7,9),@(9,9)
) (C 186 140 240)

# fluid port: cyan ring, droplet
New-Port 'fluid_port' (C 40 120 160) (C 26 32 44) @(
    @(7,5),@(8,5),
    @(6,6),@(7,6),@(8,6),@(9,6),
    @(6,7),@(7,7),@(8,7),@(9,7),
    @(6,8),@(7,8),@(8,8),@(9,8),
    @(7,9),@(8,9),
    @(7,10),@(8,10)
) (C 100 200 245)

Write-Host 'done'
