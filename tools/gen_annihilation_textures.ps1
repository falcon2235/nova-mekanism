Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$n){$bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# palette: deep void-black hull with white-hot annihilation glow seams
$base=C 38 32 48; $light=C 66 58 84; $dark=C 20 16 28; $bolt=C 120 108 150
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

# annihilation casing: void hull + white-hot cross seams (matter/antimatter meeting)
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$hot=C 255 244 220; $warm=C 230 170 255
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,7,$warm); $bmp.SetPixel($x,8,$warm) }
for($y=3;$y -le 12;$y++){ $bmp.SetPixel(7,$y,$warm); $bmp.SetPixel(8,$y,$warm) }
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$hot)}
Save $bmp 'annihilation_casing'

# controller front: void hull + containment ring + white annihilation flash core
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$ring=C 230 170 255
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$ring)}
  else{$bmp.SetPixel($x,$y,(C 12 8 20))}
}}
foreach($p in @(@(6,7),@(9,8),@(7,6),@(8,9))){$bmp.SetPixel($p[0],$p[1],(C 150 90 210))}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 255 250 235))}
Save $bmp 'annihilation_generator_controller_front'
Write-Host 'done'
