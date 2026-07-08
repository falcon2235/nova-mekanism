# Generates front textures for the 3 chemical-machine controllers (casing base + colored screen).
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

function New-Front($name, $ring, $dotR, $dotG, $dotB) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-Casing $bmp
    for ($x = 3; $x -le 12; $x++) {
        for ($y = 3; $y -le 12; $y++) {
            if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
        }
    }
    $d = C $dotR $dotG $dotB
    foreach ($p in @(@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($p[0], $p[1], $d) }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "front $name"
}

New-Front 'blast_furnace_controller_front' (C 255 120 40)  255 180 60    # orange (heat)
New-Front 'reactor_controller_front'       (C 90 200 120)  120 240 150   # green (chemical)
New-Front 'distillation_controller_front'  (C 90 190 220)  140 220 245   # cyan (distillation)
Write-Host 'done'
