# Generates 16x16 textures for the titanium/magnesium materials and ores.
Add-Type -AssemblyName System.Drawing

$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $dir 'block'))
$itemDir = [System.IO.Path]::GetFullPath((Join-Path $dir 'item'))
[System.IO.Directory]::CreateDirectory($blockDir) | Out-Null
[System.IO.Directory]::CreateDirectory($itemDir) | Out-Null

function C([int]$r, [int]$g, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }

function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }

$rng = New-Object System.Random 20260703

# --- item: chunk (rounded blob) ---
function New-Chunk($name, $base, $hi, $lo) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    $pts = @(@(5,3),@(10,3),@(3,6),@(12,6),@(4,11),@(11,12),@(7,13))
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
    Save $bmp (Join-Path $itemDir "$name.png")
    Write-Host "item $name"
}

# --- item: ingot ---
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
    # bevel top corners
    $bmp.SetPixel(3, 6, (C 0 0 0 0)); $bmp.SetPixel(12, 6, $lo)
    Save $bmp (Join-Path $itemDir "$name.png")
    Write-Host "item $name"
}

# --- item: dust/powder (scattered) ---
function New-Dust($name, $base, $hi) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($i = 0; $i -lt 90; $i++) {
        $x = 3 + $rng.Next(10); $y = 4 + $rng.Next(9)
        $c = if ($rng.Next(3) -eq 0) { $hi } else { $base }
        $bmp.SetPixel($x, $y, $c)
    }
    Save $bmp (Join-Path $itemDir "$name.png")
    Write-Host "item $name"
}

# --- item: sponge (porous block-ish) ---
function New-Sponge($name, $base, $hole) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, (C 0 0 0 0)) } }
    for ($x = 2; $x -le 13; $x++) {
        for ($y = 2; $y -le 13; $y++) {
            $c = if ((($x*3 + $y*5) % 7) -lt 2) { $hole } else { $base }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    Save $bmp (Join-Path $itemDir "$name.png")
    Write-Host "item $name"
}

# --- block: ore (base stone/deepslate + specks) ---
function New-Ore($name, $baseCol, $baseDark, $speck, $speckHi) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = if ($rng.Next(4) -eq 0) { $baseDark } else { $baseCol }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $blobs = @(@(4,4),@(11,5),@(6,10),@(10,11),@(3,12))
    foreach ($b in $blobs) {
        foreach ($d in @(@(0,0),@(1,0),@(0,1),@(1,1),@(-1,0),@(0,-1))) {
            $x = $b[0] + $d[0]; $y = $b[1] + $d[1]
            if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
                $c = if (($d[0] -eq 0 -and $d[1] -eq 0)) { $speckHi } else { $speck }
                $bmp.SetPixel($x, $y, $c)
            }
        }
    }
    Save $bmp (Join-Path $blockDir "$name.png")
    Write-Host "block $name"
}

New-Chunk 'raw_titanium'   (C 110 123 139) (C 150 163 179) (C 74 85 104)
New-Chunk 'raw_magnesium'  (C 154 160 168) (C 190 196 204) (C 108 114 122)
New-Ingot 'titanium_ingot' (C 192 200 208) (C 224 232 240) (C 130 140 152)
New-Dust  'titanium_oxide' (C 232 234 238) (C 255 255 255)
New-Dust  'magnesium_dust' (C 200 204 210) (C 232 236 242)
New-Sponge 'titanium_sponge' (C 138 143 152) (C 70 74 82)

$stone = C 127 127 127; $stoneD = C 105 105 105
$deep = C 74 74 78;   $deepD = C 58 58 62
New-Ore 'titanium_ore'           $stone $stoneD (C 120 136 168) (C 160 176 208)
New-Ore 'deepslate_titanium_ore' $deep  $deepD  (C 120 136 168) (C 160 176 208)
New-Ore 'magnesium_ore'           $stone $stoneD (C 196 202 212) (C 228 232 240)
New-Ore 'deepslate_magnesium_ore' $deep  $deepD  (C 196 202 212) (C 228 232 240)

Write-Host 'done'
