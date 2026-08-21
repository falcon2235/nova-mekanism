# Regenerates the block/item tag files from the registry sources.
#
# These tags are not cosmetic: every casing uses `requiresCorrectToolForDrops()`, so a
# block missing from minecraft:mineable/pickaxe silently drops NOTHING when mined. Tags
# hand-maintained alongside 100+ blocks drift, so they are generated here instead.
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$src  = Join-Path $root 'src\main\java\com\falcon2235\moremultiblock'
$data = Join-Path $root 'src\main\resources\data'
$NS = 'mekanism_more_multiblock'

function ReadText($p){ Get-Content -LiteralPath $p -Raw }
function WriteJson($path, $obj){
  $dir = Split-Path -LiteralPath $path -Parent
  if(-not (Test-Path -LiteralPath $dir)){ New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $json = $obj | ConvertTo-Json -Depth 6
  Set-Content -LiteralPath $path -Value $json -Encoding UTF8
}

# ---- collect every registered block id ----
$registry = ReadText (Join-Path $src 'MMMRegistry.java')
$blocks = New-Object System.Collections.Generic.List[string]
foreach($m in [regex]::Matches($registry, 'registerBlock\("([a-z_0-9]+)"')){
  $id = $m.Groups[1].Value
  if($id -ne 'parallel_processor_'){ $blocks.Add($id) }
}
# parallel processors are registered from a tier array
foreach($m in [regex]::Matches($registry, 'PARALLEL_TIERS\s*=\s*\{([0-9,\s]+)\}')){
  foreach($t in ($m.Groups[1].Value -split ',')){
    $t = $t.Trim(); if($t){ $blocks.Add("parallel_processor_$t") }
  }
}
# controllers are registered as "<type id>_controller"
foreach($file in @('machine\ChemMachineType.java','MachineType.java')){
  $p = Join-Path $src $file
  if(Test-Path -LiteralPath $p){
    foreach($m in [regex]::Matches((ReadText $p), '^\s+[A-Z_]+\("([a-z_0-9]+)"', 'Multiline')){
      $blocks.Add($m.Groups[1].Value + '_controller')
    }
  }
}
$blocks = $blocks | Sort-Object -Unique
Write-Host "blocks: $($blocks.Count)"

$ids = @($blocks | ForEach-Object { "${NS}:$_" })

# ---- minecraft:mineable/pickaxe : every block we add ----
WriteJson (Join-Path $data 'minecraft\tags\blocks\mineable\pickaxe.json') @{ replace = $false; values = $ids }

# ---- minecraft:needs_iron_tool : the ores and the tougher machine casings ----
$needsIron = @($blocks | Where-Object { $_ -match '_ore$' -or $_ -match '_casing$' -or $_ -match 'neutronium' } | ForEach-Object { "${NS}:$_" })
WriteJson (Join-Path $data 'minecraft\tags\blocks\needs_iron_tool.json') @{ replace = $false; values = $needsIron }

# ---- forge ore / raw-material tags, so other mods and unifiers see our materials ----
# material => the ore block ids that yield it
$ores = @{
  titanium  = @('titanium_ore','deepslate_titanium_ore')
  magnesium = @('magnesium_ore','deepslate_magnesium_ore')
  nickel    = @('nickel_ore','deepslate_nickel_ore')
  chromium  = @('chromium_ore','deepslate_chromium_ore')
  bauxite   = @('bauxite_ore','deepslate_bauxite_ore')
  zinc      = @('zinc_ore','deepslate_zinc_ore')
  cooperite = @('cooperite_ore','deepslate_cooperite_ore')
  saltpeter = @('saltpeter_ore','deepslate_saltpeter_ore')
  antimony  = @('antimony_ore','deepslate_antimony_ore')
  naquadah  = @('naquadah_ore','deepslate_naquadah_ore')
}
$allOreBlocks = New-Object System.Collections.Generic.List[string]
foreach($mat in $ores.Keys){
  $vals = @($ores[$mat] | Where-Object { $blocks -contains $_ } | ForEach-Object { "${NS}:$_" })
  if($vals.Count -eq 0){ continue }
  foreach($v in $vals){ $allOreBlocks.Add($v) }
  WriteJson (Join-Path $data "forge\tags\blocks\ores\$mat.json") @{ replace = $false; values = $vals }
  WriteJson (Join-Path $data "forge\tags\items\ores\$mat.json")  @{ replace = $false; values = $vals }
}
# umbrella forge:ores tag
WriteJson (Join-Path $data 'forge\tags\blocks\ores.json') @{ replace = $false; values = @($allOreBlocks | Sort-Object -Unique) }
WriteJson (Join-Path $data 'forge\tags\items\ores.json')  @{ replace = $false; values = @($allOreBlocks | Sort-Object -Unique) }

# ---- forge:raw_materials/* for the raw drops ----
$raws = @('titanium','magnesium','nickel','chromium','bauxite','zinc','cooperite','antimony','naquadah')
$allRaw = New-Object System.Collections.Generic.List[string]
foreach($mat in $raws){
  $id = "${NS}:raw_$mat"
  $allRaw.Add($id)
  WriteJson (Join-Path $data "forge\tags\items\raw_materials\$mat.json") @{ replace = $false; values = @($id) }
}
WriteJson (Join-Path $data 'forge\tags\items\raw_materials.json') @{ replace = $false; values = @($allRaw) }

Write-Host 'tags regenerated'
