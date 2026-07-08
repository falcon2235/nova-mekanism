Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'; $itemDir = Join-Path $root 'item'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$p){$bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose()}
$rng = New-Object System.Random 20260709

# --- star containment casing: deep space blue-black with a bolted frame + starfield ---
$base = C 26 28 46; $dark = C 14 15 28; $light = C 46 50 78; $bolt = C 120 150 220
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $c = $base
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $dark }
  elseif ($x -eq 1 -or $y -eq 1){ $c = $light }
  elseif ($x -eq 14 -or $y -eq 14){ $c = $dark }
  $bmp.SetPixel($x,$y,$c)
}}
# stars
for ($i=0;$i -lt 14;$i++){ $x=3+$rng.Next(10); $y=3+$rng.Next(10); $b=$rng.Next(3); $col = if($b -eq 0){C 255 255 255}elseif($b -eq 1){C 180 200 255}else{C 255 220 160}; $bmp.SetPixel($x,$y,$col) }
foreach ($pt in @(@(2,2),@(13,2),@(2,13),@(13,13))){ $bmp.SetPixel($pt[0],$pt[1],$bolt) }
Save $bmp (Join-Path $dir 'star_casing.png'); Write-Host 'casing star_casing'

# --- star generator controller front: casing base + radiant gold star ring ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $c = $base
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $dark }
  elseif ($x -eq 1 -or $y -eq 1){ $c = $light }
  elseif ($x -eq 14 -or $y -eq 14){ $c = $dark }
  $bmp.SetPixel($x,$y,$c)
}}
$ring = C 255 196 70; $ringH = C 255 240 180
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $dx=$x-7.5; $dy=$y-7.5; $dist=[Math]::Sqrt($dx*$dx+$dy*$dy)
  if ($dist -ge 3.2 -and $dist -le 4.4){ $bmp.SetPixel($x,$y,$ring) }
  elseif ($dist -le 1.6){ $bmp.SetPixel($x,$y,$ringH) }
}}
foreach ($pt in @(@(7,1),@(8,1),@(7,14),@(8,14),@(1,7),@(1,8),@(14,7),@(14,8))){ $bmp.SetPixel($pt[0],$pt[1],$ringH) }
Save $bmp (Join-Path $dir 'star_generator_controller_front.png'); Write-Host 'front star_generator_controller_front'

# --- black hole seed item: dark core with a bright accretion ring ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $dx=$x-7.5; $dy=$y-7.5; $dist=[Math]::Sqrt($dx*$dx+$dy*$dy)
  if ($dist -le 2.2){ $bmp.SetPixel($x,$y,(C 4 2 10)) }                # event horizon (near-black)
  elseif ($dist -le 3.4){ $bmp.SetPixel($x,$y,(C 255 150 40)) }        # inner accretion (hot orange)
  elseif ($dist -le 4.6){ $bmp.SetPixel($x,$y,(C 180 70 200)) }        # outer accretion (violet)
  elseif ($dist -le 5.6){ $bmp.SetPixel($x,$y,(C 60 30 90)) }          # halo
}}
# a couple of bright lensing points
foreach ($pt in @(@(11,4),@(4,11),@(12,9))){ $bmp.SetPixel($pt[0],$pt[1],(C 255 240 220)) }
Save $bmp (Join-Path $itemDir 'black_hole_seed.png'); Write-Host 'item black_hole_seed'
Write-Host 'done'
