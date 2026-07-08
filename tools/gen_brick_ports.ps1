# Brick-skinned port textures for the primitive blast furnace (7 ports).
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

$brick = C 150 97 83
$brickD = C 124 77 62
$mortar = C 167 143 133

function Paint-BrickBase($bmp) {
    for ($y = 0; $y -lt 16; $y++) {
        $row = [int][Math]::Floor($y / 4)
        for ($x = 0; $x -lt 16; $x++) {
            $c = if (($y % 4) -eq 3) { $mortar } else { if ((($x + $row * 4) % 8) -eq 7) { $mortar } else { if ((($x + $y) % 5) -eq 0) { $brickD } else { $brick } } }
            $bmp.SetPixel($x, $y, $c)
        }
    }
}

function Paint-Panel($bmp, $ring, [int]$x0, [int]$y0, [int]$x1, [int]$y1) {
    for ($x = $x0; $x -le $x1; $x++) {
        for ($y = $y0; $y -le $y1; $y++) {
            if ($x -eq $x0 -or $x -eq $x1 -or $y -eq $y0 -or $y -eq $y1) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
        }
    }
}

function Paint-Symbol($bmp, [string]$port) {
    switch ($port) {
        'energy_port' {
            Paint-Panel $bmp (C 128 106 0) 4 4 11 11
            $yellow = C 255 213 0
            foreach ($pt in @(@(8,5),@(7,6),@(7,7),@(8,7),@(9,7),@(8,8),@(7,9),@(7,10))) { $bmp.SetPixel($pt[0], $pt[1], $yellow) }
        }
        'item_input_port' {
            Paint-Panel $bmp (C 33 82 122) 4 4 11 11
            $blue = C 66 165 245
            foreach ($pt in @(@(7,5),@(8,5),@(7,6),@(8,6),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),@(6,8),@(7,8),@(8,8),@(9,8),@(7,9),@(8,9))) { $bmp.SetPixel($pt[0], $pt[1], $blue) }
        }
        'item_output_port' {
            Paint-Panel $bmp (C 128 76 0) 4 4 11 11
            $orange = C 255 152 0
            foreach ($pt in @(@(7,9),@(8,9),@(7,8),@(8,8),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),@(6,6),@(7,6),@(8,6),@(9,6),@(7,5),@(8,5))) { $bmp.SetPixel($pt[0], $pt[1], $orange) }
        }
        'gas_input_port' {
            Paint-Panel $bmp (C 106 76 148) 3 3 12 10
            $g = C 186 140 240
            foreach ($pt in @(@(6,5),@(7,5),@(8,5),@(9,5),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(6,7),@(8,7))) { $bmp.SetPixel($pt[0], $pt[1], $g) }
            $ac = C 66 165 245
            foreach ($pt in @(@(7,11),@(8,11),@(6,12),@(7,12),@(8,12),@(9,12),@(7,13),@(8,13))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'gas_output_port' {
            Paint-Panel $bmp (C 106 76 148) 3 3 12 10
            $g = C 186 140 240
            foreach ($pt in @(@(6,5),@(7,5),@(8,5),@(9,5),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(6,7),@(8,7))) { $bmp.SetPixel($pt[0], $pt[1], $g) }
            $ac = C 255 152 0
            foreach ($pt in @(@(7,13),@(8,13),@(6,12),@(7,12),@(8,12),@(9,12),@(7,11),@(8,11))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'fluid_input_port' {
            Paint-Panel $bmp (C 40 120 160) 3 3 12 10
            $f = C 100 200 245
            foreach ($pt in @(@(7,4),@(6,5),@(7,5),@(8,5),@(6,6),@(7,6),@(8,6),@(6,7),@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $f) }
            $ac = C 66 165 245
            foreach ($pt in @(@(7,11),@(8,11),@(6,12),@(7,12),@(8,12),@(9,12),@(7,13),@(8,13))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'fluid_output_port' {
            Paint-Panel $bmp (C 40 120 160) 3 3 12 10
            $f = C 100 200 245
            foreach ($pt in @(@(7,4),@(6,5),@(7,5),@(8,5),@(6,6),@(7,6),@(8,6),@(6,7),@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $f) }
            $ac = C 255 152 0
            foreach ($pt in @(@(7,13),@(8,13),@(6,12),@(7,12),@(8,12),@(9,12),@(7,11),@(8,11))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
    }
}

$ports = @('energy_port','item_input_port','item_output_port','gas_input_port','gas_output_port','fluid_input_port','fluid_output_port')
foreach ($port in $ports) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-BrickBase $bmp
    Paint-Symbol $bmp $port
    $bmp.Save((Join-Path $dir "${port}_brick.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "brick $port"
}
Write-Host 'done'
