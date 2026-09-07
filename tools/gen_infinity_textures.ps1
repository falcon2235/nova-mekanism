Add-Type -AssemblyName System.Drawing
# Infinity tier (v1.2.0): the trans-dimensional fusion reactor's shell and its products.
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$itemDir  = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function SaveB($bmp,$n){$bmp.Save((Join-Path $blockDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}
function SaveI($bmp,$n){$bmp.Save((Join-Path $itemDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# --- trans-dimensional casing: deep violet plating with a white rift seam ---
$base=C 46 34 74; $light=C 74 58 112; $dark=C 26 18 44; $bolt=C 100 84 148
$rift=C 236 228 255; $riftDim=C 168 140 232

$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$base
  if($x -eq 0 -or $y -eq 0){$c=$light}
  elseif($x -eq 15 -or $y -eq 15){$c=$dark}
  elseif(($x+$y) % 7 -eq 0){$c=$light}
  $bmp.SetPixel($x,$y,$c)
}}
foreach($p in @(@(2,2),@(13,2),@(2,13),@(13,13))){$bmp.SetPixel($p[0],$p[1],$bolt)}
# the rift: a jagged tear across the plate, brightest at the centre
foreach($p in @(@(4,11),@(5,10),@(6,10),@(7,9),@(8,8),@(9,7),@(10,6),@(11,5),@(12,4))){
  $bmp.SetPixel($p[0],$p[1],$rift)
}
foreach($p in @(@(4,12),@(5,11),@(6,9),@(7,10),@(9,8),@(10,7),@(11,6),@(12,5))){
  $bmp.SetPixel($p[0],$p[1],$riftDim)
}
SaveB $bmp 'transdimensional_casing'

# --- controller front: the same plating with a containment lens ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$base
  if($x -eq 0 -or $y -eq 0){$c=$light}
  elseif($x -eq 15 -or $y -eq 15){$c=$dark}
  $bmp.SetPixel($x,$y,$c)
}}
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$bolt)}
  else{$bmp.SetPixel($x,$y,(C 16 10 28))}
}}
# fusion point: white core bleeding into violet
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 255 255 255))}
foreach($p in @(@(6,7),@(9,7),@(6,8),@(9,8),@(7,6),@(8,6),@(7,9),@(8,9))){$bmp.SetPixel($p[0],$p[1],$rift)}
foreach($p in @(@(5,6),@(10,6),@(5,9),@(10,9),@(6,5),@(9,5),@(6,10),@(9,10))){$bmp.SetPixel($p[0],$p[1],$riftDim)}
SaveB $bmp 'transdimensional_fusion_controller_front'

# (infinity_alloy uses the shared Mekanism-style ingot painter in gen_mekstyle_items.ps1)

# --- infinity circuit: a white board with prismatic traces ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$board=C 234 232 244; $boardHi=C 255 255 255; $boardEdge=C 138 126 180
for($x=2;$x -le 13;$x++){for($y=2;$y -le 13;$y++){
  $c = if($x -eq 2 -or $x -eq 13 -or $y -eq 2 -or $y -eq 13){ $boardEdge } else { $board }
  $bmp.SetPixel($x,$y,$c)
}}
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,3,$boardHi) }
# traces in shifting hues: the circuit is not one colour
foreach($p in @(@(5,5),@(6,5),@(7,5))){$bmp.SetPixel($p[0],$p[1],(C 255 110 140))}
foreach($p in @(@(9,6),@(10,6),@(10,7))){$bmp.SetPixel($p[0],$p[1],(C 120 200 255))}
foreach($p in @(@(5,9),@(5,10),@(6,10))){$bmp.SetPixel($p[0],$p[1],(C 150 255 170))}
foreach($p in @(@(9,10),@(10,10),@(11,10))){$bmp.SetPixel($p[0],$p[1],(C 255 210 110))}
# central die
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 40 32 64))}
$bmp.SetPixel(7,7,(C 255 255 255))
SaveI $bmp 'infinity_circuit'
Write-Host 'done'
