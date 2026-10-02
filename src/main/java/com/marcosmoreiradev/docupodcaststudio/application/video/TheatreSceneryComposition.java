package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreStageBackdropResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** The same deterministic stage composition is used by preview and video. */
public final class TheatreSceneryComposition {
    public byte[] renderPng(Composition composition, int width, int height) throws java.io.IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        ImageIO.write(render(composition, width, height), "png", bytes);
        return bytes.toByteArray();
    }
    public void writePng(Composition composition, int width, int height, Path target) throws java.io.IOException {
        Files.write(target, renderPng(composition, width, height));
    }

    public static final String MODE = "scenery";
    public static final String VIEW_KEY = "theatre.presentationMode";
    private static final Color DIRECTION_ARROW_FILL = new Color(253, 236, 200);
    private static final Color DIRECTION_ARROW_BORDER = new Color(45, 32, 18);
    private static final int MAX_INDIVIDUAL_RECIPIENTS = 4;
    public record Figure(String id, String name, boolean speaker, Path image, boolean assigned, boolean collective) {
        public Figure(String id,String name,boolean speaker,Path image,boolean assigned) {
            this(id,name,speaker,image,assigned,false);
        }
    }
    public record Composition(Path backdrop, List<Figure> figures) {}

    public Composition resolve(DocuPodcastProject project, TheatreProjectLayer.TextActionPlacement placement, Path root) {
        if (project == null || placement == null || root == null) return new Composition(null, List.of());
        var theatre = project.theatre();
        Path backdrop = new TheatreStageBackdropResolver().resolve(project, placement.sceneId(), placement.intervencionId(), root)
                .map(TheatreStageBackdropResolver.ResolvedBackdrop::absolutePath).orElse(null);
        if (isOffstage(placement.origin())) {
            return new Composition(backdrop, List.of());
        }
        LinkedHashMap<String, Boolean> selected = new LinkedHashMap<>();
        var chorus = theatre.choralVoiceAssignments().stream()
                .filter(c -> c.intervencionId().equals(placement.intervencionId())).findFirst();
        if (chorus.isPresent()) {
            chorus.get().participantCharacterIds().forEach(id -> selected.put(id, true));
        } else if (!placement.characterId().isBlank()) {
            selected.put(placement.characterId(), true);
        }
        for (String raw : placement.interactionTarget().split("[,;|]")) {
            String target = raw.strip();
            if (target.isBlank() || target.equalsIgnoreCase("para si mismo")) continue;
            String id = theatre.characters().stream().filter(c -> c.id().equalsIgnoreCase(target)
                    || c.displayName().equalsIgnoreCase(target) || c.aliases().stream().anyMatch(target::equalsIgnoreCase))
                    .map(TheatreProjectLayer.CharacterProfile::id).findFirst().orElse(target);
            selected.putIfAbsent(id, false);
        }
        List<Figure> figures = new ArrayList<>();
        selected.forEach((id, speaker) -> {
            String name = theatre.characters().stream().filter(c -> c.id().equals(id))
                    .map(TheatreProjectLayer.CharacterProfile::displayName).findFirst().orElse(id);
            String asset = speaker && !chorus.isPresent() ? theatre.intervencionesVisuales().stream()
                    .filter(v -> v.intervencionId().equals(placement.intervencionId())).map(TheatreProjectLayer.IntervencionVisual::assetId)
                    .findFirst().orElse("") : "";
            if (asset.isBlank()) asset = theatre.characterImages().stream().filter(i -> i.characterId().equals(id))
                    .filter(i -> i.sceneId().isBlank() || i.sceneId().equals(placement.sceneId()))
                    .sorted(Comparator.comparing(i -> i.sceneId().isBlank()))
                    .map(TheatreProjectLayer.CharacterImage::assetId).findFirst().orElse("");
            figures.add(new Figure(id, name, speaker, path(project, root, asset), !asset.isBlank(),
                    theatre.characters().stream().noneMatch(c -> c.id().equals(id))));
        });
        return new Composition(backdrop, compactRecipients(figures, placement.interactionTarget()));
    }

    static boolean isOffstage(String location) {
        if (location == null) return false;
        String normalized = java.text.Normalizer
                .normalize(location.strip().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('-', '_')
                .replace(' ', '_');
        return normalized.equals("fuera_escena") || normalized.equals("fuera_de_escena")
                || normalized.equals("offstage");
    }

    static List<Figure> compactRecipients(List<Figure> figures, String interactionTarget) {
        if (figures == null || figures.isEmpty()) return List.of();
        List<Figure> recipients = figures.stream().filter(figure -> !figure.speaker()).toList();
        if (recipients.size() <= MAX_INDIVIDUAL_RECIPIENTS) return List.copyOf(figures);
        ArrayList<Figure> compact = new ArrayList<>();
        figures.stream().filter(Figure::speaker).forEach(compact::add);
        compact.add(new Figure("COLLECTIVE_RECIPIENTS", collectiveRecipientLabel(interactionTarget),
                false, null, true, true));
        return List.copyOf(compact);
    }

    private static String collectiveRecipientLabel(String interactionTarget) {
        String normalized = interactionTarget == null ? "" : java.text.Normalizer
                .normalize(interactionTarget.strip().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.contains("pueblo") ? "Todo el pueblo" : "Todos los personajes restantes";
    }

    private Path path(DocuPodcastProject project, Path root, String asset) {
        return project.assets().byId(asset).filter(a -> a.isImage()).map(a -> root.resolve(a.relativePath()).toAbsolutePath().normalize())
                .filter(p -> p.startsWith(root.toAbsolutePath().normalize()) && Files.isRegularFile(p)).orElse(null);
    }

    public BufferedImage render(Composition scene, int width, int height) {
        BufferedImage result = new BufferedImage(Math.max(1,width), Math.max(1,height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.BLACK); g.fillRect(0,0,width,height);
            BufferedImage bg = read(scene.backdrop());
            Rectangle bounds = new Rectangle(0,0,width,height);
            if (bg != null) bounds = fit(g, bg, bounds, false);
            else { g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 24)); g.drawString("Escenografía sin asignar", 24, 40); }
            int count = scene.figures().size();
            if (count == 0) return result;
            int gap = Math.max(4, width / 100);
            int slot = Math.max(1, Math.min((int)(bounds.width*.30), (bounds.width-gap*(count+1))/count));
            int figureHeight = Math.max(1,(int)(bounds.height*.62));
            int start = bounds.x+(bounds.width-count*slot-(count-1)*gap)/2;
            int base = bounds.y+bounds.height-Math.max(8,height/40);
            for (int i=0;i<count;i++) {
                Figure f = scene.figures().get(i);
                int x=start+i*(slot+gap), labelHeight=Math.max(28,height/20);
                Rectangle box = new Rectangle(x,base-labelHeight-figureHeight,slot,figureHeight);
                BufferedImage photo = read(f.image());
                if (photo != null) fit(g,photo,box,true);
                else if (f.collective()) {
                    drawCollectiveRecipient(g, f.name(), box, width, height);
                } else {
                    g.setColor(Color.BLACK); g.fillRect(box.x,box.y,box.width,box.height);
                    label(g,f.assigned()?"Imagen no disponible":"Imagen sin asignar",box.x,box.y+box.height/2,slot,Math.max(10,width/95));
                }
                g.setColor(new Color(0,0,0,200)); g.fillRect(x,base-labelHeight,slot,labelHeight);
                label(g,(f.speaker()?"Habla: ":"Se dirige a: ")+f.name(),x,base-labelHeight/3,slot,Math.max(12,width/80));
            }
            drawDirectionArrows(g, scene.figures(), bounds, start, slot, gap, width, height);
        } finally { g.dispose(); }
        return result;
    }

    private static void drawDirectionArrows(Graphics2D g, List<Figure> figures,
                                            Rectangle bounds, int start, int slot, int gap,
                                            int width, int height) {
        int speaker = -1;
        for (int i = 0; i < figures.size(); i++) {
            Figure figure = figures.get(i);
            if (figure.speaker() && !figure.collective()) {
                speaker = i;
                break;
            }
        }
        if (speaker < 0) return;
        ArrayList<Integer> recipients = new ArrayList<>();
        for (int i = 0; i < figures.size(); i++) {
            Figure recipient = figures.get(i);
            if (i == speaker || recipient.speaker() || isAudience(recipient)) continue;
            recipients.add(i);
        }
        if (recipients.isEmpty()) return;
        double fromX = start + speaker * (slot + gap) + slot / 2.0;
        double toX = recipients.stream()
                .mapToDouble(index -> start + index * (slot + gap) + slot / 2.0)
                .average().orElse(fromX);
        double y = bounds.y + Math.max(24.0, bounds.height * 0.16);
        drawSolidArrow(g, fromX, toX, y, width, height);
    }

    private static boolean isAudience(Figure figure) {
        String value = (figure.id() + " " + figure.name()).toUpperCase(Locale.ROOT);
        return value.contains("PUBLICO") || value.contains("PÚBLICO") || value.contains("AUDIENCE");
    }

    private static void drawCollectiveRecipient(Graphics2D g, String text, Rectangle box,
                                                int width, int height) {
        int cardWidth = Math.max(120, (int) Math.round(box.width * 0.86));
        int cardHeight = Math.max(64, Math.min((int) Math.round(box.height * 0.34), height / 4));
        int x = box.x + (box.width - cardWidth) / 2;
        int y = box.y + (box.height - cardHeight) / 2;
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRoundRect(x, y, cardWidth, cardHeight, 22, 22);
        g.setColor(DIRECTION_ARROW_FILL);
        g.setStroke(new BasicStroke(Math.max(2.0f, width / 520.0f)));
        g.drawRoundRect(x, y, cardWidth, cardHeight, 22, 22);
        label(g, text, x, y + cardHeight / 2 + Math.max(5, height / 120), cardWidth,
                Math.max(14, width / 64));
    }

    private static void drawSolidArrow(Graphics2D g, double fromX, double toX, double y,
                                       int width, int height) {
        double distance = Math.abs(toX - fromX);
        if (distance < 32.0) return;
        double direction = Math.signum(toX - fromX);
        double inset = Math.min(distance * 0.18, Math.max(18.0, width / 42.0));
        double startX = fromX + direction * inset;
        double tipX = toX - direction * inset;
        double arrowLength = Math.abs(tipX - startX);
        if (arrowLength < 24.0) return;
        double shaftHalf = Math.max(4.0, height / 90.0);
        double headHalf = Math.max(11.0, height / 42.0);
        double headLength = Math.min(arrowLength * 0.38, Math.max(22.0, width / 34.0));
        double neckX = tipX - direction * headLength;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(startX, y - shaftHalf);
        arrow.lineTo(neckX, y - shaftHalf);
        arrow.lineTo(neckX, y - headHalf);
        arrow.lineTo(tipX, y);
        arrow.lineTo(neckX, y + headHalf);
        arrow.lineTo(neckX, y + shaftHalf);
        arrow.lineTo(startX, y + shaftHalf);
        arrow.closePath();
        g.setColor(DIRECTION_ARROW_FILL);
        g.fill(arrow);
        g.setColor(DIRECTION_ARROW_BORDER);
        g.setStroke(new BasicStroke(Math.max(2.0f, width / 520.0f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);
    }
    private static BufferedImage read(Path path) {
        if(path==null) return null;
        try { return ImageIO.read(path.toFile()); } catch(java.io.IOException e) { return null; }
    }
    private static Rectangle fit(Graphics2D g, BufferedImage image, Rectangle box, boolean bottom) {
        double scale=Math.min((double)box.width/image.getWidth(),(double)box.height/image.getHeight());
        int w=Math.max(1,(int)(image.getWidth()*scale)), h=Math.max(1,(int)(image.getHeight()*scale));
        int x=box.x+(box.width-w)/2,y=box.y+(bottom?box.height-h:(box.height-h)/2);
        g.drawImage(image,x,y,w,h,null); // SrcOver preserves PNG alpha against the already painted stage.
        return new Rectangle(x,y,w,h);
    }
    private static void label(Graphics2D g,String text,int x,int baseline,int width,int size) {
        do { g.setFont(new Font("Georgia",Font.BOLD,size--)); } while(size>8 && g.getFontMetrics().stringWidth(text)>width-6);
        g.setColor(Color.WHITE); g.drawString(text,x+Math.max(3,(width-g.getFontMetrics().stringWidth(text))/2),baseline);
    }
}
