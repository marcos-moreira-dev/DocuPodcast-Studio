package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.grammar.GrammarDiagnostic;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Optional production voice declarations. Markdown takes precedence over supplementary CSV. */
public final class TheatreGrammarVoiceConfiguration {
    public record Result(ImportPlan plan, List<GrammarDiagnostic> diagnostics) { }

    public Result enrich(Path source, ImportPlan plan) throws IOException {
        Path root = source.toAbsolutePath().getParent().toRealPath();
        Path csv = root.resolve("config/voces.csv");
        if (!Files.exists(csv)) return new Result(plan, List.of());
        List<GrammarDiagnostic> warnings = new ArrayList<>();
        if (!csv.toRealPath().startsWith(root)) {
            return new Result(plan, List.of(GrammarDiagnostic.warning("THEATRE_VOICE_CONFIG", "config/voces.csv sale de la carpeta de la obra.")));
        }
        List<List<String>> rows = rows(Files.readString(csv));
        if (rows.isEmpty()) return new Result(plan, List.of());
        List<String> header = rows.getFirst().stream().map(s -> s.replace("\uFEFF", "").strip()).toList();
        if (!header.containsAll(List.of("personaje_id", "voz_alias_o_mezcla", "modo", "sintetizar"))) {
            return new Result(plan, List.of(GrammarDiagnostic.warning("THEATRE_VOICE_CONFIG", "Cabecera de config/voces.csv no reconocida.")));
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        List<ProfilePlan> profiles = new ArrayList<>(plan.characters());
        Set<String> seen = new HashSet<>();
        for (List<String> row : rows.subList(1, rows.size())) {
            if (row.stream().allMatch(String::isBlank)) continue;
            if (row.size() != header.size()) {
                warnings.add(GrammarDiagnostic.warning("THEATRE_VOICE_CONFIG", "Fila incompleta en config/voces.csv."));
                continue;
            }
            String name = row.get(header.indexOf("personaje_id")).strip();
            String voice = row.get(header.indexOf("voz_alias_o_mezcla")).strip();
            String mode = row.get(header.indexOf("modo")).strip();
            if (!Boolean.parseBoolean(row.get(header.indexOf("sintetizar")).strip())) continue;
            if (!seen.add(name.toUpperCase(Locale.ROOT))) throw new IOException("Personaje duplicado en voces.csv: " + name);
            int index = -1;
            for (int i = 0; i < profiles.size(); i++) {
                ProfilePlan p = profiles.get(i);
                if (p.name().equalsIgnoreCase(name) || p.id().equalsIgnoreCase(name)
                        || p.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(name))) { index = i; break; }
            }
            if (index < 0) {
                warnings.add(GrammarDiagnostic.warning("THEATRE_VOICE_CONFIG", "Personaje de voces.csv no declarado: " + name));
                continue;
            }
            ProfilePlan p = profiles.get(index);
            if (mode.equalsIgnoreCase("MIX")) {
                groups.put(p.name(), Arrays.stream(voice.split(";")).map(String::strip).filter(s -> !s.isBlank()).toList());
            } else if (mode.equalsIgnoreCase("SINGLE") && !voice.isBlank()) {
                if (p.voz().isBlank()) profiles.set(index, new ProfilePlan(p.name(), p.notes(), voice, p.tono(), p.imagenes(), p.id(), p.aliases()));
                else if (!p.voz().equalsIgnoreCase(voice)) warnings.add(GrammarDiagnostic.warning("THEATRE_VOICE_CONFLICT", "Se conserva la voz del Markdown para " + name));
            } else warnings.add(GrammarDiagnostic.warning("THEATRE_VOICE_CONFIG", "Modo de voz no reconocido para " + name));
        }
        Map<String, List<String>> participants = new HashMap<>();
        for (var group : groups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (String alias : group.getValue()) {
                var match = profiles.stream().filter(p -> p.voz().equalsIgnoreCase(alias)).findFirst();
                if (match.isPresent()) names.add(match.get().name());
                else warnings.add(GrammarDiagnostic.warning("THEATRE_CHORUS_VOICE", "Voz del coro " + group.getKey() + " sin personaje asociado: " + alias));
            }
            participants.put(group.getKey().toUpperCase(Locale.ROOT), names.stream().distinct().toList());
        }
        List<InterventionPlan> interventions = plan.interventions().stream().map(i -> {
            List<String> names = participants.get(i.characterName().toUpperCase(Locale.ROOT));
            return names != null && names.size() >= 2 && i.simultaneousVoiceNames().isEmpty()
                    ? i.withSimultaneousVoiceNames(names) : i;
        }).toList();
        return new Result(new ImportPlan(plan.title(), plan.acts(), profiles, plan.objects(), plan.mediaLinks(),
                interventions, plan.voiceCatalog(), plan.toneCatalog(), plan.grammarVersion()), List.copyOf(warnings));
    }

    private static List<List<String>> rows(String text) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\n' || c == '\r')) {
                row.add(cell.toString()); cell.setLength(0);
                if (c != ',') {
                    rows.add(List.copyOf(row)); row.clear();
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        if (quoted) throw new IOException("Comillas sin cerrar en config/voces.csv");
        if (!cell.isEmpty() || !row.isEmpty()) { row.add(cell.toString()); rows.add(List.copyOf(row)); }
        return rows;
    }
}
