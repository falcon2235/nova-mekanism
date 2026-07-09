Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$n){$bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# palette: cool steel-blue accelerator hull
$base=C 108 120 138; $light=C 150 164 184; $dark=C 66 76 92; $bolt=C 200 212 230
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

# accelerator casing: hull + a horizontal beam-tube band across the middle
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$tube=C 40 46 58; $tubeHi=C 120 200 235
for($x=3;$x -le 12;$x++){ $bmp.SetPixel($x,6,$dark); $bmp.SetPixel($x,7,$tube); $bmp.SetPixel($x,8,$tube); $bmp.SetPixel($x,9,$light) }
for($x=3;$x -le 12;$x+=3){ $bmp.SetPixel($x,7,$tubeHi) }   # beam glints
Save $bmp 'accelerator_casing'

# collider magnet: dark core with bright cyan electromagnet windings (glowing)
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$mag=C 30 40 60
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){ $bmp.SetPixel($x,$y,$mag) }}
$w1=C 90 210 245; $w2=C 200 245 255
for($y=4;$y -le 11;$y+=2){for($x=3;$x -le 12;$x++){ $c=if((($x+$y)%2) -eq 0){$w2}else{$w1}; $bmp.SetPixel($x,$y,$c) }}
foreach($p in @(@(4,3),@(11,3),@(4,12),@(11,12))){$bmp.SetPixel($p[0],$p[1],$w2)}
Save $bmp 'collider_magnet'

# controller front: hull + dark screen + cyan ring + beam dot
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Base $bmp
$ring=C 90 210 245
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){ if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$ring)}else{$bmp.SetPixel($x,$y,(C 24 30 42))} }}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 210 245 255))}
Save $bmp 'hadron_collider_controller_front'
Write-Host 'done'
