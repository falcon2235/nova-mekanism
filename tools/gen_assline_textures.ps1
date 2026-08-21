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

# assline palette: GT yellow-accent machine steel
$aBase=C 118 122 130; $aLight=C 154 158 168; $aDark=C 70 73 80; $aBolt=C 190 194 204
$yellow=C 224 188 60; $yellowDk=C 150 122 34

# assline casing: steel hull + yellow hazard corners
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $aBase $aLight $aDark $aBolt
foreach($p in @(@(3,3),@(4,3),@(3,4),@(12,3),@(11,3),@(12,4),@(3,12),@(3,11),@(4,12),@(12,12),@(12,11),@(11,12))){$bmp.SetPixel($p[0],$p[1],$yellow)}
SaveB $bmp 'assline_casing'

# assline grate: dark frame with slats
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $aBase $aLight $aDark $aBolt
for($y=3;$y -le 12;$y+=3){for($x=2;$x -le 13;$x++){ $bmp.SetPixel($x,$y,$aDark); $bmp.SetPixel($x,$y+1,(C 30 32 36)) }}
SaveB $bmp 'assline_grate'

# assline conveyor: rolling belt with yellow chevrons
$bmp=New-Object System.Drawing.Bitmap 16,16
$belt=C 44 46 52; $beltHi=C 74 78 86
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$belt
  if($y -le 1 -or $y -ge 14){$c=$aDark}
  elseif($y -eq 2 -or $y -eq 13){$c=$aLight}
  $bmp.SetPixel($x,$y,$c)
}}
for($x=0;$x -lt 16;$x+=4){for($i=0;$i -lt 3;$i++){
  $bx=$x+$i; if($bx -lt 16){ $bmp.SetPixel($bx,(6+$i),$yellow); $bmp.SetPixel($bx,(10-$i),$yellowDk) }
}}
SaveB $bmp 'assline_conveyor'

# assembly line controller front: belt window + yellow frame
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $aBase $aLight $aDark $aBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$yellow)}
  else{$bmp.SetPixel($x,$y,$belt)}
}}
for($x=5;$x -le 10;$x+=2){ $bmp.SetPixel($x,7,$yellow); $bmp.SetPixel($x+1,8,$yellowDk) }
SaveB $bmp 'assembly_line_controller_front'

# research casing: pale lab panel with cyan trace lines
$rBase=C 176 182 190; $rLight=C 210 216 224; $rDark=C 116 122 130; $rBolt=C 236 240 246
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
$trace=C 80 200 230
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,5,$trace) }
for($y=5;$y -le 10;$y++){ $bmp.SetPixel(9,$y,$trace) }
$bmp.SetPixel(9,10,(C 200 245 255))
SaveB $bmp 'research_casing'

# research station controller front: screen with scan reticle
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$trace)}
  else{$bmp.SetPixel($x,$y,(C 18 24 30))}
}}
foreach($p in @(@(7,5),@(8,5),@(5,7),@(5,8),@(10,7),@(10,8),@(7,10),@(8,10))){$bmp.SetPixel($p[0],$p[1],$trace)}
foreach($p in @(@(7,7),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 200 245 255))}
SaveB $bmp 'research_station_controller_front'

# --- items ---
function Paint-Orb($core,$coreHi,$name){
  $bmp=New-Object System.Drawing.Bitmap 16,16
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
  for($x=4;$x -le 11;$x++){for($y=4;$y -le 11;$y++){
    $dx=$x-7.5; $dy=$y-7.5
    if(($dx*$dx+$dy*$dy) -le 14){$bmp.SetPixel($x,$y,$core)}
  }}
  foreach($p in @(@(6,6),@(7,5))){$bmp.SetPixel($p[0],$p[1],$coreHi)}
  for($x=5;$x -le 10;$x++){ $bmp.SetPixel($x,12,(C 60 62 70)) }
  for($x=6;$x -le 9;$x++){ $bmp.SetPixel($x,13,(C 90 94 104)) }
  SaveI $bmp $name
}
Paint-Orb (C 120 126 138) (C 190 196 208) 'data_orb'
Paint-Orb (C 90 210 245) (C 210 245 255) 'research_data_superconductor'
Paint-Orb (C 255 170 60) (C 255 225 160) 'research_data_fusion'
Paint-Orb (C 168 96 255) (C 226 190 255) 'research_data_void_mining'
Paint-Orb (C 200 90 220) (C 250 190 255) 'research_data_transdimensional'
Paint-Orb (C 120 220 255) (C 215 245 255) 'research_data_cryogenics'
Paint-Orb (C 255 130 70)  (C 255 205 165) 'research_data_metallurgy'
Paint-Orb (C 110 100 90)  (C 190 180 165) 'research_data_petrochemistry'
Paint-Orb (C 80 235 190)  (C 195 250 235) 'research_data_particle'
Paint-Orb (C 255 90 150)  (C 255 190 215) 'research_data_antimatter'
Paint-Orb (C 240 200 90)  (C 255 240 190) 'research_data_replication'
Paint-Orb (C 100 160 255) (C 195 220 255) 'research_data_digital'
Paint-Orb (C 150 255 120) (C 220 255 200) 'research_data_arcane'
Write-Host 'done'
