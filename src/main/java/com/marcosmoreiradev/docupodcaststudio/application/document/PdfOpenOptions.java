package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Options for opening a PDF without binding the application layer to a renderer. */
public record PdfOpenOptions(String password) {
    public PdfOpenOptions {
        password = password == null ? "" : password;
    }

    public static PdfOpenOptions empty() {
        return new PdfOpenOptions("");
    }

    public boolean hasPassword() {
        return !password.isBlank();
    }
}
