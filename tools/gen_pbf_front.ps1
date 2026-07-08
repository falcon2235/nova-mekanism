# Primitive blast furnace controller front: brick pattern + glowing furnace mouth.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

$brick = C 150 97 83
$brickD = C 124 77 62
$mortar = C 167 143 133

$bmp = New-Object System.Drawing.Bitmap 16, 16
# brick pattern: 4px tall rows, offset every other row
for ($y = 0; $y -lt 16; $y++) {
    $row = [int][Math]::Floor($y / 4)
    for ($x = 0; $x -lt 16; $x++) {
        $c = if (($y % 4) -eq 3) { $mortar } else { if ((($x + $row * 4) % 8) -eq 7) { $mortar } else { if ((($x + $y) % 5) -eq 0) { $brickD } else { $brick } } }
        $bmp.SetPixel($x, $y, $c)
    }
}
# furnace mouth: dark arch with fire glow
for ($x = 4; $x -le 11; $x++) {
    for ($y = 6; $y -le 13; $y++) {
        $edge = ($x -eq 4 -or $x -eq 11 -or $y -eq 6)
        if ($edge) { $bmp.SetPixel($x, $y, (C 60 40 34)) }
        else { $bmp.SetPixel($x, $y, (C 25 18 15)) }
    }
}
foreach ($p in @(@(6,12),@(7,11),@(8,12),@(9,11),@(7,13),@(8,13),@(6,13),@(9,13))) {
    $bmp.SetPixel($p[0], $p[1], (C 255 140 30))
}
foreach ($p in @(@(7,12),@(8,11))) { $bmp.SetPixel($p[0], $p[1], (C 255 210 90)) }

$bmp.Save((Join-Path $dir 'primitive_blast_furnace_controller_front.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host 'pbf front done'
