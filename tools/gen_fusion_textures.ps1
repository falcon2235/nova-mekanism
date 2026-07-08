# Textures for the fusion reactor: fusion casing + glowing fusion coil, controller
# front, port skins (7), and the stellar core item.
Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'
$itemDir = Join-Path $root 'item'
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null
function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$rng = New-Object System.Random 20260708

# fusion casing = deep blue-violet reactor hull
$styles = @{
    'fusion' = @{ base = (C 44 52 92); light = (C 74 84 140); dark = (C 26 30 56); bolt = (C 120 180 240) }
}

function Paint-GtBase($bmp, $p) {
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = $p.base
            if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $c = $p.dark }
            elseif ($x -eq 1 -or $y -eq 1) { $c = $p.light }
            elseif ($x -eq 14 -or $y -eq 14) { $c = $p.dark }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    foreach ($pt in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
        $bx = $pt[0]; $by = $pt[1]
        $bmp.SetPixel($bx, $by, $p.bolt)
        $bmp.SetPixel($bx+1, $by, $p.dark)
        $bmp.SetPixel($bx, $by+1, $p.dark)
        $bmp.SetPixel($bx+1, $by+1, $p.bolt)
    }
}

# --- fusion casing ---
$p = $styles['fusion']
$bmp = New-Object System.Drawing.Bitmap 16, 16
Paint-GtBase $bmp $p
for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, 7, $p.dark); $bmp.SetPixel($x, 8, $p.light) }
Save $bmp (Join-Path $dir 'fusion_casing.png'); Write-Host 'casing fusion_casing'

# --- fusion coil: glowing cyan energy rings on a dark frame ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
$frame = C 26 30 56
$glow = C 90 220 255
$glowD = C 40 140 200
$glowH = C 200 250 255
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        if ($x -le 1 -or $x -ge 14) { $bmp.SetPixel($x, $y, $frame); continue }
        $m = $y % 4
        $c = switch ($m) { 0 { $glowH } 1 { $glow } 2 { $glowD } 3 { $glow } }
        if ((($x + ($y * 3)) % 12) -eq 0) { $c = $glowH }
        $bmp.SetPixel($x, $y, $c)
    }
}
Save $bmp (Join-Path $dir 'fusion_coil.png'); Write-Host 'coil fusion_coil'

# --- controller front ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
Paint-GtBase $bmp $p
$ring = C 90 220 255
for ($x = 3; $x -le 12; $x++) {
    for ($y = 3; $y -le 12; $y++) {
        if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) { $bmp.SetPixel($x, $y, $ring) }
        else { $bmp.SetPixel($x, $y, (C 20 24 44)) }
    }
}
foreach ($pt in @(@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], (C 200 250 255)) }
Save $bmp (Join-Path $dir 'fusion_reactor_controller_front.png'); Write-Host 'front fusion_reactor_controller_front'

# --- port skins over the fusion base ---
function Paint-Panel($bmp, $ring, [int]$x0, [int]$y0, [int]$x1, [int]$y1) {
    for ($x = $x0; $x -le $x1; $x++) {
        for ($y = $y0; $y -le $y1; $y++) {
            if ($x -eq $x0 -or $x -eq $x1 -or $y -eq $y0 -or $y -eq $y1) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 20 24 44)) }
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
    Paint-GtBase $bmp $p
    Paint-Symbol $bmp $port
    Save $bmp (Join-Path $dir "${port}_fusion.png"); Write-Host "port ${port}_fusion"
}

# --- stellar core item: radiant star gem ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
$core = C 150 220 255; $coreH = C 235 250 255; $coreD = C 90 150 210
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        $dx = $x - 7.5; $dy = $y - 7.5
        $dist = [Math]::Sqrt($dx*$dx + $dy*$dy)
        if ($dist -le 5.4) {
            $c = $core
            if ($dist -le 2.0) { $c = $coreH } elseif ($dist -ge 4.2) { $c = $coreD }
            $bmp.SetPixel($x, $y, $c)
        }
    }
}
# star rays
foreach ($pt in @(@(7,1),@(8,1),@(7,14),@(8,14),@(1,7),@(1,8),@(14,7),@(14,8),@(3,3),@(12,12),@(12,3),@(3,12))) {
    $bmp.SetPixel($pt[0], $pt[1], $coreH)
}
Save $bmp (Join-Path $itemDir 'stellar_core.png'); Write-Host 'item stellar_core'
Write-Host 'done'
