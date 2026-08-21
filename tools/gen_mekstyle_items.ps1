Add-Type -AssemblyName System.Drawing
# Redraws every ingot / dust / gem-ish item in the visual language Mekanism uses, so
# our materials sit consistently next to Mekanism's in inventories and JEI:
#   ingot - a rounded 3-D bar seen at an angle: bright top face, mid front face,
#           dark bottom lip, specular highlight on the top-left
#   dust  - a heaped mound: pointed top, wide base, lit from the top-left, with a
#           few granule speckles for texture
# The artwork is drawn from scratch here (only the silhouette/shading *convention*
# follows Mekanism); colours are this mod's own per-material palette.
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}
function Shade($c,[int]$d){
  $r=[Math]::Max(0,[Math]::Min(255,$c.R+$d)); $g=[Math]::Max(0,[Math]::Min(255,$c.G+$d)); $b=[Math]::Max(0,[Math]::Min(255,$c.B+$d))
  [System.Drawing.Color]::FromArgb(255,$r,$g,$b)
}
function NewCanvas(){
  $bmp = New-Object System.Drawing.Bitmap 16,16
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)) }}
  return $bmp
}
function Save($bmp,$n){
  $bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose(); Write-Host $n
}

# --- ingot: angled 3-D bar (Mekanism convention) ---
# rows are [y] = @(xStart, xEnd) spans of the front face; the top face is drawn
# one row above and inset, giving the classic parallelogram look.
# Traces a 1px outline around every filled pixel. Deriving the outline from the fill
# (instead of hand-plotting it) keeps the silhouette coherent — hand-plotted outlines
# drift out of sync with the spans and leave notches.
function Add-Outline($bmp,$outline){
  $filled = New-Object 'bool[,]' 16,16
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $filled[$x,$y] = ($bmp.GetPixel($x,$y).A -ne 0) }}
  for($x=0;$x -lt 16;$x++){
    for($y=0;$y -lt 16;$y++){
      if($filled[$x,$y]){ continue }
      $touch = $false
      foreach($d in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){
        $nx=$x+$d[0]; $ny=$y+$d[1]
        if($nx -ge 0 -and $nx -lt 16 -and $ny -ge 0 -and $ny -lt 16 -and $filled[$nx,$ny]){ $touch = $true; break }
      }
      if($touch){ $bmp.SetPixel($x,$y,$outline) }
    }
  }
}

function New-Ingot($name,$base,$hi,$lo){
  $bmp = NewCanvas
  $outline = Shade $lo -46
  $topMid  = Shade $hi -14
  # The bar is drawn in isometric skew: the visible TOP face is a parallelogram that
  # slides left as it comes forward, and the FRONT face hangs below it. That offset —
  # not a symmetric shape — is what makes it read as a metal ingot rather than a pill.
  # rows: y => (xStart, xEnd, colour)
  $rows = @(
    @(4, 6, 12, $hi),      # top face, back edge (brightest)
    @(5, 5, 12, $hi),      # top face
    @(6, 4, 11, $topMid),  # top face, front edge
    @(7, 3, 11, $base),    # front face
    @(8, 3, 10, $base),    # front face
    @(9, 3, 10, $lo)       # bottom lip (in shadow)
  )
  foreach($r in $rows){
    for($x=$r[1]; $x -le $r[2]; $x++){ $bmp.SetPixel($x,$r[0],$r[3]) }
  }
  # lit left edge / shadowed right edge on the front face
  $bmp.SetPixel(3,7,(Shade $base 20)); $bmp.SetPixel(3,8,(Shade $base 20))
  $bmp.SetPixel(11,7,(Shade $base -24)); $bmp.SetPixel(10,8,(Shade $base -24))
  # specular glint across the top face
  $bmp.SetPixel(8,4,(Shade $hi 28)); $bmp.SetPixel(9,4,(Shade $hi 28)); $bmp.SetPixel(7,5,(Shade $hi 20))
  Add-Outline $bmp $outline
  Save $bmp $name
}

# --- dust: heaped mound (Mekanism convention) ---
function New-Dust($name,$base,$hi){
  $bmp = NewCanvas
  $lo = Shade $base -46
  $outline = Shade $base -78
  # mound silhouette: narrow at the top, widening toward the base
  $spans = @(
    @(7,8),    # y=3
    @(6,9),    # y=4
    @(5,10),   # y=5
    @(4,11),   # y=6
    @(3,12),   # y=7
    @(3,12),   # y=8
    @(2,13),   # y=9
    @(2,13),   # y=10
    @(3,12)    # y=11
  )
  $y = 3
  foreach($s in $spans){
    for($x=$s[0]; $x -le $s[1]; $x++){
      # light falls from the top-left: shade by distance along the diagonal
      $t = (($x - $s[0]) + ($y - 3)) / 14.0
      $c = if($t -lt 0.30){ $hi } elseif($t -lt 0.62){ $base } else { $lo }
      $bmp.SetPixel($x,$y,$c)
    }
    $y++
  }
  # granule speckles so it reads as loose powder, not a solid blob
  foreach($p in @(@(6,6),@(9,8),@(5,9),@(11,10),@(8,5))){ $bmp.SetPixel($p[0],$p[1],(Shade $hi 20)) }
  foreach($p in @(@(10,7),@(4,8),@(7,10),@(12,9))){ $bmp.SetPixel($p[0],$p[1],$lo) }
  Add-Outline $bmp $outline
  Save $bmp $name
}

# --- raw ore: a cluster of rounded lumps (Mekanism convention) ---
# Mekanism draws raw ores as two or three overlapping nuggets, each lit from the
# top-left. Drawing discs (rather than one blob) is what gives the chunky ore read.
function Add-Lump($bmp,$cx,$cy,$r,$base,$hi,$lo){
  for($x=0;$x -lt 16;$x++){
    for($y=0;$y -lt 16;$y++){
      $dx = $x - $cx; $dy = $y - $cy
      if(($dx*$dx + $dy*$dy) -gt ($r*$r)){ continue }
      # shade along the top-left -> bottom-right diagonal of THIS lump
      $t = ($dx + $dy) / (2.0 * $r)
      $c = if($t -lt -0.30){ $hi } elseif($t -lt 0.35){ $base } else { $lo }
      $bmp.SetPixel($x,$y,$c)
    }
  }
}

function New-Raw($name,$base,$hi,$lo){
  $bmp = NewCanvas
  $outline = Shade $lo -60
  # big lump upper-left, medium lump lower-right, small lump upper-right.
  # The small lump is kept clear of the big one so the cluster silhouette stays
  # legible instead of fusing into one wide mass.
  Add-Lump $bmp 5.6 6.2 3.5 $base $hi $lo
  Add-Lump $bmp 10.2 10.2 2.9 $base $hi $lo
  Add-Lump $bmp 11.6 4.4 2.0 $base $hi $lo
  # a couple of mineral glints
  $bmp.SetPixel(4,5,(Shade $hi 24)); $bmp.SetPixel(9,9,(Shade $hi 18)); $bmp.SetPixel(11,4,(Shade $hi 18))
  Add-Outline $bmp $outline
  Save $bmp $name
}

# ---------------- raw ores ----------------
New-Raw 'raw_antimony'  (C 150 158 172) (C 196 204 216) (C 104 112 126)
New-Raw 'raw_bauxite'   (C 172 104 72)  (C 204 136 100) (C 128 72 48)
New-Raw 'raw_chromium'  (C 172 184 200) (C 210 222 238) (C 122 134 150)
New-Raw 'raw_cooperite' (C 214 220 214) (C 244 248 244) (C 158 164 158)
New-Raw 'raw_magnesium' (C 154 160 168) (C 190 196 204) (C 108 114 122)
New-Raw 'raw_naquadah'  (C 40 78 52)    (C 88 160 104)  (C 24 50 32)
New-Raw 'raw_nickel'    (C 176 182 160) (C 208 214 192) (C 128 134 112)
New-Raw 'raw_titanium'  (C 110 123 139) (C 150 163 179) (C 74 85 104)
New-Raw 'raw_zinc'      (C 156 172 182) (C 198 214 224) (C 106 120 130)
New-Raw 'singularity_fragment' (C 62 40 96)   (C 168 120 235) (C 30 16 52)

# ---------------- ingots ----------------
New-Ingot 'aluminum_ingot'          (C 214 220 228) (C 240 244 250) (C 156 164 176)
New-Ingot 'chromium_ingot'          (C 190 202 218) (C 230 240 252) (C 132 146 164)
New-Ingot 'cupronickel_ingot'       (C 212 166 136) (C 238 198 170) (C 150 108 82)
New-Ingot 'iridium_ingot'           (C 236 234 224) (C 254 252 246) (C 178 176 164)
New-Ingot 'naquadah_alloy_ingot'    (C 60 62 60)    (C 110 88 128)  (C 34 20 44)
New-Ingot 'naquadah_enriched_ingot' (C 44 92 60)    (C 92 168 108)  (C 26 56 36)
New-Ingot 'naquadria_ingot'         (C 88 56 116)   (C 150 108 188) (C 52 32 72)
New-Ingot 'nickel_ingot'            (C 198 204 182) (C 228 234 214) (C 140 146 124)
New-Ingot 'palladium_ingot'         (C 184 180 174) (C 220 216 210) (C 128 124 118)
New-Ingot 'platinum_ingot'          (C 228 228 220) (C 250 250 244) (C 168 168 158)
New-Ingot 'rhodium_ingot'           (C 214 220 226) (C 244 248 252) (C 152 158 166)
New-Ingot 'ruthenium_ingot'         (C 168 178 190) (C 206 214 224) (C 112 122 134)
New-Ingot 'special_steel_ingot'     (C 130 144 166) (C 176 192 214) (C 84 96 116)
New-Ingot 'super_alloy_ingot'       (C 152 122 186) (C 202 180 228) (C 100 74 132)
New-Ingot 'titanium_ingot'          (C 192 200 208) (C 224 232 240) (C 130 140 152)
New-Ingot 'trinium_ingot'           (C 214 206 162) (C 244 238 206) (C 158 150 108)
New-Ingot 'zinc_ingot'                (C 186 200 206) (C 224 238 242) (C 126 140 148)
New-Ingot 'extra_super_duralumin_ingot' (C 206 200 182) (C 240 236 218) (C 146 140 122)
New-Ingot 'transdimensional_metal'  (C 176 110 210) (C 226 176 250) (C 108 58 140)
New-Ingot 'transdimensional_alloy'  (C 132 84 196)  (C 196 150 246) (C 78 42 126)
New-Ingot 'neutronium'              (C 232 232 236) (C 255 255 255) (C 150 150 158)
New-Ingot 'graviton_alloy_ingot'     (C 92 178 214)  (C 168 238 255) (C 46 110 148)

# ---------------- dusts ----------------
New-Dust 'alumina'                    (C 242 242 244) (C 255 255 255)
New-Dust 'aluminum_dust'              (C 208 214 222) (C 238 242 248)
New-Dust 'ammonium_chloride'          (C 240 240 240) (C 254 254 254)
New-Dust 'antimony_dust'              (C 168 174 186) (C 206 212 222)
New-Dust 'antimony_trifluoride'       (C 214 224 220) (C 244 250 248)
New-Dust 'antimony_trioxide'          (C 236 238 242) (C 254 254 255)
New-Dust 'chromium_dust'              (C 186 198 214) (C 224 234 248)
New-Dust 'cupronickel_dust'           (C 206 160 130) (C 232 192 164)
New-Dust 'enriched_naquadah_sulfate'  (C 58 104 74)   (C 110 190 128)
New-Dust 'fine_nickel_powder'         (C 196 202 178) (C 226 232 210)
New-Dust 'inert_metal_mixture'        (C 148 142 96)  (C 186 180 130)
New-Dust 'iridium_chloride'           (C 128 140 92)  (C 166 180 126)
New-Dust 'iridium_dust'               (C 232 230 218) (C 252 250 242)
New-Dust 'iridium_metal_residue'      (C 104 104 110) (C 142 142 150)
New-Dust 'magnesium_dust'             (C 200 204 210) (C 232 236 242)
New-Dust 'naquadah_dust'              (C 46 88 58)    (C 96 176 110)
New-Dust 'naquadria_sulfate'          (C 92 60 118)   (C 150 104 186)
New-Dust 'nickel_dust'                (C 176 182 160) (C 208 214 192)
New-Dust 'osmiridium_dust'            (C 120 140 176) (C 168 188 220)
New-Dust 'osmium_tetroxide'           (C 198 214 224) (C 232 244 250)
New-Dust 'palladium_dust'             (C 176 172 166) (C 212 208 202)
New-Dust 'palladium_raw'              (C 150 146 138) (C 190 186 178)
New-Dust 'platinum_dust'              (C 224 224 214) (C 248 248 240)
New-Dust 'platinum_group_sludge'      (C 96 78 60)    (C 130 108 84)
New-Dust 'platinum_raw'               (C 226 220 200) (C 248 244 230)
New-Dust 'rarest_metal_mixture'       (C 96 88 132)   (C 134 126 176)
New-Dust 'rhodium_dust'               (C 210 216 222) (C 240 244 248)
New-Dust 'ruthenium_dust'             (C 160 170 182) (C 198 208 218)
New-Dust 'ruthenium_tetroxide'        (C 196 158 66)  (C 230 196 104)
New-Dust 'saltpeter'                  (C 240 236 222) (C 254 252 246)
New-Dust 'sodium_carbonate'           (C 236 236 230) (C 252 252 248)
New-Dust 'sodium_dichromate'          (C 228 130 40)  (C 250 170 80)
New-Dust 'special_steel_dust'         (C 116 128 148) (C 156 170 192)
New-Dust 'super_alloy_dust'           (C 158 130 190) (C 206 186 230)
New-Dust 'titanium_dust'              (C 196 200 206) (C 232 236 242)
New-Dust 'titanium_oxide'             (C 232 234 238) (C 255 255 255)
New-Dust 'trinium_sulfide'            (C 206 196 150) (C 238 230 190)
New-Dust 'zinc_dust'                    (C 180 196 204) (C 220 234 240)
New-Dust 'neutron_rich_mass'             (C 208 210 220) (C 244 246 252)
New-Dust 'stellar_ash'                   (C 118 106 94)  (C 168 152 132)
New-Dust 'extra_super_duralumin_dust'   (C 202 196 178) (C 238 234 214)
Write-Host 'done'
