Add-Type -AssemblyName System.Drawing
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$itemDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function SaveB($bmp,$n){$bmp.Save((Join-Path $blockDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}
function SaveI($bmp,$n){$bmp.Save((Join-Path $itemDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

function Paint-Hull($bmp,$base,$light,$dark,$bolt){
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
    $c=$base
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){$c=$dark}
    elseif($x -eq 1 -or $y -eq 1){$c=$light}
    elseif($x -eq 14 -or $y -eq 14){$c=$dark}
    $bmp.SetPixel($x,$y,$c)
  }}
  foreach($p in @(@(2,2),@(12,2),@(2,12),@(12,12))){$bmp.SetPixel($p[0],$p[1],$bolt);$bmp.SetPixel($p[0]+1,$p[1],$dark);$bmp.SetPixel($p[0],$p[1]+1,$dark)}
}

# inscriber casing: AE2 sky-stone dark grey with a certus-quartz inlay stripe
$iBase=C 62 60 66; $iLight=C 92 90 98; $iDark=C 38 36 42; $iBolt=C 130 128 138
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $iBase $iLight $iDark $iBolt
$certus=C 186 214 232; $certusHi=C 226 242 252
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$certus); $bmp.SetPixel($x,8,$certus) }
foreach($x in @(4,7,10)){ $bmp.SetPixel($x,7,$certusHi) }
SaveB $bmp 'inscriber_casing'

# inscriber controller front: sky-stone + press slot glyph
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $iBase $iLight $iDark $iBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$certus)}
  else{$bmp.SetPixel($x,$y,(C 24 22 28))}
}}
for($x=5;$x -le 10;$x++){ $bmp.SetPixel($x,6,(C 150 148 158)); $bmp.SetPixel($x,9,(C 150 148 158)) }
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$certusHi)}
SaveB $bmp 'large_inscriber_controller_front'

# charger casing: steel with fluix-purple energy band
$cBase=C 96 100 110; $cLight=C 132 136 148; $cDark=C 58 60 68; $cBolt=C 170 174 186
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $cBase $cLight $cDark $cBolt
$fluix=C 150 90 200; $fluixHi=C 210 160 250
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$fluix); $bmp.SetPixel($x,8,$fluix) }
foreach($x in @(4,7,10)){ $bmp.SetPixel($x,8,$fluixHi) }
SaveB $bmp 'charger_casing'

# charger controller front: fluix ring + lightning glyph
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $cBase $cLight $cDark $cBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$fluix)}
  else{$bmp.SetPixel($x,$y,(C 22 20 28))}
}}
foreach($p in @(@(8,4),@(7,5),@(7,6),@(6,7),@(8,7),@(7,8),@(9,8),@(8,9),@(8,10),@(7,11))){$bmp.SetPixel($p[0],$p[1],$fluixHi)}
SaveB $bmp 'large_charger_controller_front'

# uncharged superconductor item: grey coil (dim version of the superconductor)
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$coil=C 110 112 120; $coilHi=C 150 152 160; $core=C 70 72 80
for($a=0;$a -lt 360;$a+=8){
  $rad=$a*[Math]::PI/180.0
  $x=[int](7.5+5.5*[Math]::Cos($rad)); $y=[int](7.5+5.5*[Math]::Sin($rad))
  if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16){$bmp.SetPixel($x,$y,$coil)}
  $x2=[int](7.5+4.0*[Math]::Cos($rad)); $y2=[int](7.5+4.0*[Math]::Sin($rad))
  if($x2 -ge 0 -and $x2 -lt 16 -and $y2 -ge 0 -and $y2 -lt 16){$bmp.SetPixel($x2,$y2,$coilHi)}
}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$core)}
SaveI $bmp 'uncharged_superconductor'
Write-Host 'done'
