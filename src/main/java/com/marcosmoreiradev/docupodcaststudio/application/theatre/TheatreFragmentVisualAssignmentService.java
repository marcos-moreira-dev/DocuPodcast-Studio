package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Assigns the ordered demo fragment images to the document visual rail. */
public final class TheatreFragmentVisualAssignmentService {
    private static final Pattern FRAGMENT_FILE = Pattern.compile("fragmento_(\\d{1,3})_");

    public List<NarrativeLayerAssignment> assign(
            NarrationScriptDocument script,
            ProjectAssetCatalog assets,
            List<NarrativeLayerAssignment> fallbackImages) {
        if (script == null || script.empty() || assets == null) {
            return fallbackImages == null ? List.of() : List.copyOf(fallbackImages);
        }
        List<NarrationSegment> segments = script.segments().stream()
                .filter(NarrationSegment::narratable)
                .toList();
        Map<String, NarrativeLayerAssignment> bySegment = new LinkedHashMap<>();
        for (OrderedAsset ordered : orderedFragmentAssets(assets)) {
            int segmentIndex = ordered.order() - 1;
            if (segmentIndex < 0 || segmentIndex >= segments.size()) {
                continue;
            }
            NarrationSegment segment = segments.get(segmentIndex);
            bySegment.put(segment.id(), new NarrativeLayerAssignment(
                    "NLA-IMAGE-FRAGMENT-" + segment.id(),
                    NarrativeLayerKind.IMAGE,
                    new ScriptTextRange(segment.id(), 0, segment.narrationText().length()),
                    ordered.asset().id(),
                    "Fragmento visual " + ordered.order(),
                    "Visual del demo asignado por orden de archivo."));
        }
        for (NarrativeLayerAssignment fallback : fallbackImages == null ? List.<NarrativeLayerAssignment>of() : fallbackImages) {
            if (fallback != null && fallback.kind() == NarrativeLayerKind.IMAGE) {
                bySegment.putIfAbsent(fallback.textRange().segmentId(), fallback);
            }
        }
        return List.copyOf(bySegment.values());
    }

    private static List<OrderedAsset> orderedFragmentAssets(ProjectAssetCatalog assets) {
        ArrayList<OrderedAsset> result = new ArrayList<>();
        for (ProjectAssetReference asset : assets.byKind(ProjectAssetKind.IMAGE)) {
            OptionalInt order = visualOrder(asset);
            if (order.isPresent()) {
                result.add(new OrderedAsset(order.getAsInt(), asset));
            }
        }
        result.sort(Comparator.comparingInt(OrderedAsset::order));
        return List.copyOf(result);
    }

    private static OptionalInt visualOrder(ProjectAssetReference asset) {
        String name = visualName(asset);
        if (name.contains("imagen_01_presentacion_personajes")) {
            return OptionalInt.of(3);
        }
        Matcher matcher = FRAGMENT_FILE.matcher(name);
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        String number = matcher.group(1);
        if ("000".equals(number)) {
            return OptionalInt.of(1);
        }
        if ("00".equals(number)) {
            return OptionalInt.of(2);
        }
        return OptionalInt.of(3 + Integer.parseInt(number));
    }

    private static String visualName(ProjectAssetReference asset) {
        String displayName = asset.displayName() == null ? "" : asset.displayName();
        String relativePath = asset.relativePath() == null ? "" : asset.relativePath();
        return (displayName + " " + relativePath).replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private record OrderedAsset(int order, ProjectAssetReference asset) {
    }
}
