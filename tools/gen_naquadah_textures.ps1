# Textures for the naquadah line: antimony/naquadah ores, PGM-style intermediates,
# metal dusts/ingots (naquadah is dark green, naquadria dark purple, trinium pale gold).
Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'
$itemDir = Join-Path $root 'item'
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null
function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$rng = New-Object System.Random 20260707

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
New-Ore 'antimony_ore'           $stone $stoneD (C 150 158 172) (C 196 204 216)
New-Ore 'deepslate_antimony_ore' $deep  $deepD  (C 150 158 172) (C 196 204 216)
# naquadah: dark green glow specks
New-Ore 'naquadah_ore'           $stone $stoneD (C 42 84 56)  (C 96 176 110)
New-Ore 'deepslate_naquadah_ore' $deep  $deepD  (C 42 84 56)  (C 96 176 110)

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

New-Chunk 'raw_antimony'          (C 150 158 172) (C 196 204 216) (C 104 112 126)
New-Dust  'antimony_dust'         (C 168 174 186) (C 206 212 222) 90
New-Dust  'antimony_trioxide'     (C 236 238 242) (C 254 254 255) 90
New-Dust  'antimony_trifluoride'  (C 214 224 220) (C 244 250 248) 90
New-Chunk 'raw_naquadah'          (C 40 78 52)    (C 88 160 104)  (C 24 50 32)
New-Dust  'naquadah_dust'         (C 46 88 58)    (C 96 176 110)  100
New-Dust  'enriched_naquadah_sulfate' (C 58 104 74) (C 110 190 128) 90
New-Dust  'naquadria_sulfate'     (C 92 60 118)   (C 150 104 186)  90
New-Dust  'trinium_sulfide'       (C 206 196 150) (C 238 230 190)  90
New-Dust  'osmiridium_dust'       (C 120 140 176) (C 168 188 220)  90
New-Ingot 'naquadah_enriched_ingot' (C 44 92 60)  (C 92 168 108) (C 26 56 36)
New-Ingot 'naquadria_ingot'       (C 88 56 116)   (C 150 108 188) (C 52 32 72)
New-Ingot 'trinium_ingot'         (C 214 206 162) (C 244 238 206) (C 158 150 108)
New-Ingot 'naquadah_alloy_ingot'  (C 60 62 60)    (C 110 88 128)  (C 34 20 44)
Write-Host 'done'
