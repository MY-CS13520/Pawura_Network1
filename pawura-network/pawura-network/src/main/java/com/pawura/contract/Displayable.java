package com.pawura.contract;

/**
 * Displayable – any domain object that can produce a human-readable summary.
 */
public interface Displayable {
    /** Returns a formatted, one-line summary suitable for list cells. */
    String getDisplaySummary();

    /** Returns a multi-line detail string for a detail panel. */
    String getDetailText();
}
