package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.util.ArrayList;
import java.util.List;

final class CommandLineTokenizer {
    private CommandLineTokenizer() { }

    static List<String> split(String command) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < (command == null ? 0 : command.length()); i++) {
            char c = command.charAt(i);
            if ((c == '\"' || c == '\'') && (!quoted || quote == c)) {
                quoted = !quoted;
                quote = quoted ? c : 0;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) { tokens.add(current.toString()); current.setLength(0); }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return List.copyOf(tokens);
    }
}
