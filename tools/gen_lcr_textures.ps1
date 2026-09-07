Add-Type -AssemblyName System.Drawing
# GregTech-spec Large Chemical Reactor blocks: the Chemically Inert (PTFE) casing
# and the PTFE Pipe Casing that sits at the reactor's dead centre.
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}
function SaveB($bmp,$n){$bmp.Save((Join-Path $blockDir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# --- Chemically Inert Machine Casing: GT's pale, faintly green-white PTFE plate ---
$face=C 222 226 216; $hi=C 240 243 236; $lo=C 176 182 170; $edge=C 140 146 136
$bolt=C 246 248 242

$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$face
  if($x -eq 0 -or $y -eq 0){$c=$hi}
  elseif($x -eq 15 -or $y -eq 15){$c=$edge}
  elseif($x -eq 14 -or $y -eq 14){$c=$lo}
  $bmp.SetPixel($x,$y,$c)
}}
# GT casings carry an inset panel line
for($i=3;$i -le 12;$i++){
  $bmp.SetPixel($i,3,$lo); $bmp.SetPixel(3,$i,$lo)
  $bmp.SetPixel($i,12,$hi); $bmp.SetPixel(12,$i,$hi)
}
# corner bolts
foreach($p in @(@(1,1),@(14,1),@(1,14),@(14,14))){
  $bmp.SetPixel($p[0],$p[1],$bolt)
}
SaveB $bmp 'ptfe_casing'

# --- PTFE Pipe Casing: a bundle of four white pipe ends on a dark frame ---
$frame=C 58 60 56; $frameHi=C 84 88 82
$pipe=C 226 230 220; $pipeHi=C 248 250 244; $pipeLo=C 158 164 152; $bore=C 40 42 38

$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$frame
  if($x -eq 0 -or $y -eq 0){$c=$frameHi}
  $bmp.SetPixel($x,$y,$c)
}}
# four 6x6 pipe ends, one per quadrant
foreach($o in @(@(1,1),@(9,1),@(1,9),@(9,9))){
  $ox=$o[0]; $oy=$o[1]
  for($x=0;$x -lt 6;$x++){for($y=0;$y -lt 6;$y++){
    # rounded end: skip the four corners
    if(($x -eq 0 -or $x -eq 5) -and ($y -eq 0 -or $y -eq 5)){ continue }
    $c=$pipe
    if($y -le 1){ $c=$pipeHi }
    elseif($y -ge 4){ $c=$pipeLo }
    $bmp.SetPixel($ox+$x,$oy+$y,$c)
  }}
  # the bore down the middle
  for($x=2;$x -le 3;$x++){for($y=2;$y -le 3;$y++){
    $bmp.SetPixel($ox+$x,$oy+$y,$bore)
  }}
}
SaveB $bmp 'ptfe_pipe_casing'

# --- reactor controller front: inert casing with a reaction-chamber window ---
$bmp=New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
  $c=$face
  if($x -eq 0 -or $y -eq 0){$c=$hi}
  elseif($x -eq 15 -or $y -eq 15){$c=$edge}
  $bmp.SetPixel($x,$y,$c)
}}
for($x=3;$x -le 12;$x++){for($y=3;$y -le 12;$y++){
  if($x -eq 3 -or $x -eq 12 -or $y -eq 3 -or $y -eq 12){$bmp.SetPixel($x,$y,$edge)}
  else{$bmp.SetPixel($x,$y,(C 34 38 34))}
}}
# swirling reagents behind the glass
foreach($p in @(@(6,6),@(7,6),@(8,6),@(9,7),@(9,8),@(8,9),@(7,9),@(6,9),@(6,8),@(6,7))){
  $bmp.SetPixel($p[0],$p[1],(C 120 220 160))
}
foreach($p in @(@(7,7),@(8,7),@(7,8),@(8,8))){$bmp.SetPixel($p[0],$p[1],(C 200 255 220))}
SaveB $bmp 'reactor_controller_front'
Write-Host 'done'
