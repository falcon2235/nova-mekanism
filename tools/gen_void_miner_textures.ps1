Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$n){$bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# palette: dark gunmetal rig steel with warm hazard accents
$base=C 84 88 96; $light=C 120 126 136; $dark=C 52 55 62; $bolt=C 170 178 190
function Paint-Base($bmp){
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
    $c=$base
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){$c=$dark}
    elseif($x -eq 1 -or $y -eq 1){$c=$light}
    elseif($x -eq 14 -or $y -eq 14){$c=$dark}
    $bmp.SetPixel($x,$y,$c)
  }}
  foreach($p in @(@(2,2),@(12,2),@(2,12),@(12,12))){$bmp.SetPixel($p[0],$p[1],$bolt);$bmp.SetPixel($p[0]+1,$p[1],$dark);$bmp.SetPixel($p[0],$p[1]+1,$dark)}
}

# void miner casing: rig hull + diagonal hazard stripe band across the middle
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$yel=C 212 176 60; $blk=C 34 36 40
for($x=2;$x -le 13;$x++){for($y=6;$y -le 9;$y++){
  $c=if(((($x+$y) % 4) -lt 2)){$yel}else{$blk}
  $bmp.SetPixel($x,$y,$c)
}}
Save $bmp 'void_miner_casing'

# void drill (side): dark shaft with a glowing violet void-energy spiral
$bmp=New-Object System.Drawing.Bitmap 16,16
$shaft=C 40 36 52; $shaftHi=C 66 60 84; $v1=C 168 96 255; $v2=C 226 190 255
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$shaft
  if($x -eq 0 -or $x -eq 15){$c=C 24 22 32}
  elseif($x -eq 1 -or $x -eq 14){$c=$shaftHi}
  $bmp.SetPixel($x,$y,$c)
}}
for($y=0;$y -lt 16;$y++){
  $x1=3 + (($y) % 10); $x2=12 - (($y) % 10)
  if($x1 -ge 2 -and $x1 -le 13){$bmp.SetPixel($x1,$y,$v1)}
  if($x2 -ge 2 -and $x2 -le 13){$bmp.SetPixel($x2,$y,$v1)}
  if(($y % 4) -eq 0){$bmp.SetPixel(7,$y,$v2);$bmp.SetPixel(8,$y,$v2)}
}
Save $bmp 'void_drill'

# void drill (top/bottom): concentric violet rings around a bright core
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $dx=[Math]::Abs($x-7.5); $dy=[Math]::Abs($y-7.5)
  $d=[Math]::Max($dx,$dy)
  $c = if($d -ge 7){C 24 22 32} elseif($d -ge 6){$shaftHi} elseif($d -ge 4){$shaft} elseif($d -ge 3){$v1} elseif($d -ge 1.5){C 40 36 52} else {$v2}
  $bmp.SetPixel($x,$y,$c)
}}
Save $bmp 'void_drill_top'

# controller front: rig hull + dark screen with a violet ore-scan grid + core dot
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$ring=C 168 96 255
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$ring)}
  else{$bmp.SetPixel($x,$y,(C 22 20 30))}
}}
for($x=5;$x -le 10;$x+=2){for($y=5;$y -le 10;$y++){ if(($y % 2) -eq 1){$bmp.SetPixel($x,$y,(C 96 60 148))} }}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 226 190 255))}
Save $bmp 'void_ore_miner_controller_front'
Write-Host 'done'
