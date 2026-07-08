# Regenerates all GT-style textures: base casing, controller fronts (6),
# parallel units (6), controller side, and all port skins (7 ports x 4 styles).
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

# style palettes: base, light, dark, bolt
$styles = @{
    'default'    = @{ base = (C 74 85 104);   light = (C 113 128 150); dark = (C 45 55 72);    bolt = (C 160 174 192) }
    'heat_proof' = @{ base = (C 168 178 150); light = (C 198 206 180); dark = (C 116 126 100); bolt = (C 220 226 204) }
    'ptfe'       = @{ base = (C 222 226 230); light = (C 244 246 249); dark = (C 164 172 182); bolt = (C 250 251 253) }
    'stainless'  = @{ base = (C 200 204 210); light = (C 232 236 241); dark = (C 142 148 158); bolt = (C 245 247 250) }
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

function Save($bmp, $name) {
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host $name
}

# --- plain casing (default style, with seam like GT machine hull) ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
Paint-GtBase $bmp $styles['default']
for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, 7, $styles['default'].dark); $bmp.SetPixel($x, 8, $styles['default'].light) }
Save $bmp 'multiblock_casing'

# --- controller side (default base + vents) ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
Paint-GtBase $bmp $styles['default']
for ($y = 5; $y -le 10; $y += 2) { for ($x = 4; $x -le 11; $x++) { $bmp.SetPixel($x, $y, (C 30 36 48)) } }
Save $bmp 'controller_side'

# --- parallel processing units: default base + tier core ---
$tierColors = @{
    '10'  = (C 76 175 80); '25' = (C 33 150 243); '50' = (C 156 39 176)
    '100' = (C 255 152 0); '200' = (C 244 67 54); '300' = (C 0 229 255)
}
foreach ($tier in @('10','25','50','100','200','300')) {
    $col = $tierColors[$tier]
    $darkc = C ([int]($col.R * 0.5)) ([int]($col.G * 0.5)) ([int]($col.B * 0.5))
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-GtBase $bmp $styles['default']
    for ($x = 4; $x -le 11; $x++) {
        for ($y = 4; $y -le 11; $y++) {
            if ($x -eq 4 -or $x -eq 11 -or $y -eq 4 -or $y -eq 11) { $bmp.SetPixel($x, $y, $darkc) }
            else { $bmp.SetPixel($x, $y, $col) }
        }
    }
    Save $bmp "parallel_processor_$tier"
}

# --- controller fronts: GT base + dark screen + colored ring + dot ---
function New-Front($name, $styleKey, $ring) {
    $p = $styles[$styleKey]
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-GtBase $bmp $p
    for ($x = 3; $x -le 12; $x++) {
        for ($y = 3; $y -le 12; $y++) {
            if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
        }
    }
    foreach ($pt in @(@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $ring) }
    Save $bmp $name
}
New-Front 'enriching_controller_front'      'default'    (C 229 57 53)
New-Front 'crushing_controller_front'       'default'    (C 126 87 194)
New-Front 'smelting_controller_front'       'default'    (C 255 179 0)
New-Front 'blast_furnace_controller_front'  'heat_proof' (C 255 120 40)
New-Front 'reactor_controller_front'        'ptfe'       (C 90 200 120)
New-Front 'distillation_controller_front'   'stainless'  (C 60 160 210)

# --- port skins: symbol painters over each style base ---
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
    foreach ($styleKey in $styles.Keys) {
        $bmp = New-Object System.Drawing.Bitmap 16, 16
        Paint-GtBase $bmp $styles[$styleKey]
        Paint-Symbol $bmp $port
        $suffix = if ($styleKey -eq 'default') { '' } else { "_$styleKey" }
        Save $bmp "$port$suffix"
    }
}
Write-Host 'done'
