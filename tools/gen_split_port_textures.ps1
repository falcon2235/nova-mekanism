# Generates the 4 split gas/fluid port textures (casing + symbol + in/out arrow).
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

function New-SplitPort($name, $ring, $symbolPixels, $symbolColor, $arrowUp) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-Casing $bmp
    for ($x = 3; $x -le 12; $x++) {
        for ($y = 3; $y -le 10; $y++) {
            if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 10) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
        }
    }
    foreach ($p in $symbolPixels) { $bmp.SetPixel($p[0], $p[1], $symbolColor) }
    # arrow strip at the bottom: input = blue down arrow, output = orange up arrow
    if ($arrowUp) {
        $ac = C 255 152 0
        foreach ($p in @(@(7,11),@(8,11), @(6,12),@(7,12),@(8,12),@(9,12), @(7,13),@(8,13))) { $bmp.SetPixel($p[0], $p[1], $ac) }
        $bmp.SetPixel(7, 11, $ac); $bmp.SetPixel(8, 11, $ac)
        foreach ($p in @(@(5,13),@(10,13))) { $bmp.SetPixel($p[0], $p[1], (C 128 76 0)) }
    } else {
        $ac = C 66 165 245
        foreach ($p in @(@(7,13),@(8,13), @(6,12),@(7,12),@(8,12),@(9,12), @(7,11),@(8,11))) { $bmp.SetPixel($p[0], $p[1], $ac) }
        foreach ($p in @(@(5,11),@(10,11))) { $bmp.SetPixel($p[0], $p[1], (C 33 82 122)) }
    }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "port $name"
}

# gas symbol: cloud puffs (purple)
$gasRing = C 106 76 148
$gasSym = @(@(6,6),@(7,6),@(8,6),@(9,6), @(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7), @(6,8),@(8,8))
$gasCol = C 186 140 240

# fluid symbol: droplet (cyan)
$fluidRing = C 40 120 160
$fluidSym = @(@(7,5), @(6,6),@(7,6),@(8,6), @(6,7),@(7,7),@(8,7), @(6,8),@(7,8),@(8,8))
$fluidCol = C 100 200 245

New-SplitPort 'gas_input_port'    $gasRing   $gasSym   $gasCol   $false
New-SplitPort 'gas_output_port'   $gasRing   $gasSym   $gasCol   $true
New-SplitPort 'fluid_input_port'  $fluidRing $fluidSym $fluidCol $false
New-SplitPort 'fluid_output_port' $fluidRing $fluidSym $fluidCol $true
Write-Host 'done'
