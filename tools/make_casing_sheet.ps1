# Each machine's casing next to the port skin it is assigned, so a mismatch between
# the two is obvious. A port is built INTO the casing wall; if their palettes differ
# the wall looks broken.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$out = $args[0]
# machine label, casing texture, assigned port style
$rows = @(
 @('blast_furnace','heat_proof_casing','heat_proof'),
 @('reactor','ptfe_casing','ptfe'),
 @('distillation/mixer','stainless_casing','stainless'),
 @('alloy_blast','alloy_blast_casing','alloy'),
 @('vacuum_freezer','frost_proof_casing','frost'),
 @('circuit_assembler','assembly_casing','assembly'),
 @('electrolyzer','electrolyzer_casing','electrolyzer'),
 @('centrifuge','centrifuge_casing','centrifuge'),
 @('fusion_reactor','fusion_casing','fusion'),
 @('void_miner','void_miner_casing','hazard'),
 @('OIL_RIG','oil_rig_casing','oil_rig'),
 @('combustion_gen','engine_casing','engine'),
 @('large_inscriber','inscriber_casing','inscriber'),
 @('large_charger','charger_casing','charger'),
 @('grand_mana_pool','livingrock_casing','livingrock'),
 @('grand_elven_gate','elven_gate_casing','elven'),
 @('grand_terra_plate','terra_plate_casing','terra'),
 @('research_station','research_casing','research'),
 @('assembly_line','assline_casing','assline'),
 @('grand_imbuement','sourcestone_casing','sourcestone'),
 @('matter_replicator','replicator_casing','replicator'),
 @('transdim_fusion','transdimensional_casing','transdim'),
 @('CATALYTIC_REFORMER','reformer_casing','reformer'),
 @('star_generator','star_casing','star'),
 @('stabilizer','neutronium_casing','neutronium'),
 @('hadron_collider','accelerator_casing','accelerator'),
 @('annihilation_gen','annihilation_casing','annihilation')
)
$scale=4; $cell=16*$scale; $pad=6; $labelW=150; $head=16
$w = $labelW + 4*($cell+$pad) + $pad
$h = $head + $rows.Count*($cell+$pad) + $pad
$bmp = New-Object System.Drawing.Bitmap $w,$h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear([System.Drawing.Color]::FromArgb(255,30,30,34))
$font=New-Object System.Drawing.Font('Consolas',9)
$wh=[System.Drawing.Brushes]::White
$g.DrawString('casing',$font,$wh,$labelW,1)
$g.DrawString('energy',$font,$wh,($labelW+$cell+$pad),1)
$g.DrawString('item in',$font,$wh,($labelW+2*($cell+$pad)),1)
$g.DrawString('fluid out',$font,$wh,($labelW+3*($cell+$pad)),1)
function Draw($g,$dir,$name,$x,$y,$cell){
  $p = Join-Path $dir ($name+'.png')
  if(Test-Path -LiteralPath $p){ $i=[System.Drawing.Image]::FromFile($p); $g.DrawImage($i,$x,$y,$cell,$cell); $i.Dispose() }
}
for($r=0;$r -lt $rows.Count;$r++){
  $y=$head+$r*($cell+$pad)
  $g.DrawString($rows[$r][0],$font,$wh,2,($y+$cell/2-7))
  $style=$rows[$r][2]
  $sfx = if($style -eq 'default'){''}else{'_'+$style}
  Draw $g $dir $rows[$r][1] $labelW $y $cell
  Draw $g $dir ('energy_port'+$sfx) ($labelW+$cell+$pad) $y $cell
  Draw $g $dir ('item_input_port'+$sfx) ($labelW+2*($cell+$pad)) $y $cell
  Draw $g $dir ('fluid_output_port'+$sfx) ($labelW+3*($cell+$pad)) $y $cell
}
$g.Dispose()
$bmp.Save($out,[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Write-Host $out
