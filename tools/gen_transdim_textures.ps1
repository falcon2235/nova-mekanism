Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$itemDir = Join-Path $root 'item'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$p){$bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose()}
$rng = New-Object System.Random 20260711

# --- transdimensional alloy ingot (iridescent violet-teal) ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
for ($x=3;$x -le 12;$x++){ for ($y=6;$y -le 11;$y++){
  $t=($x+$y)%3
  $b = if($t -eq 0){C 150 90 210}elseif($t -eq 1){C 90 180 220}else{C 200 120 200}
  if($y -eq 6 -or $x -eq 3){ $b = C 210 180 240 }
  if($y -eq 11 -or $x -eq 12){ $b = C 70 50 110 }
  $bmp.SetPixel($x,$y,$b)
}}
$bmp.SetPixel(3,6,(C 0 0 0 0)); $bmp.SetPixel(12,6,(C 70 50 110))
foreach ($p in @(@(5,7),@(9,9),@(7,8))){ $bmp.SetPixel($p[0],$p[1],(C 255 255 255)) }
Save $bmp (Join-Path $itemDir 'transdimensional_alloy.png'); Write-Host 'item transdimensional_alloy'

# --- transdimensional circuit (dark board + iridescent traces + a rift chip) ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
$board = C 30 20 46; $boardD = C 18 12 30
for ($x=2;$x -le 13;$x++){ for ($y=2;$y -le 13;$y++){
  $c = $board; if($y -eq 2 -or $x -eq 2){ $c = C 60 44 86 }; if($y -eq 13 -or $x -eq 13){ $c = $boardD }
  $bmp.SetPixel($x,$y,$c)
}}
# iridescent traces
$tr = C 120 210 230; $tr2 = C 200 130 240
foreach ($p in @(@(4,4),@(5,4),@(6,4),@(4,5),@(4,6),@(4,7),@(11,4),@(11,5),@(11,6),@(4,11),@(5,11),@(6,11),@(11,11),@(10,11),@(9,11),@(11,10),@(11,9))){ $bmp.SetPixel($p[0],$p[1],$tr) }
foreach ($p in @(@(7,4),@(8,4),@(4,8),@(4,9),@(11,7),@(11,8),@(7,11),@(8,11))){ $bmp.SetPixel($p[0],$p[1],$tr2) }
# central rift chip
for ($x=6;$x -le 9;$x++){ for ($y=6;$y -le 9;$y++){ $bmp.SetPixel($x,$y,(C 10 6 16)) } }
foreach ($p in @(@(7,6),@(8,7),@(6,8),@(9,9))){ $bmp.SetPixel($p[0],$p[1],(C 255 180 240)) }
$bmp.SetPixel(7,7,(C 120 220 255)); $bmp.SetPixel(8,8,(C 220 150 250))
Save $bmp (Join-Path $itemDir 'transdimensional_circuit.png'); Write-Host 'item transdimensional_circuit'
Write-Host 'done'
