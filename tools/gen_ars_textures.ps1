Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}
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

# sourcestone palette: warm tan stone with glowing teal source swirls (Ars look)
$sBase=C 172 152 128; $sLight=C 202 184 160; $sDark=C 118 102 84; $sBolt=C 226 210 188
$teal=C 60 220 190; $tealHi=C 170 255 235

# sourcestone casing: stone hull + source swirl
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $sBase $sLight $sDark $sBolt
foreach($p in @(@(5,7),@(6,6),@(7,5),@(8,5),@(9,6),@(10,7),@(10,8),@(9,9),@(8,10),@(7,10),@(6,9),@(5,8))){$bmp.SetPixel($p[0],$p[1],$teal)}
foreach($p in @(@(7,7),@(8,8))){$bmp.SetPixel($p[0],$p[1],$tealHi)}
Save $bmp 'sourcestone_casing'

# controller front: stone + imbuement socket with radiant source core
$bmp=New-Object System.Drawing.Bitmap 16,16
Paint-Hull $bmp $sBase $sLight $sDark $sBolt
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$teal)}
  else{$bmp.SetPixel($x,$y,(C 26 32 34))}
}}
foreach($p in @(@(7,5),@(5,7),@(10,8),@(8,10))){$bmp.SetPixel($p[0],$p[1],$teal)}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$tealHi)}
Save $bmp 'grand_imbuement_chamber_controller_front'
Write-Host 'done'
