# Wound-coil textures for the 4 new EBF coil tiers (cupronickel already exists).
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

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

# copper: bright red-orange copper
New-Coil 'copper_coil'     (C 70 50 40)  (C 240 150 90)  (C 210 115 60) (C 150 75 35)
# titanium: silvery
New-Coil 'titanium_coil'   (C 55 60 70)  (C 235 240 246) (C 198 205 215) (C 140 148 160)
# plutonium: glowing green
New-Coil 'plutonium_coil'  (C 30 50 30)  (C 160 255 160) (C 96 200 96)  (C 50 130 50)
# antimatter: violet on near-black
New-Coil 'antimatter_coil' (C 20 12 28)  (C 220 140 255) (C 150 60 200) (C 80 20 120)
Write-Host 'done'
