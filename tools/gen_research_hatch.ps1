Add-Type -AssemblyName System.Drawing
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}
function SaveB($bmp,$n){$bmp.Save((Join-Path $blockDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# Research data hatch: an assembly-grey frame around a slot that lights when loaded.
function New-Hatch($name,$slotDark,$slotLit,$lit){
  $base=C 118 122 132; $light=C 152 156 166; $dark=C 76 80 90; $bolt=C 176 180 190
  $bmp=New-Object System.Drawing.Bitmap 16,16
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
    $c=$base
    if($x -eq 0 -or $y -eq 0){$c=$light}
    elseif($x -eq 15 -or $y -eq 15){$c=$dark}
    $bmp.SetPixel($x,$y,$c)
  }}
  foreach($p in @(@(2,2),@(13,2),@(2,13),@(13,13))){$bmp.SetPixel($p[0],$p[1],$bolt)}
  # the data slot
  for($x=4;$x -le 11;$x++){for($y=5;$y -le 10;$y++){
    if($x -eq 4 -or $x -eq 11 -or $y -eq 5 -or $y -eq 10){$bmp.SetPixel($x,$y,$dark)}
    else{$bmp.SetPixel($x,$y,$slotDark)}
  }}
  if($lit){
    # an orb seated in the slot, glowing
    foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],$slotLit)}
    foreach($p in @(@(6,7),@(9,7),@(6,8),@(9,8),@(7,6),@(8,6),@(7,9),@(8,9))){
      $bmp.SetPixel($p[0],$p[1],(C 150 235 255))
    }
    # status lamp
    $bmp.SetPixel(2,2,(C 120 255 140))
  } else {
    $bmp.SetPixel(2,2,(C 90 40 40))
  }
  SaveB $bmp $name
}
New-Hatch 'research_hatch'        (C 32 36 44) (C 255 255 255) $false
New-Hatch 'research_hatch_loaded' (C 32 36 44) (C 255 255 255) $true
Write-Host 'done'
