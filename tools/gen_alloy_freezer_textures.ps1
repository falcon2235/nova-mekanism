# Textures for the alloy blast furnace + vacuum freezer machines:
# two special-steel casings, two controller fronts, port skins (7 x 2 styles),
# and the super-alloy ingot/dust items.
Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'
$itemDir = Join-Path $root 'item'
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null
function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$rng = New-Object System.Random 20260706

# style palettes: base, light, dark, bolt
# alloy = GT "high temperature smelting casing" look: dark maroon-brown plates
# assembly = GTNH assembly-line casing: dark teal-grey with lighter frame
$styles = @{
    'alloy'    = @{ base = (C 92 58 50);    light = (C 126 84 72);   dark = (C 54 32 28);    bolt = (C 190 116 78) }
    'frost'    = @{ base = (C 176 200 214); light = (C 214 232 242); dark = (C 120 150 168); bolt = (C 232 246 252) }
    'assembly' = @{ base = (C 70 96 104);   light = (C 104 138 148); dark = (C 40 58 64);    bolt = (C 150 200 210) }
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

# --- casings: GT base + a horizontal seam (like the other machine hulls) ---
function New-Casing($name, $styleKey) {
    $p = $styles[$styleKey]
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-GtBase $bmp $p
    for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, 7, $p.dark); $bmp.SetPixel($x, 8, $p.light) }
    Save $bmp (Join-Path $dir "$name.png"); Write-Host "casing $name"
}
New-Casing 'alloy_blast_casing' 'alloy'
New-Casing 'frost_proof_casing' 'frost'

# --- assembly casing: GT "grate machine casing" look — steel frame, dark interior,
#     bold X-cross grate bars (teal-tinted steel to match the port skins) ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
$aP = $styles['assembly']
$grateBg = C 26 34 38
$grateBar = C 136 168 178
$grateBarD = C 92 118 126
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        $c = $grateBg
        if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $c = $aP.dark }
        elseif ($x -eq 1 -or $y -eq 1 -or $x -eq 14 -or $y -eq 14) { $c = $aP.base }
        $bmp.SetPixel($x, $y, $c)
    }
}
# X-cross bars (2px wide diagonals)
for ($x = 2; $x -le 13; $x++) {
    for ($y = 2; $y -le 13; $y++) {
        $d1 = [Math]::Abs($x - $y)
        $d2 = [Math]::Abs($x + $y - 15)
        if ($d1 -le 1 -or $d2 -le 1) {
            $c = if ($d1 -eq 0 -or $d2 -eq 0) { $grateBar } else { $grateBarD }
            $bmp.SetPixel($x, $y, $c)
        }
    }
}
# centre plate bolt
foreach ($pt in @(@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $aP.bolt) }
Save $bmp (Join-Path $dir 'assembly_casing.png'); Write-Host 'casing assembly_casing (grate)'

# --- reinforced glass side walls: TRANSPARENT pane with a steel frame + shine streak ---
$bmp = New-Object System.Drawing.Bitmap 16, 16
$gFrame = C 110 156 166
$gFrameD = C 74 106 114
$gShine = C 214 240 246 170
$gTint = C 170 215 224 40
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) {
            $c = if (($x + $y) % 2 -eq 0) { $gFrame } else { $gFrameD }
            $bmp.SetPixel($x, $y, $c)
        } elseif (($x -eq $y -and $x -ge 2 -and $x -le 6) -or (($x - 1) -eq $y -and $x -ge 3 -and $x -le 7)) {
            $bmp.SetPixel($x, $y, $gShine)   # diagonal shine streak
        } else {
            $bmp.SetPixel($x, $y, $gTint)    # nearly transparent pane
        }
    }
}
Save $bmp (Join-Path $dir 'assembly_glass.png'); Write-Host 'glass assembly_glass (transparent)'

# --- heat vent: alloy frame + dark horizontal slats with an ember glow (GT HEAT_VENT) ---
$p = $styles['alloy']
$bmp = New-Object System.Drawing.Bitmap 16, 16
Paint-GtBase $bmp $p
$slit = C 24 18 16
$ember = C 232 120 40
for ($sy = 4; $sy -le 11; $sy += 2) {
    for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, $sy, $slit) }
}
foreach ($pt in @(@(5,4),@(9,6),@(6,8),@(11,10))) { $bmp.SetPixel($pt[0], $pt[1], $ember) }
Save $bmp (Join-Path $dir 'heat_vent.png'); Write-Host 'casing heat_vent'

# --- controller fronts: GT base + dark screen + coloured ring + centre dot ---
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
    Save $bmp (Join-Path $dir "$name.png"); Write-Host "front $name"
}
New-Front 'alloy_blast_furnace_controller_front' 'alloy'    (C 255 140 40)
New-Front 'vacuum_freezer_controller_front'      'frost'    (C 90 200 235)
New-Front 'circuit_assembler_controller_front'   'assembly' (C 120 230 160)

# --- port skins: symbol painters over each style base (mirrors gen_gt_all.ps1) ---
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
        Save $bmp (Join-Path $dir "${port}_${styleKey}.png"); Write-Host "port ${port}_${styleKey}"
    }
}

# --- super alloy items: iridescent purple-teal ingot + dust ---
function New-Ingot($name, $base, $hi, $lo) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($x = 3; $x -le 12; $x++) {
        for ($y = 6; $y -le 11; $y++) {
            $c = $base
            if ($y -eq 6 -or $x -eq 3) { $c = $hi }
            if ($y -eq 11 -or $x -eq 12) { $c = $lo }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.SetPixel(3, 6, (C 0 0 0 0)); $bmp.SetPixel(12, 6, $lo)
    Save $bmp (Join-Path $itemDir "$name.png"); Write-Host "item $name"
}
function New-Dust($name, $base, $hi, [int]$density) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($i = 0; $i -lt $density; $i++) {
        $x = 3 + $rng.Next(10); $y = 4 + $rng.Next(9)
        $c = if ($rng.Next(3) -eq 0) { $hi } else { $base }
        $bmp.SetPixel($x, $y, $c)
    }
    Save $bmp (Join-Path $itemDir "$name.png"); Write-Host "item $name"
}
New-Ingot 'super_alloy_ingot' (C 152 122 186) (C 202 180 228) (C 100 74 132)
New-Dust  'super_alloy_dust'  (C 158 130 190) (C 206 186 230) 90
# titanium dust: silvery grey-white to match the titanium ingot
New-Dust  'titanium_dust'     (C 196 200 206) (C 232 236 242) 90

# supreme control circuit: a PCB board with traces + a central chip
function New-Circuit($name, $board, $boardHi, $trace, $chip, $chipHi) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($x = 2; $x -le 13; $x++) {
        for ($y = 2; $y -le 13; $y++) {
            $c = $board
            if ($x -eq 2 -or $y -eq 2) { $c = $boardHi }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    # traces
    foreach ($x in 4,7,10,12) { for ($y = 3; $y -le 12; $y++) { $bmp.SetPixel($x, $y, $trace) } }
    foreach ($y in 4,11) { for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, $y, $trace) } }
    # central chip
    for ($x = 6; $x -le 9; $x++) { for ($y = 6; $y -le 9; $y++) { $bmp.SetPixel($x, $y, $chip) } }
    $bmp.SetPixel(6, 6, $chipHi); $bmp.SetPixel(7, 6, $chipHi)
    # solder pins
    foreach ($p in @(@(5,7),@(5,8),@(10,7),@(10,8),@(7,5),@(8,5),@(7,10),@(8,10))) { $bmp.SetPixel($p[0], $p[1], $chipHi) }
    Save $bmp (Join-Path $itemDir "$name.png"); Write-Host "item $name"
}
New-Circuit 'supreme_control_circuit' (C 24 96 72) (C 40 132 100) (C 150 224 190) (C 40 44 52) (C 210 180 90)
Write-Host 'done'
