# GregTech-style machine casing textures: bordered plate + corner bolts,
# plus a wound heating coil.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

# GT-like machine casing: outer border, inner plate, 4 corner bolts, subtle seams
function New-GtCasing($name, $base, $light, $dark, $bolt) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = $base
            # outer border (1px dark, then 1px light bevel)
            if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $c = $dark }
            elseif ($x -eq 1 -or $y -eq 1) { $c = $light }
            elseif ($x -eq 14 -or $y -eq 14) { $c = $dark }
            # subtle plate seams (GT panel look)
            elseif ($y -eq 7 -and $x -ge 3 -and $x -le 12) { $c = $dark }
            elseif ($y -eq 8 -and $x -ge 3 -and $x -le 12) { $c = $light }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    # corner bolts (2x2 with highlight)
    foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
        $bx = $p[0]; $by = $p[1]
        $bmp.SetPixel($bx, $by, $bolt)
        $bmp.SetPixel($bx+1, $by, $dark)
        $bmp.SetPixel($bx, $by+1, $dark)
        $bmp.SetPixel($bx+1, $by+1, $bolt)
    }
    $bmp.Save((Join-Path $dir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "casing $name"
}

# heat-proof (invar green-grey, GT EBF casing)
New-GtCasing 'heat_proof_casing' (C 168 178 150) (C 198 206 180) (C 116 126 100) (C 220 226 204)
# chemically inert PTFE (pale white)
New-GtCasing 'ptfe_casing' (C 222 226 230) (C 244 246 249) (C 164 172 182) (C 250 251 253)
# clean stainless (light metallic)
New-GtCasing 'stainless_casing' (C 200 204 210) (C 232 236 241) (C 142 148 158) (C 245 247 250)

# cupronickel heating coil: horizontal windings with dark frame edges
$bmp = New-Object System.Drawing.Bitmap 16, 16
$frame = C 62 52 46
$copper = C 199 111 58
$copperD = C 138 70 32
$copperL = C 230 152 92
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        if ($x -le 1 -or $x -ge 14) { $bmp.SetPixel($x, $y, $frame); continue }
        $m = $y % 4
        $c = switch ($m) {
            0 { $copperL }
            1 { $copper }
            2 { $copperD }
            3 { $copper }
        }
        # winding seam every 6 px shifts slightly for a wound look
        if ((($x + ($y * 2)) % 16) -eq 0) { $c = $copperD }
        $bmp.SetPixel($x, $y, $c)
    }
}
$bmp.Save((Join-Path $dir 'cupronickel_coil.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host 'coil cupronickel_coil'
Write-Host 'done'
