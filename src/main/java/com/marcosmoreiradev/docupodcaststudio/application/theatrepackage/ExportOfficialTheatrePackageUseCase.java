package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarV2Writer;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Symmetric theatre-v2 exporter. A directory and a ZIP contain the same bytes and manifest. */
public final class ExportOfficialTheatrePackageUseCase {
    private final com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository scripts;
    public ExportOfficialTheatrePackageUseCase(com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository scripts) {
        this.scripts = Objects.requireNonNull(scripts);
    }
    public Result execute(DocuPodcastProject project, Path projectRoot, Path destination) throws IOException {
        var script = scripts
                .load(projectRoot.resolve("project.docupodcast.json")).orElseThrow(() -> new IOException("Guarda la lectura preparada antes de exportar la carpeta teatral."));
        return execute(project, projectRoot, destination, script);
    }

    public Result execute(DocuPodcastProject project, Path projectRoot, Path destination,
                          NarrationScriptDocument script) throws IOException {
        Objects.requireNonNull(project); Objects.requireNonNull(projectRoot); Objects.requireNonNull(destination);
        if (script == null || script.empty()) throw new IOException("La exportación teatral requiere sus parlamentos, no un guion vacío.");
        for (var intervention : project.theatre().intervenciones()) {
            if (script.segments().stream().noneMatch(s -> s.id().equals(intervention.blockId()) || s.sourceBlockIds().contains(intervention.blockId())))
                throw new IOException("Intervención sin texto exportable: " + intervention.id());
            if (project.theatre().textActionPlacements().stream().noneMatch(p -> p.intervencionId().equals(intervention.id())))
                throw new IOException("Asigna una escena antes de exportar " + intervention.id());
        }
        boolean zip = destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
        Path parent = destination.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("El destino del paquete no tiene carpeta padre.");
        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, ".theatre-export-");
        try {
            LinkedHashMap<String, Binding> bindings = bindings(project, script);
            LinkedHashMap<String, String> paths = new LinkedHashMap<>();
            ArrayList<ManifestAsset> manifestAssets = new ArrayList<>();
            LinkedHashMap<String, List<Binding>> usesByAsset = new LinkedHashMap<>();
            bindings.values().forEach(b -> usesByAsset.computeIfAbsent(b.assetId, ignored -> new ArrayList<>()).add(b));
            for (List<Binding> uses : usesByAsset.values()) {
                Binding binding = uses.getFirst();
                ProjectAssetReference ref = project.assets().byId(binding.assetId)
                        .orElseThrow(() -> new IOException("Referencia de asset teatral inexistente: " + binding.assetId));
                Path source = projectRoot.toAbsolutePath().normalize().resolve(ref.relativePath()).normalize();
                if (!source.startsWith(projectRoot.toAbsolutePath().normalize()) || !Files.isRegularFile(source))
                    throw new IOException("Asset teatral configurado pero faltante: " + ref.relativePath());
                String extension = extension(source.getFileName().toString());
                String relative = "assets/" + binding.folder + "/" + safe(binding.logicalId) + extension;
                Path target = staging.resolve(relative); Files.createDirectories(target.getParent()); Files.copy(source, target);
                String hash = sha256(target); long size = Files.size(target);
                paths.put(binding.assetId, relative);
                manifestAssets.add(new ManifestAsset(relative, binding.logicalId, binding.kind, hash, size, binding.metadata,
                        uses.stream().skip(1).toList()));
            }
            Files.writeString(staging.resolve("obra.teatro.md"), new TheatreGrammarV2Writer().write(project, paths, script), StandardCharsets.UTF_8);
            String packageId = safe(project.metadata().title()).toLowerCase(Locale.ROOT);
            Files.writeString(staging.resolve("docupodcast-theatre.json"), manifest(packageId, manifestAssets, project.viewState().getOrDefault("theatre.presentationMode", "fragments")), StandardCharsets.UTF_8);
            if (Files.exists(destination)) throw new IOException("El destino ya existe: " + destination);
            if (zip) { zip(staging, destination); deleteTree(staging); }
            else Files.move(staging, destination);
            return new Result(destination, manifestAssets.size());
        } catch (Exception ex) {
            deleteTree(staging);
            if (ex instanceof IOException io) throw io;
            throw new IOException("No se pudo exportar el paquete teatral.", ex);
        }
    }

    private static LinkedHashMap<String, Binding> bindings(DocuPodcastProject project, NarrationScriptDocument script) {
        TheatreProjectLayer t = project.theatre(); LinkedHashMap<String, Binding> result = new LinkedHashMap<>();
        t.characterImages().forEach(v -> put(result, new Binding(v.assetId(), "personajes", assetLogical(v.assetId()), TheatrePackageAssetKind.CHARACTER_IMAGE,
                metadata("characterId", v.characterId(), "sceneId", v.sceneId(), "view", v.view()))));
        t.objectImages().forEach(v -> put(result, new Binding(v.assetId(), "objetos", assetLogical(v.assetId()), TheatrePackageAssetKind.OBJECT_IMAGE,
                metadata("objectId", v.objectId(), "sceneId", v.sceneId(), "view", v.view()))));
        t.stageBackdrops().stream().sorted(Comparator.comparing((TheatreProjectLayer.StageBackdrop v) ->
                t.stageBackdropAssignments().stream().noneMatch(a -> a.backdropId().equals(v.id())))).forEach(v -> {
            var assignment = t.stageBackdropAssignments().stream().filter(a -> a.backdropId().equals(v.id())).findFirst().orElse(null);
            Map<String,String> meta = assignment == null ? metadata("backdropId", v.id(), "displayName", v.displayName())
                    : metadata("backdropId", v.id(), "displayName", v.displayName(), "scope", assignment.scope(), "scopeId", assignment.scopeId());
            put(result, new Binding(v.assetId(), "fondos", assetLogical(v.assetId()), TheatrePackageAssetKind.BACKDROP, meta));
        });
        t.scenes().stream().filter(v -> !v.spatialMapAssetId().isBlank()).forEach(v -> put(result, new Binding(v.spatialMapAssetId(), "mapas", assetLogical(v.spatialMapAssetId()), TheatrePackageAssetKind.SPATIAL_MAP, metadata("sceneId", v.id()))));
        t.intervencionesVisuales().forEach(v -> put(result, new Binding(v.assetId(), "frames", assetLogical(v.assetId()), TheatrePackageAssetKind.INTERVENTION_IMAGE, metadata("interventionId", v.intervencionId()))));
        t.intermediateFrames().forEach(v -> put(result, new Binding(v.assetId(), "frames_intermedios", assetLogical(v.assetId()), TheatrePackageAssetKind.INTERMEDIATE_FRAME,
                metadata("fromInterventionId", v.fromIntervencionId(), "toInterventionId", v.toIntervencionId()))));
        project.narrativeLayerAssignments().stream().filter(v -> v.kind() == NarrativeLayerKind.HUMAN_AUDIO).forEach(v -> {
            String intervention = t.intervenciones().stream().filter(i -> i.blockId().equals(v.textRange().segmentId())
                    || (script != null && script.segments().stream().anyMatch(s -> s.id().equals(v.textRange().segmentId()) && s.sourceBlockIds().contains(i.blockId()))))
                    .map(TheatreProjectLayer.Intervencion::id).findFirst().orElse("");
            if (!intervention.isBlank()) {
                int end = script == null ? v.textRange().endOffset() : script.segmentById(v.textRange().segmentId())
                        .map(s -> Math.min(v.textRange().endOffset(), s.narrationText().length())).orElse(v.textRange().endOffset());
                put(result, new Binding(v.targetId(), "audio", assetLogical(v.targetId()), TheatrePackageAssetKind.HUMAN_AUDIO,
                        metadata("interventionId", intervention, "textStart", Integer.toString(v.textRange().startOffset()), "textEnd", Integer.toString(end))));
            }
        });
        t.audioTracks().forEach(track -> {
            String anchor = track.startIntervencionId();
            if (anchor.isBlank() && script != null) anchor = t.intervenciones().stream()
                    .filter(i -> script.segments().stream().anyMatch(s -> s.id().equals(track.startSegmentId()) && s.sourceBlockIds().contains(i.blockId())))
                    .map(i -> i.id()).findFirst().orElse("");
            if (anchor.isBlank()) throw new IllegalArgumentException("Pista sin intervención exportable: " + track.id());
            put(result, new Binding(track.assetId(), "audio", assetLogical(track.assetId()), TheatrePackageAssetKind.HUMAN_AUDIO,
                    metadata("trackId", track.id(), "interventionId", anchor,
                            "sourceStartSeconds", Double.toString(track.sourceStartSeconds()), "sourceEndSeconds", Double.toString(track.sourceEndSeconds()),
                            "endMode", track.endMode().name(), "volume", Double.toString(track.volume()),
                            "sourceDurationSeconds", Double.toString(track.sourceDurationSeconds()), "gentleFade", Boolean.toString(track.gentleFade()))));
        });
        project.voiceLibrary().referenceSampleSets().forEach(set -> set.samples().stream()
                .filter(sample -> sample.ownership() == com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership.PROJECT_ASSET)
                .forEach(sample -> {
                    var asset = project.assets().references().stream().filter(a -> a.relativePath().equals(sample.fileUri())).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Muestra sin asset portable: " + sample.id()));
                    put(result, new Binding(asset.id(), "voces", assetLogical(asset.id()), TheatrePackageAssetKind.VOICE_SAMPLE,
                            metadata("voiceProfileId", sample.voiceProfileId(), "sampleId", sample.id(), "tone", sample.tone().name(),
                                    "durationMillis", Long.toString(sample.durationMillis()), "notes", sample.notes(), "referenceTranscript", sample.referenceTranscript())));
                }));
        return result;
    }
    private static void put(Map<String, Binding> result, Binding binding) {
        result.putIfAbsent(binding.assetId + "|" + binding.kind + "|" + new TreeMap<>(binding.metadata), binding);
    }
    private static Map<String,String> metadata(String... values) { LinkedHashMap<String,String> result = new LinkedHashMap<>(); for (int i=0;i+1<values.length;i+=2) if (!values[i+1].isBlank()) result.put(values[i], values[i+1]); return result; }
    private static String manifest(String packageId, List<ManifestAsset> assets, String mode) {
        StringBuilder out = new StringBuilder("{\n  \"schemaVersion\": 2,\n  \"grammarVersion\": \"theatre-v2\",\n  \"grammar\": \"obra.teatro.md\",\n  \"packageId\": \"").append(json(packageId)).append("\",\n  \"packageVersion\": \"1.0.0\",\n  \"assets\": [");
        out.insert(2, "  \"presentationMode\": \"" + json(com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry.normalizeFrameMode(mode)) + "\",\n");
        for (int i=0;i<assets.size();i++) {
            ManifestAsset a=assets.get(i); if(i>0) out.append(',');
            out.append("\n    {\"path\":\"").append(json(a.path)).append("\",\"logicalId\":\"").append(json(a.logicalId)).append("\",\"kind\":\"").append(a.kind).append("\",\"sha256\":\"").append(a.hash).append("\",\"size\":").append(a.size);
            a.metadata.forEach((k,v)->out.append(",\"").append(json(k)).append("\":\"").append(json(v)).append("\""));
            if (!a.additionalBindings.isEmpty()) {
                out.append(",\"bindings\":[");
                for (int j=0;j<a.additionalBindings.size();j++) {
                    if(j>0) out.append(','); Binding b=a.additionalBindings.get(j);
                    out.append("{\"kind\":\"").append(b.kind).append('"');
                    b.metadata.forEach((k,v)->out.append(",\"").append(json(k)).append("\":\"").append(json(v)).append("\""));
                    out.append('}');
                }
                out.append(']');
            }
            out.append('}');
        }
        return out.append("\n  ]\n}\n").toString();
    }
    private static void zip(Path root, Path target) throws IOException { try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(target))){ try(var paths=Files.walk(root)){ for(Path file:paths.filter(Files::isRegularFile).sorted().toList()){ String rel=root.relativize(file).toString().replace('\\','/'); out.putNextEntry(new ZipEntry(rel)); Files.copy(file,out); out.closeEntry(); } } } }
    private static String sha256(Path file) throws IOException { try { MessageDigest d=MessageDigest.getInstance("SHA-256"); try(InputStream in=Files.newInputStream(file)){ byte[] b=new byte[65536]; for(int n;(n=in.read(b))>=0;)if(n>0)d.update(b,0,n); } return HexFormat.of().formatHex(d.digest()); } catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    private static String extension(String name){int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot).toLowerCase(Locale.ROOT);}
    private static String safe(String value){String s=value==null?"":value.replaceAll("[^A-Za-z0-9_-]","-").replaceAll("-+","-").replaceAll("^-|-$","");return s.isBlank()?"theatre":s;}
    private static String fallback(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
    private static String assetLogical(String assetId){return assetId != null && assetId.startsWith("THEATRE-") ? assetId.substring("THEATRE-".length()) : safe(assetId);}
    private static String json(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
    private static void deleteTree(Path root){try{if(root!=null&&Files.exists(root))try(var p=Files.walk(root)){for(Path f:p.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}catch(IOException ignored){}}
    private record Binding(String assetId,String folder,String logicalId,TheatrePackageAssetKind kind,Map<String,String> metadata){}
    private record ManifestAsset(String path,String logicalId,TheatrePackageAssetKind kind,String hash,long size,Map<String,String> metadata,List<Binding> additionalBindings){}
    public record Result(Path destination,int assetCount){}
}
