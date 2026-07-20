package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

/** Width policy shared by every product side-dock. */
public record SideDockLayoutPolicy(
        WidthRange collapsed,
        WidthRange expanded,
        WidthRange compactExpanded) {

    public SideDockLayoutPolicy {
        collapsed = require(collapsed, "collapsed");
        expanded = require(expanded, "expanded");
        compactExpanded = compactExpanded == null ? expanded : compactExpanded;
    }

    public static SideDockLayoutPolicy of(double collapsedWidth, double expandedMin, double expandedPref) {
        return new SideDockLayoutPolicy(
                WidthRange.fixed(collapsedWidth),
                WidthRange.flexible(expandedMin, expandedPref),
                null);
    }

    public static SideDockLayoutPolicy withCompact(double collapsedWidth,
                                                    double expandedMin,
                                                    double expandedPref,
                                                    double compactMin,
                                                    double compactPref) {
        return new SideDockLayoutPolicy(
                WidthRange.fixed(collapsedWidth),
                WidthRange.flexible(expandedMin, expandedPref),
                WidthRange.flexible(compactMin, compactPref));
    }

    private static WidthRange require(WidthRange range, String field) {
        if (range == null) throw new IllegalArgumentException(field + " is required");
        return range;
    }

    public record WidthRange(double min, double pref, double max) {
        public WidthRange {
            if (min < 0 || pref < min || max < pref) {
                throw new IllegalArgumentException("Expected 0 <= min <= pref <= max");
            }
        }

        public static WidthRange fixed(double width) { return new WidthRange(width, width, width); }
        public static WidthRange flexible(double min, double pref) {
            return new WidthRange(min, pref, Double.MAX_VALUE);
        }
    }
}
