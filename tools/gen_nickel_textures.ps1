# Nickel/cupronickel item + ore textures and the mixer controller front.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$blockDir = Join-Path $dir 'block'
$itemDir = Join-Path $dir 'item'
[System.IO.Directory]::CreateDirectory($blockDir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null
function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$rng = New-Object System.Random 20260704

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
    Save $bmp (Join-Path $blockDir "$name.png"); Write-Host "block $name"
}

# nickel: pale greenish-silver; cupronickel: warm silver-rose
New-Chunk 'raw_nickel'        (C 176 182 160) (C 208 214 192) (C 128 134 112)
New-Dust  'fine_nickel_powder' (C 196 202 178) (C 226 232 210) 55
New-Dust  'nickel_dust'        (C 176 182 160) (C 208 214 192) 90
New-Ingot 'nickel_ingot'       (C 198 204 182) (C 228 234 214) (C 140 146 124)
New-Dust  'cupronickel_dust'   (C 206 160 130) (C 232 192 164) 90
New-Ingot 'cupronickel_ingot'  (C 212 166 136) (C 238 198 170) (C 150 108 82)

$stone = C 127 127 127; $stoneD = C 105 105 105
$deep = C 74 74 78;   $deepD = C 58 58 62
New-Ore 'nickel_ore'           $stone $stoneD (C 184 190 166) (C 216 222 200)
New-Ore 'deepslate_nickel_ore' $deep  $deepD  (C 184 190 166) (C 216 222 200)

# mixer controller front: stainless GT base + teal screen
$p = @{ base = (C 200 204 210); light = (C 232 236 241); dark = (C 142 148 158); bolt = (C 245 247 250) }
$bmp = New-Object System.Drawing.Bitmap 16, 16
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
    $bmp.SetPixel($pt[0], $pt[1], $p.bolt); $bmp.SetPixel($pt[0]+1, $pt[1], $p.dark)
    $bmp.SetPixel($pt[0], $pt[1]+1, $p.dark); $bmp.SetPixel($pt[0]+1, $pt[1]+1, $p.bolt)
}
$ring = C 64 190 170
for ($x = 3; $x -le 12; $x++) {
    for ($y = 3; $y -le 12; $y++) {
        if ($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12) { $bmp.SetPixel($x, $y, $ring) }
        else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
    }
}
# swirl (mixing blades)
foreach ($pt in @(@(7,6),@(8,6),@(6,7),@(9,8),@(7,9),@(8,9),@(7,7),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $ring) }
Save $bmp (Join-Path $blockDir 'mixer_controller_front.png'); Write-Host 'block mixer_controller_front'
Write-Host 'done'
