param(
    [string]$ComfyUrl = "http://127.0.0.1:8188",
    [string]$WorkspaceRoot = ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))),
    [int]$TimeoutMinutes = 20
)

$ErrorActionPreference = "Stop"

$comfyRoot = Join-Path $WorkspaceRoot "tools\image\ComfyUI"
$inputRoot = Join-Path $comfyRoot "input"
$outputRoot = Join-Path $comfyRoot "output"
$targetRoot = Join-Path $WorkspaceRoot "target\experiments\local-ai\aviadores-storyboards-v2"
$rawRoot = Join-Path $targetRoot "raw"
$ffmpeg = Join-Path $WorkspaceRoot "tools\ffmpeg\bin\ffmpeg.exe"

New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null
New-Item -ItemType Directory -Path $rawRoot -Force | Out-Null
if (-not (Test-Path -LiteralPath $ffmpeg)) {
    throw "No existe FFmpeg administrado: $ffmpeg"
}

$sources = @{
    "local-sb-camera-close.png" = Join-Path $WorkspaceRoot "src\main\resources\images\theatre\cameras\CERCA_CENTRO_NIVEL.png"
    "local-sb-camera-wide.png" = Join-Path $WorkspaceRoot "src\main\resources\images\theatre\cameras\PANORAMICA_CENTRO_NIVEL.png"
    "local-sb-capitan.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\capitan_bigote\capitan_bigote_01_frontal.png"
    "local-sb-teniente.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\teniente_tornillo\teniente_tornillo_01_frontal.png"
    "local-sb-avion.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\utileria\avion_tornillo_dorado_01.png"
    "local-sb-mapa.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\utileria\obj_mapa_01.png"
    "local-sb-vaca.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\utileria\animal_vaca_01.png"
}

foreach ($entry in $sources.GetEnumerator()) {
    if (-not (Test-Path -LiteralPath $entry.Value)) {
        throw "No existe la referencia local: $($entry.Value)"
    }
    Copy-Item -LiteralPath $entry.Value -Destination (Join-Path $inputRoot $entry.Key) -Force
}

$negative = @"
color, colored drawing, sepia, photorealistic, photograph, glossy render, oil painting,
finished illustration, dense crosshatching, excessive detail, text, letters, caption,
speech bubble, arrows, labels, watermark, logo, malformed anatomy, extra limbs,
duplicate protagonist, cloned person, six men, character lineup, character sheet,
isolated product, blank background, fashion reference sheet, swapped clothing,
modern airplane, audience seating visible
"@

$specs = @(
    @{
        Id = "fragmento_01_escena1_hangar_presentacion"
        Seed = 24101
        Camera = "local-sb-camera-close.png"
        Denoise = 0.88
        Prompt = @"
rough professional storyboard thumbnail, black graphite pencil on white paper,
very sparse confident contour lines, simple block shapes, minimal pale grey shading,
unfinished production sketch, frontal theatre stage viewed from audience center at eye level,
proscenium curtains visibly frame the composition, old rural aircraft hangar stage set at dawn,
the vintage cream fabric and brass Tornillo Dorado biplane is the only subject,
biplane centered in a clear wide establishing composition, propeller and radial engine readable,
a few simple crates, workbench, stool and lantern at stage edges, low horizon on painted backdrop,
no people, no audience, practical editable storyboard made with few strokes, 16:9
"@
    },
    @{
        Id = "fragmento_04_dignidad_mantenimiento"
        Seed = 24104
        Camera = "local-sb-camera-close.png"
        Denoise = 0.86
        Prompt = @"
rough professional storyboard thumbnail, monochrome black graphite on white paper,
economical gesture drawing, simplified faces and hands, minimal grey blocks,
frontal theatre stage seen from audience center at eye level, proscenium curtains visible,
old aircraft hangar stage set with the Tornillo Dorado biplane behind the actors,
medium wide two-shot with exactly two aviator characters,
Capitan Bigote on audience-left: mature tall veteran, large curled moustache,
aviator cap and goggles, scarf, shearling leather jacket, trousers and tall boots,
arms folded and chin raised with proud dismissive authority,
Teniente Tornillo on audience-right: younger slim pilot, small moustache,
aviator cap and goggles, scarf, leather jacket, waistcoat, trousers and lace-up boots,
earnestly presents one rusty screw with a tiny flower and gestures in puzzled disagreement,
strong separated silhouettes, readable eyelines and comic hierarchy, 16:9
"@
    },
    @{
        Id = "fragmento_09_mapa_norte_abajo"
        Seed = 24109
        Camera = "local-sb-camera-wide.png"
        Denoise = 0.90
        Prompt = @"
rough theatre storyboard thumbnail, black graphite pencil on white paper,
loose economical gesture lines, minimal faces, sparse light grey shading,
unfinished and easy to revise, panoramic frontal theatre camera at audience eye level,
proscenium curtains and stage boundary remain visible,
the stage set represents the open cockpit of the Tornillo Dorado in flight,
simple wing, cockpit rim, struts, rigging and painted cloud backdrop,
exactly two aviator actors in a readable medium two-shot,
Capitan Bigote at audience-left operating the controls, mature confident veteran,
large curled moustache, aviator cap and goggles, scarf and shearling jacket silhouette,
Teniente Tornillo at audience-right, younger slim pilot with small moustache,
aviator cap and goggles, scarf and leather jacket, no prescription glasses,
he holds a large unfolded navigation map upside down and studies it with worried confusion,
the captain reacts with amused confidence, map visible without hiding both faces,
clear stage blocking, strong silhouettes, 16:9
"@
    },
    @{
        Id = "fragmento_24_cierre_tornillo_dorado"
        Seed = 24124
        Camera = "local-sb-camera-close.png"
        Denoise = 0.88
        Prompt = @"
rough professional closing storyboard thumbnail, monochrome graphite on white paper,
simple gesture figures, few facial traits, sparse scenery lines and pale grey blocks,
unfinished practical theatre planning sketch, frontal theatre stage at audience eye level,
proscenium curtains and stage frame visible,
rural landing stage set after a safe flight, intact Tornillo Dorado biplane in the background,
exactly two aviator protagonists center stage,
Capitan Bigote audience-left stands proudly hands on hips, tall mature pilot,
large curled moustache, aviator cap and goggles, scarf and shearling jacket silhouette,
Teniente Tornillo audience-right looks relieved and embarrassed, one hand on his aviator cap,
younger slim pilot with small moustache, scarf and leather jacket silhouette,
one perplexed tan cow and three hay bales form the absurd rural audience at stage-right,
one battered abandoned brimmed hat lies clearly in foreground as the final joke,
clean visual hierarchy, no villagers, no human crowd, no crash or injuries, 16:9
"@
    }
)

function New-LoadImageNode([string]$imageName) {
    return @{
        class_type = "LoadImage"
        inputs = @{ image = $imageName }
    }
}

function Invoke-StoryboardPrompt([hashtable]$spec) {
    $workflow = [ordered]@{}
    $workflow["1"] = @{
        class_type = "CheckpointLoaderSimple"
        inputs = @{ ckpt_name = "DreamShaper_8_pruned.safetensors" }
    }
    $workflow["2"] = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $spec.Prompt; clip = @("1", 1) }
    }
    $workflow["3"] = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $negative; clip = @("1", 1) }
    }
    $workflow["4"] = New-LoadImageNode $spec.Camera
    $workflow["5"] = @{
        class_type = "ImageScale"
        inputs = @{
            image = @("4", 0)
            upscale_method = "lanczos"
            width = 960
            height = 544
            crop = "center"
        }
    }
    $workflow["6"] = @{
        class_type = "VAEEncode"
        inputs = @{
            pixels = @("5", 0)
            vae = @("1", 2)
        }
    }
    $samplerNodeId = "7"
    $decodeNodeId = "8"
    $saveNodeId = "9"
    $workflow[$samplerNodeId] = @{
        class_type = "KSampler"
        inputs = @{
            model = @("1", 0)
            seed = [long]$spec.Seed
            steps = 32
            cfg = 7.5
            sampler_name = "dpmpp_2m"
            scheduler = "karras"
            positive = @("2", 0)
            negative = @("3", 0)
            latent_image = @("6", 0)
            denoise = [double]$spec.Denoise
        }
    }
    $workflow[$decodeNodeId] = @{
        class_type = "VAEDecode"
        inputs = @{ samples = @($samplerNodeId, 0); vae = @("1", 2) }
    }
    $workflow[$saveNodeId] = @{
        class_type = "SaveImage"
        inputs = @{
            images = @($decodeNodeId, 0)
            filename_prefix = "docupodcast-local-storyboards-v2/$($spec.Id)"
        }
    }

    $body = @{
        prompt = $workflow
        client_id = "docupodcast-local-storyboard-smoke"
    } | ConvertTo-Json -Depth 30

    try {
        $queued = Invoke-RestMethod -Uri "$ComfyUrl/prompt" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 60
    } catch {
        $details = $_.ErrorDetails.Message
        throw "ComfyUI rechazo el workflow $($spec.Id): $details"
    }

    $promptId = [string]$queued.prompt_id
    if ([string]::IsNullOrWhiteSpace($promptId)) {
        throw "ComfyUI no devolvio prompt_id para $($spec.Id)."
    }

    Write-Host "[$($spec.Id)] promptId=$promptId seed=$($spec.Seed)"
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $entry = $null
    do {
        Start-Sleep -Seconds 2
        $history = Invoke-RestMethod -Uri "$ComfyUrl/history/$promptId" -TimeoutSec 20
        $property = $history.PSObject.Properties[$promptId]
        if ($null -ne $property) {
            $entry = $property.Value
            break
        }
    } while ((Get-Date) -lt $deadline)

    if ($null -eq $entry) {
        throw "Timeout esperando el storyboard local $($spec.Id)."
    }
    if ($entry.status.status_str -ne "success") {
        $messages = $entry.status.messages | ConvertTo-Json -Depth 20 -Compress
        throw "Fallo el storyboard local $($spec.Id): $messages"
    }

    $images = $entry.outputs.$saveNodeId.images
    if ($null -eq $images -or $images.Count -eq 0) {
        throw "ComfyUI termino sin PNG para $($spec.Id)."
    }

    $image = $images[0]
    $relative = if ([string]::IsNullOrWhiteSpace([string]$image.subfolder)) {
        [string]$image.filename
    } else {
        Join-Path ([string]$image.subfolder) ([string]$image.filename)
    }
    $source = Join-Path $outputRoot $relative
    $rawTarget = Join-Path $rawRoot "$($spec.Id)_storyboard_local_raw.png"
    $target = Join-Path $targetRoot "$($spec.Id)_storyboard_local.png"
    Copy-Item -LiteralPath $source -Destination $rawTarget -Force
    & $ffmpeg -hide_banner -loglevel error -y -i $rawTarget -vf "hue=s=0" $target
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $target)) {
        throw "FFmpeg no pudo normalizar a escala de grises $($spec.Id)."
    }
    Write-Host "[$($spec.Id)] $target"
    return $target
}

$results = @()
foreach ($spec in $specs) {
    $results += Invoke-StoryboardPrompt $spec
}

$results
