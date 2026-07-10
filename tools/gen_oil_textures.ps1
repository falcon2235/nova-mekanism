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

# --- oil rig: weathered steel with an oil-smear band ---
$rBase=C 122 116 104; $rLight=C 158 152 138; $rDark=C 74 70 62; $rBolt=C 190 184 170
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
$oil=C 30 26 20; $oilHi=C 58 50 36
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$oil); $bmp.SetPixel($x,8,$oil) }
foreach($x in @(4,7,10)){ $bmp.SetPixel($x,7,$oilHi) }
Save $bmp 'oil_rig_casing'

# --- drill pipe (side): steel pipe with segment joints ---
$bmp=New-Object System.Drawing.Bitmap 16,16
$pipe=C 108 104 96; $pipeHi=C 148 144 134; $pipeDk=C 60 58 52
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$pipe
  if($x -le 2 -or $x -ge 13){$c=$pipeDk}
  elseif($x -eq 3 -or $x -eq 12){$c=$pipeHi}
  $bmp.SetPixel($x,$y,$c)
}}
foreach($y in @(3,8,13)){for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,$y,$pipeDk) }}
foreach($y in @(4,9,14)){for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,$y,$pipeHi) }}
Save $bmp 'drill_pipe'

# --- drill pipe (end): ring cross-section ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $dx=[Math]::Abs($x-7.5); $dy=[Math]::Abs($y-7.5); $d=[Math]::Max($dx,$dy)
  $c = if($d -ge 7){$pipeDk} elseif($d -ge 5){$pipe} elseif($d -ge 4){$pipeHi} elseif($d -ge 2){C 40 36 30} else {C 24 20 16}
  $bmp.SetPixel($x,$y,$c)
}}
Save $bmp 'drill_pipe_top'

# --- oil rig controller front: hull + gauge screen with rising oil level ---
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $rBase $rLight $rDark $rBolt
$amber=C 224 170 60
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$amber)}
  else{$bmp.SetPixel($x,$y,(C 26 22 18))}
}}
for($x=4;$x -le 11;$x++){for($y=9;$y -le 11;$y++){ $bmp.SetPixel($x,$y,(C 46 38 26)) }}
foreach($p in @(@(5,5),@(8,6),@(10,4))){$bmp.SetPixel($p[0],$p[1],$amber)}
Save $bmp 'oil_drilling_rig_controller_front'

# --- engine casing: titanium-warm hull with piston fins ---
$eBase=C 150 108 76; $eLight=C 186 142 104; $eDark=C 96 66 46; $eBolt=C 214 176 140
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $eBase $eLight $eDark $eBolt
for($y=5;$y -le 10;$y+=2){for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,$y,$eDark) }}
Save $bmp 'engine_casing'

# --- engine gearbox: dark housing with an interlocking gear pair ---
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $eBase $eLight $eDark $eBolt
$house=C 54 44 36; $gear=C 200 190 170; $gearDk=C 140 130 112
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){ $bmp.SetPixel($x,$y,$house) }}
# left gear
foreach($p in @(@(5,5),@(6,5),@(4,6),@(5,6),@(6,6),@(7,6),@(5,7),@(6,7))){$bmp.SetPixel($p[0],$p[1],$gear)}
foreach($p in @(@(5,4),@(3,6),@(6,8),@(8,6))){$bmp.SetPixel($p[0],$p[1],$gearDk)}
# right gear (offset lower)
foreach($p in @(@(9,8),@(10,8),@(8,9),@(9,9),@(10,9),@(11,9),@(9,10),@(10,10))){$bmp.SetPixel($p[0],$p[1],$gear)}
foreach($p in @(@(9,7),@(7,9),@(10,11),@(12,9))){$bmp.SetPixel($p[0],$p[1],$gearDk)}
Save $bmp 'engine_gearbox'

# --- combustion generator controller front: hull + exhaust screen with flame glow ---
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $eBase $eLight $eDark $eBolt
$flame=C 255 150 40; $flameHi=C 255 220 120
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$eDark)}
  else{$bmp.SetPixel($x,$y,(C 30 24 20))}
}}
foreach($p in @(@(6,10),@(7,10),@(8,10),@(9,10),@(7,9),@(8,9),@(7,8),@(8,7))){$bmp.SetPixel($p[0],$p[1],$flame)}
foreach($p in @(@(7,10),@(8,10),@(8,9))){$bmp.SetPixel($p[0],$p[1],$flameHi)}
Save $bmp 'combustion_generator_controller_front'
Write-Host 'done'
