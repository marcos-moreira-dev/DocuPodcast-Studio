package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Parses Poppler's pdftotext -bbox-layout XHTML into page/block text with PDF-space bboxes. */
final class PdfBboxLayoutParser {
    PdfBboxExtraction parse(String xhtml) throws IOException {
        Document document = parseXml(stripDoctype(xhtml));
        NodeList pageNodes = document.getElementsByTagName("page");
        ArrayList<PdfBboxPage> pages = new ArrayList<>();
        for (int i = 0; i < pageNodes.getLength(); i++) {
            if (pageNodes.item(i) instanceof Element pageElement) {
                pages.add(parsePage(i + 1, pageElement));
            }
        }
        return new PdfBboxExtraction(pages);
    }

    private static PdfBboxPage parsePage(int pageNumber, Element pageElement) {
        double width = doubleAttribute(pageElement, "width").orElse(0.0);
        double height = doubleAttribute(pageElement, "height").orElse(0.0);
        NodeList blockNodes = pageElement.getElementsByTagName("block");
        ArrayList<PdfBboxTextBlock> parsedBlocks = new ArrayList<>();
        for (int i = 0; i < blockNodes.getLength(); i++) {
            if (blockNodes.item(i) instanceof Element blockElement) {
                Optional<PdfBox> box = boxFrom(blockElement);
                String text = blockText(blockElement);
                if (box.isPresent() && !text.isBlank()) {
                    parsedBlocks.add(new PdfBboxTextBlock(pageNumber, text, box.get(), width, height));
                }
            }
        }
        List<PdfBboxTextBlock> blocks = mergeNearbyBlocks(sortBlocks(parsedBlocks));
        return new PdfBboxPage(pageNumber, width, height, blocks);
    }

    private static List<PdfBboxTextBlock> sortBlocks(List<PdfBboxTextBlock> blocks) {
        return blocks.stream()
                .sorted(Comparator.comparingDouble((PdfBboxTextBlock block) -> block.bbox().yMin())
                        .thenComparingDouble(block -> block.bbox().xMin()))
                .toList();
    }

    private static List<PdfBboxTextBlock> mergeNearbyBlocks(List<PdfBboxTextBlock> blocks) {
        ArrayList<PdfBboxTextBlock> merged = new ArrayList<>();
        for (PdfBboxTextBlock block : blocks) {
            if (merged.isEmpty()) {
                merged.add(block);
                continue;
            }
            PdfBboxTextBlock previous = merged.getLast();
            if (shouldMerge(previous, block)) {
                merged.set(merged.size() - 1, merge(previous, block));
            } else {
                merged.add(block);
            }
        }
        return List.copyOf(merged);
    }

    private static boolean shouldMerge(PdfBboxTextBlock previous, PdfBboxTextBlock current) {
        if (previous.pageNumber() != current.pageNumber()) {
            return false;
        }
        if (previous.text().length() + current.text().length() > 1200) {
            return false;
        }
        double verticalGap = current.bbox().yMin() - previous.bbox().yMax();
        if (verticalGap < -2.0 || verticalGap > 14.0) {
            return false;
        }
        double overlap = Math.max(0.0, Math.min(previous.bbox().xMax(), current.bbox().xMax())
                - Math.max(previous.bbox().xMin(), current.bbox().xMin()));
        double narrower = Math.max(1.0, Math.min(previous.bbox().xMax() - previous.bbox().xMin(),
                current.bbox().xMax() - current.bbox().xMin()));
        boolean sameColumn = overlap / narrower >= 0.42
                || Math.abs(previous.bbox().xMin() - current.bbox().xMin()) <= 18.0;
        return sameColumn;
    }

    private static PdfBboxTextBlock merge(PdfBboxTextBlock previous, PdfBboxTextBlock current) {
        PdfBox box = new PdfBox(
                Math.min(previous.bbox().xMin(), current.bbox().xMin()),
                Math.min(previous.bbox().yMin(), current.bbox().yMin()),
                Math.max(previous.bbox().xMax(), current.bbox().xMax()),
                Math.max(previous.bbox().yMax(), current.bbox().yMax()));
        return new PdfBboxTextBlock(
                previous.pageNumber(),
                previous.text() + "\n" + current.text(),
                box,
                Math.max(previous.pageWidth(), current.pageWidth()),
                Math.max(previous.pageHeight(), current.pageHeight()));
    }

    private static String blockText(Element blockElement) {
        NodeList lineNodes = blockElement.getElementsByTagName("line");
        ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < lineNodes.getLength(); i++) {
            if (lineNodes.item(i) instanceof Element lineElement) {
                String line = lineText(lineElement);
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return String.join("\n", lines).strip();
    }

    private static String lineText(Element lineElement) {
        NodeList wordNodes = lineElement.getElementsByTagName("word");
        ArrayList<String> words = new ArrayList<>();
        for (int i = 0; i < wordNodes.getLength(); i++) {
            Node wordNode = wordNodes.item(i);
            String word = wordNode == null ? "" : wordNode.getTextContent().strip();
            if (!word.isBlank()) {
                words.add(word);
            }
        }
        return String.join(" ", words).strip();
    }

    private static Optional<PdfBox> boxFrom(Element element) {
        Optional<Double> xMin = doubleAttribute(element, "xMin");
        Optional<Double> yMin = doubleAttribute(element, "yMin");
        Optional<Double> xMax = doubleAttribute(element, "xMax");
        Optional<Double> yMax = doubleAttribute(element, "yMax");
        if (xMin.isEmpty() || yMin.isEmpty() || xMax.isEmpty() || yMax.isEmpty()) {
            return Optional.empty();
        }
        PdfBox box = new PdfBox(xMin.get(), yMin.get(), xMax.get(), yMax.get());
        return box.valid() ? Optional.of(box) : Optional.empty();
    }

    private static Optional<Double> doubleAttribute(Element element, String name) {
        String value = element == null ? "" : element.getAttribute(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value.strip()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Document parseXml(String xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml.stripLeading())));
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("No se pudo parsear salida bbox-layout de Poppler.", ex);
        }
    }

    private static String stripDoctype(String value) {
        String safe = value == null ? "" : value;
        return safe.replaceFirst("(?is)<!DOCTYPE[^>]*>", "");
    }
}

record PdfBboxExtraction(List<PdfBboxPage> pages) {
    PdfBboxExtraction {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    String rawText() {
        return pages.stream()
                .map(PdfBboxPage::rawText)
                .filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    int pageCount() {
        return Math.max(1, pages.size());
    }
}

record PdfBboxPage(int pageNumber, double width, double height, List<PdfBboxTextBlock> blocks) {
    PdfBboxPage {
        pageNumber = Math.max(1, pageNumber);
        width = finiteOrZero(width);
        height = finiteOrZero(height);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    String rawText() {
        return blocks.stream()
                .map(PdfBboxTextBlock::text)
                .filter(text -> !text.isBlank())
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}

record PdfBboxTextBlock(int pageNumber, String text, PdfBox bbox, double pageWidth, double pageHeight) {
    PdfBboxTextBlock {
        pageNumber = Math.max(1, pageNumber);
        text = text == null ? "" : text.strip();
        bbox = bbox == null ? new PdfBox(0, 0, 0, 0) : bbox;
        pageWidth = finiteOrZero(pageWidth);
        pageHeight = finiteOrZero(pageHeight);
    }

    String pageWidthLabel() {
        return String.format(Locale.ROOT, "%.3f", pageWidth);
    }

    String pageHeightLabel() {
        return String.format(Locale.ROOT, "%.3f", pageHeight);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
