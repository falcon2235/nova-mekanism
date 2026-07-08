# Fusion glass (transparent blue) + superconductor coil item.
Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'; $itemDir = Join-Path $root 'item'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$p){$bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose()}

# --- fusion glass: transparent pane, glowing cyan frame + shine ---
$bmp = New-Object System.Drawing.Bitmap 16,16
$fr = C 80 200 240; $frD = C 40 130 180; $shine = C 210 250 255 180; $tint = C 120 210 245 46
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = if((($x+$y)%2) -eq 0){$fr}else{$frD}; $bmp.SetPixel($x,$y,$c) }
  elseif (($x -eq $y -and $x -ge 2 -and $x -le 6) -or (($x-1) -eq $y -and $x -ge 3 -and $x -le 7)){ $bmp.SetPixel($x,$y,$shine) }
  else { $bmp.SetPixel($x,$y,$tint) }
}}
Save $bmp (Join-Path $dir 'fusion_glass.png'); Write-Host 'glass fusion_glass'

# --- superconductor: coiled wire on a card ---
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
$card = C 40 46 82; $cardD = C 26 30 56
for ($x=2;$x -le 13;$x++){ for ($y=3;$y -le 12;$y++){ $c = if($y -eq 3 -or $x -eq 2){$card}else{ if($y -eq 12 -or $x -eq 13){$cardD}else{$card} }; $bmp.SetPixel($x,$y,$c) } }
# glowing coil windings (cyan/white)
$w1 = C 120 230 255; $w2 = C 210 250 255
for ($y=4;$y -le 11;$y+=2){ for ($x=3;$x -le 12;$x++){ $c = if((($x+$y)%2) -eq 0){$w2}else{$w1}; $bmp.SetPixel($x,$y,$c) } }
foreach ($p in @(@(3,4),@(12,6),@(3,8),@(12,10))){ $bmp.SetPixel($p[0],$p[1],(C 240 255 255)) }
Save $bmp (Join-Path $itemDir 'superconductor.png'); Write-Host 'item superconductor'
Write-Host 'done'
