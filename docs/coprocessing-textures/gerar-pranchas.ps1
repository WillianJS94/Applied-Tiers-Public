$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Push-Location -LiteralPath $PSScriptRoot
try {
$manifest=Get-Content -Raw fontes.json | ConvertFrom-Json
foreach($f in $manifest.arquivos){
 $bytes=[IO.File]::ReadAllBytes((Join-Path $PWD ('fontes/'+$f.arquivo)))
 $prefix=[Text.Encoding]::ASCII.GetBytes("blob $($bytes.Length)"+[char]0)
 $hash=[Convert]::ToHexString([Security.Cryptography.SHA1]::HashData([byte[]]($prefix+$bytes))).ToLowerInvariant()
 if($hash -ne $f.git_blob){throw "Fonte incorreta: $($f.arquivo)"}
}
function Color($h){[Drawing.ColorTranslator]::FromHtml('#'+$h)}
function Read($n){[Drawing.Bitmap]::new((Join-Path $PWD "fontes/$n.png"))}
function Save($im,$p){New-Item -ItemType Directory -Force (Split-Path $p) | Out-Null;$im.Save((Join-Path $PWD $p),[Drawing.Imaging.ImageFormat]::Png)}
$tiers=@('Advanced','Elite','Ultimate')
$refs=@('7690DE','E59A57','D96DB8')
$ramps=@(@('5568A0','7690DE','A6B7EA'),@('A56F3F','E59A57','EEBD92'),@('9C4E84','D96DB8','E6A0D1'))
$original=Read 'accelerator'
$light=Read 'accelerator_light'
$report=[Collections.Generic.List[object]]::new()
function Recolor($src,$mapping,$path,$maskName){
 $dest=[Drawing.Bitmap]::new(16,16)
 $mask=[Drawing.Bitmap]::new(16,16)
 $changed=0
 foreach($y in 0..15){foreach($x in 0..15){
  $p=$src.GetPixel($x,$y);$q=$p
  $rgb=$p.Name.PadLeft(8,'0').Substring(2)
  if($p.A -gt 0 -and $mapping.ContainsKey($rgb)){
   $v=Color $mapping[$rgb];$q=[Drawing.Color]::FromArgb($p.A,$v.R,$v.G,$v.B)
   $mask.SetPixel($x,$y,[Drawing.Color]::White);$changed++
  }
  $dest.SetPixel($x,$y,$q)
  if($dest.GetPixel($x,$y).A -ne $p.A -or $dest.GetPixel($x,$y).ToArgb() -ne $q.ToArgb()){throw 'Pixel inválido'}
 }}
 Save $dest $path
 Save $mask "mascaras/$maskName.png"
 $round=[Drawing.Bitmap]::new((Join-Path $PWD $path))
 if($round.Width -ne $src.Width -or $round.Height -ne $src.Height){throw 'Dimensão alterada'}
 foreach($y in 0..15){foreach($x in 0..15){
  $a=$src.GetPixel($x,$y);$b=$round.GetPixel($x,$y)
  if($a.A -ne $b.A -or ($mask.GetPixel($x,$y).A -eq 0 -and $a.ToArgb() -ne $b.ToArgb()) -or $b.ToArgb() -ne $dest.GetPixel($x,$y).ToArgb()){throw 'Máscara ou alfa incorretos'}
 }}
 $round.Dispose()
 $report.Add([ordered]@{arquivo=$path;resolucao='16x16';alfa='idêntico';pixels_recoloridos=$changed;sha256=(Get-FileHash $path).Hash;mapa=$mapping})
 $dest
}
$bases=@{Original=$original}
$lightsA=@{Original=$light}
$lightsB=@{Original=$light}
for($i=0;$i -lt 3;$i++){
 $t=$tiers[$i];$r=$ramps[$i]
 $map=@{'915dcd'=$r[0];'b06fdd'=$r[1];'e291e3'=$r[2]}
 $bases[$t]=Recolor $original $map "variantes/A/$t/accelerator.png" 'nucleo'
 New-Item -ItemType Directory -Force "variantes/B/$t" | Out-Null
 Copy-Item "variantes/A/$t/accelerator.png" "variantes/B/$t/accelerator.png"
 Copy-Item 'fontes/accelerator_light.png' "variantes/A/$t/accelerator_light.png"
 $lightsA[$t]=$light
 $lightsB[$t]=Recolor $light $map "variantes/B/$t/accelerator_light.png" 'luz'
 if((Get-FileHash "variantes/A/$t/accelerator_light.png").Hash -ne (Get-FileHash 'fontes/accelerator_light.png').Hash -or (Get-FileHash "variantes/A/$t/accelerator.png").Hash -ne (Get-FileHash "variantes/B/$t/accelerator.png").Hash){throw 'Diferença indevida A/B'}
}
if(@($report|Where-Object {$_.arquivo -like '*/accelerator.png' -and $_.pixels_recoloridos -ne 100}).Count -or @($report|Where-Object {$_.arquivo -like '*/accelerator_light.png' -and $_.pixels_recoloridos -ne 44}).Count){throw 'Máscara incorreta'}
$lightbase=Read 'light_base';$unitbase=Read 'unit_base';$monitorbase=Read 'monitor_base'
$ringC=Read 'ring_corner';$ringH=Read 'ring_side_hor';$ringV=Read 'ring_side_ver'
$storageLights=@{};foreach($k in '1k','4k','16k','64k','256k'){$storageLights[$k]=Read "$($k)_storage_light"}
$monitorLayers=@((Read 'monitor_light_bright'),(Read 'monitor_light_medium'),(Read 'monitor_light_dark'))
function Shade($p,$factor){[Drawing.Color]::FromArgb($p.A,[int]($p.R*$factor),[int]($p.G*$factor),[int]($p.B*$factor))}
function Formed($kind,$option,$powered,$left,$right,$top,$bottom){
 # Front face: CraftingCubeModel uses a three-pixel perimeter only on edges without neighbors.
 # Both states contain the light layer; only its emissivity changes, using a fixed illustrative ambient factor.
 $b=[Drawing.Bitmap]::new(16,16);$base=$lightbase;$layers=@()
 if($kind -eq 'Unit'){$base=$unitbase}
 elseif($kind -eq 'Monitor'){$base=$monitorbase;$layers=$monitorLayers}
 elseif($storageLights.ContainsKey($kind)){$layers=@($storageLights[$kind])}
 elseif($option -eq 'A'){$layers=@($lightsA[$kind])}
 else{$layers=@($lightsB[$kind])}
 foreach($y in 0..15){foreach($x in 0..15){
  $outerX=(!$left -and $x -lt 3) -or (!$right -and $x -ge 13)
  $outerY=(!$top -and $y -lt 3) -or (!$bottom -and $y -ge 13)
  $p=$base.GetPixel($x,$y)
  if($outerX -and $outerY){$p=$ringC.GetPixel($x,$y)}
  elseif($outerX){$p=$ringV.GetPixel($x,$y)}
  elseif($outerY){$p=$ringH.GetPixel($x,$y)}
  $p=Shade $p 0.64
  if(!$outerX -and !$outerY){
   foreach($layer in $layers){$q=$layer.GetPixel($x,$y);if($q.A -gt 0){if($powered){$p=$q}else{$p=Shade $q 0.64}}}
  }
  $b.SetPixel($x,$y,$p)
 }}
 $b
}
function Cpu($grid,$option,$on){
 $h=$grid.Count;$w=$grid[0].Count
 $b=[Drawing.Bitmap]::new([int]($w*16),[int]($h*16))
 for($r=0;$r -lt $h;$r++){for($c=0;$c -lt $w;$c++){
  $face=Formed $grid[$r][$c] $option $on ($c -gt 0) ($c -lt $w-1) ($r -gt 0) ($r -lt $h-1)
  foreach($y in 0..15){foreach($x in 0..15){$b.SetPixel($c*16+$x,$r*16+$y,$face.GetPixel($x,$y))}}
  $face.Dispose()
 }}
 $b
}
# Save standalone model-equivalent face previews (not distributable block textures).
foreach($o in 'A','B'){foreach($t in $tiers){foreach($on in $false,$true){
 $state=if($on){'energizado'}else{'sem-energia'}
 Save (Formed $t $o $on $false $false $false $false) "previas/$o/$t/$state.png"
}}}
function Box($x,$y,$w,$h,$hex){$b=[Drawing.SolidBrush]::new((Color $hex));$g.FillRectangle($b,$x,$y,$w,$h);$b.Dispose()}
function Text($label,$x,$y,$size,$hex,[switch]$Bold){$style=if($Bold){[Drawing.FontStyle]::Bold}else{[Drawing.FontStyle]::Regular};$font=[Drawing.Font]::new('Segoe UI',$size,$style,[Drawing.GraphicsUnit]::Pixel);$brush=[Drawing.SolidBrush]::new((Color $hex));$g.DrawString($label,$font,$brush,[float]$x,[float]$y);$brush.Dispose();$font.Dispose()}
function Sprite($im,$x,$y,$scale){
 $zoom=[Drawing.Bitmap]::new([int]($im.Width*$scale),[int]($im.Height*$scale))
 foreach($sy in 0..($im.Height-1)){foreach($sx in 0..($im.Width-1)){ $p=$im.GetPixel($sx,$sy);foreach($dy in 0..($scale-1)){foreach($dx in 0..($scale-1)){$zoom.SetPixel($sx*$scale+$dx,$sy*$scale+$dy,$p)}}}}
 $g.DrawImageUnscaled($zoom,$x,$y);$zoom.Dispose()
}
function Item($im,$x,$y){
 # Orthographic three-face preview of the actual cube_all material, not a Minecraft screenshot.
 # Integer nearest-neighbor texture sampling through the affine face transform.
 $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
 $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 $dark=[Drawing.Bitmap]::new(16,16)
 foreach($sy in 0..15){foreach($sx in 0..15){$dark.SetPixel($sx,$sy,(Shade $im.GetPixel($sx,$sy) 0.78))}}
 $g.DrawImage($im,[Drawing.Point[]]@([Drawing.Point]::new($x+56,$y),[Drawing.Point]::new($x+112,$y+28),[Drawing.Point]::new($x,$y+28)))
 $g.DrawImage($im,[Drawing.Point[]]@([Drawing.Point]::new($x,$y+28),[Drawing.Point]::new($x+56,$y+56),[Drawing.Point]::new($x,$y+92)))
 $g.DrawImage($dark,[Drawing.Point[]]@([Drawing.Point]::new($x+56,$y+56),[Drawing.Point]::new($x+112,$y+28),[Drawing.Point]::new($x+56,$y+120)))
 $dark.Dispose()
}
$canvas=[Drawing.Bitmap]::new(2200,1440);$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear((Color 'F0F2F7'));$g.TextRenderingHint='AntiAliasGridFit'
Box 0 0 2200 150 '222638'
Text 'APPLIED TIERS · COPROCESSADORES' 45 24 52 'FFFFFF' -Bold
Text 'Estudo visual · AE2 v26.1.14-beta · paleta Espectral' 48 94 29 'CFD8F1'
$cols=@('Item · prévia 3D','Bloco individual','CPU sem energia','CPU com energia')
foreach($i in 0..3){Text $cols[$i] (370+450*$i) 176 28 '222638' -Bold}
foreach($o in 'A','B'){
 $start=if($o -eq 'A'){225}else{785}
 Text $(if($o -eq 'A'){'A · Luz original AE2'}else{'B · Luz Espectral por tier'}) 45 $start 32 '222638' -Bold
 for($i=0;$i -lt 3;$i++){
  $t=$tiers[$i];$y=$start+55+160*$i
  Box 45 $y 2105 148 'FFFFFF'
  Text $t 65 ($y+20) 28 '222638' -Bold
  Box 68 ($y+64) 25 18 $refs[$i];Text ('#'+$refs[$i]+' · '+($i+2)+' unidades') 103 ($y+58) 22 '495477'
  Item $bases[$t] 442 ($y+14)
  Sprite $bases[$t] 891 ($y+18) 7
  Sprite (Formed $t $o $false $false $false $false $false) 1341 ($y+18) 7
  Sprite (Formed $t $o $true $false $false $false $false) 1791 ($y+18) 7
 }
}
Text 'Prévia de faces do modelo, não captura do jogo. A camada luminosa existe nos dois estados da CPU formada.' 48 1373 25 '495477'
Save $canvas './prancha-principal.png';$g.Dispose();$canvas.Dispose()
$cases=@(
 @{label='Coprocessadores originais';grid=@(@('4k','Original','Original'),@('Original','Original','Original'))},
 @{label='Advanced';grid=@(@('4k','Advanced','Advanced'),@('Advanced','Advanced','Advanced'))},
 @{label='Elite';grid=@(@('4k','Elite','Elite'),@('Elite','Elite','Elite'))},
 @{label='Ultimate';grid=@(@('4k','Ultimate','Ultimate'),@('Ultimate','Ultimate','Ultimate'))},
 @{label='Três tiers';grid=@(@('Advanced','Elite','Ultimate'),@('4k','Original','Ultimate'))},
 @{label='Storages variados';grid=@(@('1k','4k','16k','64k','256k'),@('Advanced','Elite','Ultimate','Advanced','Ultimate'))},
 @{label='Units e Monitors';grid=@(@('Unit','4k','Monitor','Unit'),@('Advanced','Elite','Ultimate','Original'))},
 @{label='CPU maior';grid=@(@('4k','Elite','Elite','Elite','Elite'),@('Monitor','Elite','Elite','Elite','Elite'),@('Unit','Elite','Elite','Elite','Elite'))}
)
$canvas=[Drawing.Bitmap]::new(2340,1940);$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear((Color 'F0F2F7'));$g.TextRenderingHint='AntiAliasGridFit'
Box 0 0 2340 150 '222638';Text 'CRAFTING CPU · COMPOSIÇÕES' 45 22 51 'FFFFFF' -Bold
Text 'Molduras conectadas: somente nas bordas externas de cada face da CPU formada.' 48 95 28 'CFD8F1'
$labels=@('A · Sem energia','A · Energizada','B · Sem energia','B · Energizada')
foreach($i in 0..3){Text $labels[$i] (377+490*$i) 175 30 '222638' -Bold}
for($r=0;$r -lt $cases.Count;$r++){
 $y=230+205*$r;$case=$cases[$r];Box 45 $y 2250 187 'FFFFFF'
 Text $case.label 63 ($y+58) 24 '222638' -Bold
 if($r -le 3){Text '4k no canto superior esquerdo' 63 ($y+94) 17 '495477'}
 if($r -eq 4){Text 'Advanced · Elite · Ultimate' 63 ($y+94) 17 '495477';Text '4k · Original · Ultimate' 63 ($y+117) 17 '495477'}
 if($r -eq 5){Text '1k · 4k · 16k · 64k · 256k' 63 ($y+94) 17 '495477'}
 foreach($i in 0..3){$o=if($i -lt 2){'A'}else{'B'};$on=($i%2 -eq 1);$im=Cpu $case.grid $o $on;$scale=if($im.Height -gt 32){3}else{4};Sprite $im (385+490*$i+[int]((430-$im.Width*$scale)/2)) ($y+([int]((187-$im.Height*$scale)/2))) $scale;$im.Dispose()}
}
Text 'Simulação frontal 2D. Iluminação ambiente fixa para comparar emissividade; Monitor representado em branco neutro.' 48 1884 24 '495477'
Save $canvas './prancha-contextual.png';$g.Dispose();$canvas.Dispose()
[ordered]@{fonte_verificada=$manifest.commit;arquivos=$report;opcao_A_luz='byte a byte original';opcao_B_luz='44 pixels opacos; alfa e RGB transparentes preservados';nucleo='100 pixels';moldura='arquivos originais intactos';estados='camada luminosa presente tanto sem energia quanto energizada';ambient=0.64} | ConvertTo-Json -Depth 8 | Set-Content validacao.json -Encoding utf8
}finally{Pop-Location}


