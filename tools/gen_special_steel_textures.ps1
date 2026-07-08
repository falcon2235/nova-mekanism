# Textures for the chromium / aluminium / special steel chain.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$blockDir = Join-Path $dir 'block'
$itemDir = Join-Path $dir 'item'
[System.IO.Directory]::CreateDirectory($blockDir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null
function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$rng = New-Object System.Random 20260705

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
function New-Crystal($name, $base, $hi, $lo) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    # diamond-shaped crystal cluster
    foreach ($p in @(@(7,3),@(8,3),@(6,4),@(7,4),@(8,4),@(9,4),@(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),
                     @(4,6),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(11,6),
                     @(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),
                     @(4,8),@(5,8),@(6,8),@(7,8),@(8,8),@(9,8),
                     @(5,9),@(6,9),@(7,9),@(8,9),@(9,9),@(10,9),
                     @(6,10),@(7,10),@(8,10),@(9,10),@(7,11),@(8,11),@(6,12),@(9,12))) {
        $bmp.SetPixel($p[0], $p[1], $base)
    }
    foreach ($p in @(@(7,3),@(6,4),@(5,5),@(4,6),@(7,4),@(6,5))) { $bmp.SetPixel($p[0], $p[1], $hi) }
    foreach ($p in @(@(11,6),@(10,7),@(9,9),@(10,9),@(8,11),@(9,12))) { $bmp.SetPixel($p[0], $p[1], $lo) }
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

$stone = C 127 127 127; $stoneD = C 105 105 105
$deep = C 74 74 78;   $deepD = C 58 58 62

# chromium: bright chrome silver-blue; bauxite: red-brown
New-Ore 'chromium_ore'           $stone $stoneD (C 186 198 214) (C 224 234 248)
New-Ore 'deepslate_chromium_ore' $deep  $deepD  (C 186 198 214) (C 224 234 248)
New-Ore 'bauxite_ore'            $stone $stoneD (C 178 110 78)  (C 208 140 104)
New-Ore 'deepslate_bauxite_ore'  $deep  $deepD  (C 178 110 78)  (C 208 140 104)

New-Chunk 'raw_chromium'          (C 172 184 200) (C 210 222 238) (C 122 134 150)
New-Chunk 'enriched_chromium_ore' (C 150 168 192) (C 205 220 240) (C 100 116 140)
New-Dust  'sodium_carbonate'      (C 236 236 230) (C 252 252 248) 90
New-Dust  'sodium_dichromate'     (C 228 130 40)  (C 250 170 80)  90
New-Crystal 'sodium_dichromate_crystal' (C 232 120 32) (C 255 180 96) (C 168 78 16)
New-Chunk 'raw_bauxite'           (C 172 104 72)  (C 204 136 100) (C 128 72 48)
New-Dust  'alumina'               (C 242 242 244) (C 255 255 255) 90
New-Ingot 'aluminum_ingot'        (C 214 220 228) (C 240 244 250) (C 156 164 176)
New-Dust  'aluminum_dust'         (C 208 214 222) (C 238 242 248) 90
New-Ingot 'chromium_ingot'        (C 190 202 218) (C 230 240 252) (C 132 146 164)
New-Dust  'chromium_dust'         (C 186 198 214) (C 224 234 248) 90
New-Dust  'special_steel_dust'    (C 116 128 148) (C 156 170 192) 90
New-Ingot 'special_steel_ingot'   (C 130 144 166) (C 176 192 214) (C 84 96 116)
Write-Host 'done'
