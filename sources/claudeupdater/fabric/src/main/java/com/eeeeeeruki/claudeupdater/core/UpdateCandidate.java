package com.eeeeeeruki.claudeupdater.core;

/**
 * One mod from the index to download: either a newer version of an installed mod ({@link #installed} set),
 * or a mod that is not installed yet ({@link #installed} null, from the install screen).
 */
public class UpdateCandidate {
    public enum State { WAITING, DOWNLOADING, DONE, FAILED }

    public final String id;
    public final InstalledMod installed;
    public final String name;
    public final String summary;
    public final ModIndex.Release release;

    public volatile boolean selected;
    public volatile State state = State.WAITING;
    public volatile String error;

    /** An update for an installed mod (selected by default). */
    public UpdateCandidate(InstalledMod installed, String name, ModIndex.Release release) {
        this.id = installed.id();
        this.installed = installed;
        this.name = name;
        this.summary = null;
        this.release = release;
        this.selected = true;
    }

    /** A mod that is not installed yet (not selected by default). */
    public UpdateCandidate(String id, String name, String summary, ModIndex.Release release) {
        this.id = id;
        this.installed = null;
        this.name = name;
        this.summary = summary;
        this.release = release;
        this.selected = false;
    }

    public boolean isNewInstall() {
        return installed == null;
    }
}
