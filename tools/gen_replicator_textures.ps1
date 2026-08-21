Add-Type -AssemblyName System.Drawing
# Matter replication line: the containment casing/controller and the pattern items.
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$itemDir  = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
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

# replicator hull: pale bone-white containment with amber mass-energy seams
$rBase=C 196 190 172; $rLight=C 226 220 202; $rDark=C 140 134 118; $rBolt=C 246 242 226
$amber=C 240 200 90; $amberHi=C 255 240 180

$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
# a lattice of seams: matter being knitted together
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$amber); $bmp.SetPixel($x,8,$amber) }
for($y=3;$y -le 12;$y++){ $bmp.SetPixel(7,$y,$amber); $bmp.SetPixel(8,$y,$amber) }
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$amberHi)}
foreach($p in @(@(4,4),@(11,4),@(4,11),@(11,11))){$bmp.SetPixel($p[0],$p[1],$amberHi)}
SaveB $bmp 'replicator_casing'

# controller front: containment window with a half-formed object inside
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$amber)}
  else{$bmp.SetPixel($x,$y,(C 26 24 20))}
}}
# the copy condensing: a solid core fading into scattered motes
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$amberHi)}
foreach($p in @(@(6,6),@(9,6),@(6,9),@(9,9))){$bmp.SetPixel($p[0],$p[1],$amber)}
foreach($p in @(@(5,8),@(10,7),@(8,5),@(7,10))){$bmp.SetPixel($p[0],$p[1],(C 150 120 60))}
SaveB $bmp 'matter_replicator_controller_front'

# --- items ---
# residue: a small dull clinker
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$res=C 108 96 88; $resHi=C 150 136 124; $resLo=C 66 58 52
foreach($p in @(@(6,5),@(7,5),@(5,6),@(6,6),@(7,6),@(8,6),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(6,8),@(7,8),@(8,8),@(9,8),@(7,9),@(8,9))){
  $bmp.SetPixel($p[0],$p[1],$res)
}
foreach($p in @(@(6,5),@(5,6),@(6,6))){$bmp.SetPixel($p[0],$p[1],$resHi)}
foreach($p in @(@(9,8),@(8,9),@(7,9))){$bmp.SetPixel($p[0],$p[1],$resLo)}
SaveI $bmp 'exotic_residue'

# pattern cards: a lattice card with a coloured signature in the middle
function New-Pattern($name,$sigA,$sigB,$blank=$false){
  $bmp=New-Object System.Drawing.Bitmap 16,16
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
  $edge=C 70 74 86; $face=C 132 138 154; $faceHi=C 172 178 196
  for($x=3;$x -le 12;$x++){for($y=2;$y -le 13;$y++){
    $c = if($x -eq 3 -or $x -eq 12 -or $y -eq 2 -or $y -eq 13){ $edge } else { $face }
    $bmp.SetPixel($x,$y,$c)
  }}
  for($x=4;$x -le 11;$x++){ $bmp.SetPixel($x,3,$faceHi) }
  # contact strip along the bottom, like a data card
  for($x=5;$x -le 10;$x+=2){ $bmp.SetPixel($x,12,(C 214 190 90)) }
  if(-not $blank){
    # the imprinted signature
    foreach($p in @(@(7,6),@(8,6),@(7,7),@(8,7))){$bmp.SetPixel($p[0],$p[1],$sigA)}
    foreach($p in @(@(6,5),@(9,5),@(6,8),@(9,8),@(7,5),@(8,5))){$bmp.SetPixel($p[0],$p[1],$sigB)}
    $bmp.SetPixel(7,6,(C 255 255 255))
  } else {
    foreach($p in @(@(6,6),@(9,6),@(6,8),@(9,8))){$bmp.SetPixel($p[0],$p[1],(C 96 102 116))}
  }
  SaveI $bmp $name
}
New-Pattern 'blank_matter_pattern'        (C 0 0 0) (C 0 0 0) $true
New-Pattern 'matter_pattern_nether_star'  (C 255 250 210) (C 220 210 160)
New-Pattern 'matter_pattern_dragon_egg'   (C 190 90 240)  (C 110 40 160)
New-Pattern 'matter_pattern_chaos_shard'  (C 255 130 60)  (C 180 70 30)
New-Pattern 'matter_pattern_gaia_spirit'  (C 255 150 220) (C 200 90 170)
Write-Host 'done'
