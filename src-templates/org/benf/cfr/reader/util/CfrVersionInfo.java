package org.benf.cfr.reader.util;

/**
 * Provides information about the CFR build.
 *
 * <p>The information in this class is automatically generated when the project
 * is built.
 */
public class CfrVersionInfo {
    private CfrVersionInfo() {}

    /** CFR version */
    public static final String VERSION = "0.2-SNAPSHOT";

    /** Are we a snapshot? */
    public static final boolean SNAPSHOT = CfrVersionInfo.VERSION.contains("SNAPSHOT");

    /**
     * Whether the working tree contained not yet committed changes when
     * the project was built.
     *
     * <p>This information can be useful for error reports to find out
     * if changes have been made.
     */
    public static final boolean GIT_IS_DIRTY = false;

    /** String consisting of CFR version and Git commit hash */
    public static final String VERSION_INFO =
            VERSION + " (FabricMC)";
}
