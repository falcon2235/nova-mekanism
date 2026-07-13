Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$n){$bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

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

# livingrock casing: pale living stone with a cyan mana vein
$lBase=C 208 204 196; $lLight=C 232 228 220; $lDark=C 150 146 138; $lBolt=C 246 244 238
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $lBase $lLight $lDark $lBolt
$mana=C 70 200 240; $manaHi=C 160 235 255
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$mana); $bmp.SetPixel($x,8,$mana) }
foreach($x in @(5,8,11)){ $bmp.SetPixel($x,7,$manaHi) }
Save $bmp 'livingrock_casing'

# mana pool controller front: livingrock + round pool of glowing mana
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $lBase $lLight $lDark $lBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  $dx=[Math]::Abs($x-7.5); $dy=[Math]::Abs($y-7.5)
  if(($dx*$dx+$dy*$dy) -le 20){$bmp.SetPixel($x,$y,$mana)}
}}
foreach($p in @(@(6,6),@(9,7),@(7,9))){$bmp.SetPixel($p[0],$p[1],$manaHi)}
Save $bmp 'grand_mana_pool_controller_front'

# elven gate casing: livingwood brown with glimmering green runes
$eBase=C 116 84 54; $eLight=C 148 112 76; $eDark=C 76 54 34; $eBolt=C 180 146 104
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $eBase $eLight $eDark $eBolt
$rune=C 130 235 130; $runeHi=C 210 255 200
foreach($p in @(@(4,5),@(5,4),@(11,4),@(10,5),@(4,10),@(5,11),@(11,11),@(10,10),@(7,7),@(8,8))){$bmp.SetPixel($p[0],$p[1],$rune)}
foreach($p in @(@(7,8),@(8,7))){$bmp.SetPixel($p[0],$p[1],$runeHi)}
Save $bmp 'elven_gate_casing'

# elven gate controller front: portal arch with pink alfheim glow
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $eBase $eLight $eDark $eBolt
$portal=C 235 120 200; $portalHi=C 255 190 235
for($x=4;$x -le 11;$x++){for($y=4;$y -le 12;$y++){
  if($x -eq 4 -or $x -eq 11 -or $y -eq 4){$bmp.SetPixel($x,$y,$rune)}
  else{$bmp.SetPixel($x,$y,$portal)}
}}
foreach($p in @(@(6,7),@(8,9),@(9,6))){$bmp.SetPixel($p[0],$p[1],$portalHi)}
Save $bmp 'grand_elven_gate_controller_front'

# terra plate casing: livingrock with green terrasteel lattice
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $lBase $lLight $lDark $lBolt
$terra=C 90 190 90; $terraHi=C 160 235 150
for($x=3;$x -le 12;$x+=3){for($y=3;$y -le 12;$y++){ $bmp.SetPixel($x,$y,$terra) }}
for($y=3;$y -le 12;$y+=3){for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,$y,$terra) }}
foreach($p in @(@(6,6),@(9,9))){$bmp.SetPixel($p[0],$p[1],$terraHi)}
Save $bmp 'terra_plate_casing'

# terra plate controller front: concentric terrasteel rings
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $lBase $lLight $lDark $lBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  $dx=[Math]::Abs($x-7.5); $dy=[Math]::Abs($y-7.5); $d=[Math]::Max($dx,$dy)
  $c = if($d -ge 4){$terra} elseif($d -ge 3){C 40 80 40} elseif($d -ge 1.5){$terra} else {$terraHi}
  $bmp.SetPixel($x,$y,$c)
}}
Save $bmp 'grand_terra_plate_controller_front'
Write-Host 'done'
