package com.eeeeeeruki.claudeupdater.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of index.json in the distribution repository.
 *
 * <pre>
 * {
 *   "format": 1,
 *   "mods": {
 *     "recipecraft": {
 *       "name": "RecipeCraft",
 *       "versions": [
 *         { "version": "0.2.2", "mc": "1.20.1", "loader": "forge",
 *           "url": "https://github.com/.../recipecraft-0.2.2-forge-1.20.1.jar",
 *           "fileName": "recipecraft-0.2.2-forge-1.20.1.jar",
 *           "sha256": "...", "changelog": "...", "released": "2026-10-26" }
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
public class ModIndex {
    public int format = 1;
    public Map<String, ModEntry> mods = new LinkedHashMap<>();

    public static class ModEntry {
        public String name;
        /** One-line description shown on the install screen. */
        public String summary;
        /** false for entries that only exist to move old installs over (e.g. a renamed mod). */
        public Boolean installable;
        public List<Release> versions = new ArrayList<>();
    }

    public static class Release {
        public String version;
        public String mc;
        public String loader;
        public String url;
        public String fileName;
        public String sha256;
        public String changelog;
        public String released;
    }
}
