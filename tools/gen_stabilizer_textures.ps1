Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'; $itemDir = Join-Path $root 'item'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$p){$bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose()}
$rng = New-Object System.Random 20260710

# --- neutronium casing: ultra-dense dark metal with a heavy bolted frame + violet sheen ---
$base = C 40 38 52; $dark = C 20 18 30; $light = C 66 62 84; $bolt = C 176 150 220
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $c = $base
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $dark }
  elseif ($x -eq 1 -or $y -eq 1){ $c = $light }
  elseif ($x -eq 14 -or $y -eq 14){ $c = $dark }
  elseif ($y -eq 7 -and $x -ge 3 -and $x -le 12){ $c = $dark }
  elseif ($y -eq 8 -and $x -ge 3 -and $x -le 12){ $c = $light }
  $bmp.SetPixel($x,$y,$c)
}}
# dense speckle sheen
for ($i=0;$i -lt 10;$i++){ $x=3+$rng.Next(10); $y=3+$rng.Next(10); $bmp.SetPixel($x,$y,(C 120 100 160)) }
foreach ($pt in @(@(2,2),@(13,2),@(2,13),@(13,13),@(7,3),@(8,12))){ $bmp.SetPixel($pt[0],$pt[1],$bolt) }
Save $bmp (Join-Path $dir 'neutronium_casing.png'); Write-Host 'casing neutronium_casing'

# --- stabilizer controller front: neutronium base + purple containment ring ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $c = $base
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $dark }
  elseif ($x -eq 1 -or $y -eq 1){ $c = $light }
  elseif ($x -eq 14 -or $y -eq 14){ $c = $dark }
  $bmp.SetPixel($x,$y,$c)
}}
$ring = C 180 90 220; $ringH = C 230 190 255; $core = C 10 6 16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  $dx=$x-7.5; $dy=$y-7.5; $dist=[Math]::Sqrt($dx*$dx+$dy*$dy)
  if ($dist -le 1.8){ $bmp.SetPixel($x,$y,$core) }
  elseif ($dist -ge 3.0 -and $dist -le 4.4){ $bmp.SetPixel($x,$y,$ring) }
}}
foreach ($pt in @(@(7,2),@(8,2),@(7,13),@(8,13),@(2,7),@(2,8),@(13,7),@(13,8))){ $bmp.SetPixel($pt[0],$pt[1],$ringH) }
Save $bmp (Join-Path $dir 'black_hole_stabilizer_controller_front.png'); Write-Host 'front stabilizer'

# --- neutronium item (dense violet-black ingot) ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
$nb = C 52 44 70; $nh = C 96 82 128; $nl = C 26 20 38
for ($x=3;$x -le 12;$x++){ for ($y=6;$y -le 11;$y++){
  $c = $nb; if($y -eq 6 -or $x -eq 3){ $c = $nh }; if($y -eq 11 -or $x -eq 12){ $c = $nl }
  $bmp.SetPixel($x,$y,$c)
}}
$bmp.SetPixel(3,6,(C 0 0 0 0)); $bmp.SetPixel(12,6,$nl)
for ($i=0;$i -lt 6;$i++){ $bmp.SetPixel(4+$rng.Next(8),7+$rng.Next(4),(C 150 130 190)) }
Save $bmp (Join-Path $itemDir 'neutronium.png'); Write-Host 'item neutronium'

# --- transdimensional metal (iridescent rift crystal) ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
foreach ($p in @(@(7,2),@(8,2),@(6,3),@(9,3),@(5,4),@(10,4),@(4,5),@(11,5),@(4,6),@(11,6),@(4,7),@(11,7),@(4,8),@(11,8),@(4,9),@(11,9),@(5,10),@(10,10),@(6,11),@(9,11),@(7,12),@(8,12),@(6,13),@(9,13),@(7,13),@(8,13))){
  $dx=$p[0]-7.5; $col = if($dx -lt -2){C 90 210 230}elseif($dx -lt 1){C 180 130 240}else{C 240 130 190}
  $bmp.SetPixel($p[0],$p[1],$col)
}
# fill interior with shifting colors
for ($x=5;$x -le 10;$x++){ for ($y=4;$y -le 11;$y++){
  $cur = $bmp.GetPixel($x,$y); if ($cur.A -eq 0){
    $t=($x+$y)%3; $col = if($t -eq 0){C 120 200 255}elseif($t -eq 1){C 200 150 250}else{C 255 170 210}
    $bmp.SetPixel($x,$y,$col)
  }
}}
foreach ($p in @(@(7,5),@(8,9),@(6,7))){ $bmp.SetPixel($p[0],$p[1],(C 255 255 255)) }
Save $bmp (Join-Path $itemDir 'transdimensional_metal.png'); Write-Host 'item transdimensional_metal'
Write-Host 'done'
