Add-Type -AssemblyName System.Drawing
# Catalytic reformer (v1.2.1): refinery-column casing, controller face and the two items.
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$itemDir  = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function SaveB($b,$n){$b.Save((Join-Path $blockDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$b.Dispose();Write-Host $n}
function SaveI($b,$n){$b.Save((Join-Path $itemDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$b.Dispose();Write-Host $n}

# --- reformer casing: refinery stainless with a riveted vertical seam ---
$base=C 176 182 178; $light=C 208 214 210; $dark=C 122 128 126; $rivet=C 226 230 228
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$base
  if($x -eq 0 -or $y -eq 0){$c=$light}
  elseif($x -eq 15 -or $y -eq 15){$c=$dark}
  $bmp.SetPixel($x,$y,$c)
}}
# the vertical weld seam of a pressure column, riveted down both sides
for($y=1;$y -le 14;$y++){ $bmp.SetPixel(7,$y,$dark); $bmp.SetPixel(8,$y,$light) }
for($y=2;$y -le 13;$y+=3){ $bmp.SetPixel(5,$y,$rivet); $bmp.SetPixel(10,$y,$rivet) }
SaveB $bmp 'reformer_casing'

# --- controller front: a sight glass over the catalyst bed ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$base
  if($x -eq 0 -or $y -eq 0){$c=$light}
  elseif($x -eq 15 -or $y -eq 15){$c=$dark}
  $bmp.SetPixel($x,$y,$c)
}}
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$dark)}
  else{$bmp.SetPixel($x,$y,(C 40 34 26))}
}}
# packed catalyst pellets glowing with reaction heat
foreach($p in @(@(5,9),@(7,9),@(9,9),@(11,9),@(6,11),@(8,11),@(10,11))){
  $bmp.SetPixel($p[0],$p[1],(C 232 210 160))
}
foreach($p in @(@(5,10),@(7,10),@(9,10),@(11,10),@(6,10),@(8,10),@(10,10))){
  $bmp.SetPixel($p[0],$p[1],(C 168 148 104))
}
# hot vapour rising off the bed
foreach($p in @(@(6,5),@(9,5),@(7,6),@(10,6),@(5,7),@(8,7))){
  $bmp.SetPixel($p[0],$p[1],(C 255 168 72))
}
SaveB $bmp 'catalytic_reformer_controller_front'

# --- reforming catalyst: a white alumina pellet cake with platinum specks ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$cake=C 226 224 214; $cakeHi=C 248 246 238; $cakeLo=C 172 170 160; $pt=C 214 222 232; $ptHi=C 246 250 255
for($x=3;$x -le 12;$x++){for($y=4;$y -le 11;$y++){
  if(($x -eq 3 -or $x -eq 12) -and ($y -eq 4 -or $y -eq 11)){ continue }
  $c=$cake
  if($y -le 5){ $c=$cakeHi } elseif($y -ge 10){ $c=$cakeLo }
  $bmp.SetPixel($x,$y,$c)
}}
# platinum dispersed through the support
foreach($p in @(@(5,6),@(9,5),@(7,8),@(11,7),@(4,9),@(8,10))){ $bmp.SetPixel($p[0],$p[1],$pt) }
foreach($p in @(@(5,6),@(9,5))){ $bmp.SetPixel($p[0],$p[1],$ptHi) }
SaveI $bmp 'reforming_catalyst'

# --- octane booster: an amber aromatic crystal ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$am=C 226 142 46; $amHi=C 255 206 122; $amLo=C 152 88 24
# a faceted lozenge
for($y=3;$y -le 12;$y++){
  $half = 4 - [math]::Abs($y - 7)
  if($half -lt 1){ $half = 1 }
  for($x=8-$half;$x -le 7+$half;$x++){
    $c=$am
    if($x -lt 8 -and $y -lt 8){ $c=$amHi } elseif($y -gt 9){ $c=$amLo }
    $bmp.SetPixel($x,$y,$c)
  }
}
$bmp.SetPixel(6,5,(C 255 244 214)); $bmp.SetPixel(7,5,(C 255 232 186))
SaveI $bmp 'octane_booster'
Write-Host 'done'
