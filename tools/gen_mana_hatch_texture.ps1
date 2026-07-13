Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}

# mana hatch: livingrock hull + glowing cyan mana window (matches livingrock_casing family)
$bmp=New-Object System.Drawing.Bitmap 16,16
$base=C 208 204 196; $light=C 232 228 220; $dark=C 150 146 138; $bolt=C 246 244 238
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$base
  if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){$c=$dark}
  elseif($x -eq 1 -or $y -eq 1){$c=$light}
  elseif($x -eq 14 -or $y -eq 14){$c=$dark}
  $bmp.SetPixel($x,$y,$c)
}}
foreach($p in @(@(2,2),@(12,2),@(2,12),@(12,12))){$bmp.SetPixel($p[0],$p[1],$bolt);$bmp.SetPixel($p[0]+1,$p[1],$dark);$bmp.SetPixel($p[0],$p[1]+1,$dark)}
$mana=C 70 200 240; $manaHi=C 170 240 255
for($x=4;$x -le 11;$x++){for($y=4;$y -le 11;$y++){
  if($x -eq 4 -or $x -eq 11 -or $y -eq 4 -or $y -eq 11){$bmp.SetPixel($x,$y,$dark)}
  else{$bmp.SetPixel($x,$y,$mana)}
}}
foreach($p in @(@(6,6),@(9,8),@(7,9))){$bmp.SetPixel($p[0],$p[1],$manaHi)}
$bmp.Save((Join-Path $dir 'mana_hatch.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host 'mana_hatch saved'
