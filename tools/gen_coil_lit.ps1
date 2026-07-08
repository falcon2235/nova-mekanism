# Regenerates coil textures: new champagne-silver cupronickel (distinct from copper)
# and glowing "_lit" variants for all 5 coils.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, [Math]::Min(255,$r), [Math]::Min(255,$g), [Math]::Min(255,$b)) }

function New-Coil($name, $frame, $light, $mid, $dark) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            if ($x -le 1 -or $x -ge 14) { $bmp.SetPixel($x, $y, $frame); continue }
            $m = $y % 4
            $c = switch ($m) {
                0 { $light }
                1 { $mid }
                2 { $dark }
                3 { $mid }
            }
            if ((($x + ($y * 2)) % 16) -eq 0) { $c = $dark }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "coil $name"
}

# Lit variant: windings glow hot — bright rows plus an incandescent seam row.
function New-CoilLit($name, $frame, $glowHi, $glowMid, $glowLo) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            if ($x -le 1 -or $x -ge 14) { $bmp.SetPixel($x, $y, $frame); continue }
            $m = $y % 4
            $c = switch ($m) {
                0 { $glowHi }
                1 { $glowMid }
                2 { $glowLo }
                3 { $glowMid }
            }
            if ((($x + ($y * 2)) % 16) -eq 0) { $c = $glowHi }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "coil $name"
}

# copper: red-orange (unchanged look)
New-Coil    'copper_coil'         (C 70 50 40)  (C 240 150 90)  (C 210 115 60)  (C 150 75 35)
New-CoilLit 'copper_coil_lit'     (C 90 55 40)  (C 255 235 170) (C 255 180 90)  (C 235 120 50)
# cupronickel: NEW champagne-silver ("white copper") — clearly distinct from copper
New-Coil    'cupronickel_coil'     (C 60 58 50)  (C 238 232 218) (C 212 204 186) (C 156 148 128)
New-CoilLit 'cupronickel_coil_lit' (C 80 74 60)  (C 255 250 225) (C 250 226 170) (C 232 190 120)
# titanium: cool blue-silver
New-Coil    'titanium_coil'        (C 55 60 70)  (C 235 240 246) (C 198 205 215) (C 140 148 160)
New-CoilLit 'titanium_coil_lit'    (C 70 78 92)  (C 255 255 255) (C 220 235 255) (C 160 200 250)
# plutonium: green glow
New-Coil    'plutonium_coil'       (C 30 50 30)  (C 160 255 160) (C 96 200 96)   (C 50 130 50)
New-CoilLit 'plutonium_coil_lit'   (C 40 70 40)  (C 235 255 210) (C 170 255 150) (C 110 235 110)
# antimatter: violet on near-black
New-Coil    'antimatter_coil'      (C 20 12 28)  (C 220 140 255) (C 150 60 200)  (C 80 20 120)
New-CoilLit 'antimatter_coil_lit'  (C 40 24 56)  (C 255 220 255) (C 230 150 255) (C 180 80 235)
Write-Host 'done'
