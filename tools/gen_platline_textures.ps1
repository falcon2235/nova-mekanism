# Textures for the platinum-group line: cooperite/saltpeter ores, PGM intermediates,
# metal dusts/ingots, electrolyzer + centrifuge casings, controller fronts, port skins.
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
# electrolyzer = pale cyan-blue cell casing w/ copper accents; centrifuge = amber-bronze
$styles = @{
    'electrolyzer' = @{ base = (C 148 178 186); light = (C 186 214 220); dark = (C 96 124 132);  bolt = (C 206 132 74) }
    'centrifuge'   = @{ base = (C 190 158 104); light = (C 222 194 142); dark = (C 132 104 62);  bolt = (C 244 218 170) }
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

function New-Casing($name, $styleKey) {
    $p = $styles[$styleKey]
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    Paint-GtBase $bmp $p
    for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, 7, $p.dark); $bmp.SetPixel($x, 8, $p.light) }
    Save $bmp (Join-Path $dir "$name.png"); Write-Host "casing $name"
}
New-Casing 'electrolyzer_casing' 'electrolyzer'
New-Casing 'centrifuge_casing'   'centrifuge'

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
New-Front 'electrolyzer_controller_front' 'electrolyzer' (C 70 200 220)
New-Front 'centrifuge_controller_front'   'centrifuge'   (C 255 170 60)

# --- port skins over each style base ---
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

# --- ores ---
function New-Ore($name, $baseCol, $baseDark, $speck, $speckHi) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = if ($rng.Next(4) -eq 0) { $baseDark } else { $baseCol }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    foreach ($b in @(@(4,4),@(11,5),@(6,10),@(10,11),@(3,12))) {
        foreach ($d in @(@(0,0),@(1,0),@(0,1),@(1,1),@(-1,0),@(0,-1))) {
            $x = $b[0] + $d[0]; $y = $b[1] + $d[1]
            if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
                $c = if (($d[0] -eq 0 -and $d[1] -eq 0)) { $speckHi } else { $speck }
                $bmp.SetPixel($x, $y, $c)
            }
        }
    }
    Save $bmp (Join-Path $dir "$name.png"); Write-Host "block $name"
}
$stone = C 127 127 127; $stoneD = C 105 105 105
$deep = C 74 74 78;   $deepD = C 58 58 62
# cooperite: bright platinum-white specks; saltpeter: chalky white-grey specks
New-Ore 'cooperite_ore'           $stone $stoneD (C 226 230 226) (C 250 252 250)
New-Ore 'deepslate_cooperite_ore' $deep  $deepD  (C 226 230 226) (C 250 252 250)
New-Ore 'saltpeter_ore'           $stone $stoneD (C 232 228 214) (C 250 248 240)
New-Ore 'deepslate_saltpeter_ore' $deep  $deepD  (C 232 228 214) (C 250 248 240)

# --- items ---
function New-Chunk($name, $base, $hi, $lo) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($x = 3; $x -le 12; $x++) {
        for ($y = 3; $y -le 13; $y++) {
            $dx = $x - 7.5; $dy = $y - 8
            if (($dx*$dx)/22 + ($dy*$dy)/26 -le 1) {
                $c = $base
                if ($dx -lt -1 -and $dy -lt 0) { $c = $hi }
                if ($dx -gt 2 -or $dy -gt 3) { $c = $lo }
                if ($rng.Next(6) -eq 0) { $c = $hi }
                $bmp.SetPixel($x, $y, $c)
            }
        }
    }
    Save $bmp (Join-Path $itemDir "$name.png"); Write-Host "item $name"
}
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

New-Dust  'saltpeter'             (C 240 236 222) (C 254 252 246) 90
New-Chunk 'raw_cooperite'         (C 214 220 214) (C 244 248 244) (C 158 164 158)
New-Dust  'platinum_group_sludge' (C 96 78 60)    (C 130 108 84)  100
New-Dust  'platinum_raw'          (C 226 220 200) (C 248 244 230) 90
New-Dust  'palladium_raw'         (C 150 146 138) (C 190 186 178) 90
New-Dust  'inert_metal_mixture'   (C 148 142 96)  (C 186 180 130) 90
New-Dust  'rarest_metal_mixture'  (C 96 88 132)   (C 134 126 176) 90
New-Dust  'ruthenium_tetroxide'   (C 196 158 66)  (C 230 196 104) 90
New-Dust  'osmium_tetroxide'      (C 198 214 224) (C 232 244 250) 90
New-Dust  'iridium_metal_residue' (C 104 104 110) (C 142 142 150) 100
New-Dust  'iridium_chloride'      (C 128 140 92)  (C 166 180 126) 90
New-Dust  'ammonium_chloride'     (C 240 240 240) (C 254 254 254) 90
New-Dust  'platinum_dust'         (C 224 224 214) (C 248 248 240) 90
New-Ingot 'platinum_ingot'        (C 228 228 220) (C 250 250 244) (C 168 168 158)
New-Dust  'palladium_dust'        (C 176 172 166) (C 212 208 202) 90
New-Ingot 'palladium_ingot'       (C 184 180 174) (C 220 216 210) (C 128 124 118)
New-Dust  'rhodium_dust'          (C 210 216 222) (C 240 244 248) 90
New-Ingot 'rhodium_ingot'         (C 214 220 226) (C 244 248 252) (C 152 158 166)
New-Dust  'ruthenium_dust'        (C 160 170 182) (C 198 208 218) 90
New-Ingot 'ruthenium_ingot'       (C 168 178 190) (C 206 214 224) (C 112 122 134)
New-Dust  'iridium_dust'          (C 232 230 218) (C 252 250 242) 90
New-Ingot 'iridium_ingot'         (C 236 234 224) (C 254 252 246) (C 178 176 164)
Write-Host 'done'
