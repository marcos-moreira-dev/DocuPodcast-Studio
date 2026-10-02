package com.marcosmoreiradev.docupodcaststudio.application.document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Secure structural gate applied before MathML reaches a speech engine. */
public final class MathMlSafetyValidator {
    private MathMlSafetyValidator() { }

    public static String validate(String value) {
        String mathMl = value == null ? "" : value.strip();
        if (mathMl.isBlank() || mathMl.contains("<!DOCTYPE")
                || mathMl.contains("<!ENTITY")) {
            throw new IllegalArgumentException("MathML vacío o inseguro.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(mathMl.getBytes(StandardCharsets.UTF_8)));
            if (!"math".equals(document.getDocumentElement().getLocalName())) {
                throw new IllegalArgumentException("La raíz MathML debe ser <math>.");
            }
            return mathMl;
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (Exception invalid) {
            throw new IllegalArgumentException("MathML no válido.", invalid);
        }
    }
}
