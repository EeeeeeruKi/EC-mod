package com.eeeeeeruki.claudeupdater.client;

import com.eeeeeeruki.claudeupdater.core.UpdateCandidate;
import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shown before anything is put in place, when the chosen mods need other mods that are not there.
 * Mods in the library or on Modrinth can be downloaded together (selected by default); the others can have
 * their name copied or a search page opened. The chosen mods are only installed once the player decides.
 */
public class DependencyScreen extends Screen {
    /** What the player decided: install (with the required mods that were fetched) or cancel everything. */
    public interface Decision {
        void done(List<UpdateCandidate> extra, boolean install);
    }

    private enum Mode { LIST, DOWNLOADING }

    private static final int ROW_W = 320;
    private static final int ROW_H = 22;
    private static final int TOP = 56;
    private static final int SMALL = 48;

    /** One row: a mod that can be downloaded, or a mod the player has to get elsewhere. */
    private record Row(UpdateCandidate offer, UpdaterCore.MissingDeps.External external) {
    }

    private final Screen parent;
    private final UpdaterCore.MissingDeps deps;
    /** Everything in this install so far (the chosen mods and required mods already fetched). */
    private final List<UpdateCandidate> batch;
    /** Required mods fetched on earlier dependency screens of the same install. */
    private final List<UpdateCandidate> extra;
    /** Mod IDs already offered, so a mod that was turned down is not asked about again. */
    private final Set<String> asked;
    private final Decision decision;
    private final List<Row> rowsData = new ArrayList<>();
    private volatile Mode mode = Mode.LIST;
    private volatile boolean fetchedAll;
    private volatile UpdaterCore.MissingDeps next;
    private volatile boolean needRebuild;
    private int page;
    private final Map<Button, UpdateCandidate> rows = new LinkedHashMap<>();
    private Button mainButton;

    public DependencyScreen(Screen parent, UpdaterCore.MissingDeps deps, List<UpdateCandidate> batch, Decision decision) {
        this(parent, deps, batch, new ArrayList<>(), new HashSet<>(), decision);
    }

    private DependencyScreen(Screen parent, UpdaterCore.MissingDeps deps, List<UpdateCandidate> batch,
                             List<UpdateCandidate> extra, Set<String> asked, Decision decision) {
        super(Component.literal("必要なMOD"));
        this.parent = parent;
        this.deps = deps;
        this.batch = batch;
        this.extra = extra;
        this.asked = asked;
        this.decision = decision;
        for (UpdateCandidate c : deps.library) {
            rowsData.add(new Row(c, null));
            asked.add(c.id);
        }
        for (UpdaterCore.MissingDeps.External x : deps.external) {
            rowsData.add(new Row(null, x));
            asked.add(x.id());
        }
    }

    /** Rows get room for a line underneath when a mod that is not available has a note about Modrinth. */
    private int rowH() {
        return deps.external.stream().anyMatch(x -> x.note() != null) ? 34 : ROW_H;
    }

    @Override
    protected void init() {
        rows.clear();
        mainButton = null;
        int x = width / 2 - ROW_W / 2;
        int by = height - 28;

        int perPage = perPage();
        int pages = Math.max(1, (rowsData.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;
        for (int i = page * perPage; i < Math.min(rowsData.size(), (page + 1) * perPage); i++) {
            Row r = rowsData.get(i);
            int y = TOP + (i - page * perPage) * rowH();
            if (r.offer() != null) {
                UpdateCandidate c = r.offer();
                Button b = Button.builder(offerLabel(c), btn -> {
                    if (mode != Mode.LIST) return;
                    c.selected = !c.selected;
                    btn.setMessage(offerLabel(c));
                    if (mainButton != null) mainButton.active = anySelected();
                }).bounds(x, y, ROW_W, 20).tooltip(Tooltip.create(offerTooltip(c))).build();
                rows.put(addRenderableWidget(b), c);
            } else {
                UpdaterCore.MissingDeps.External ext = r.external();
                Button label = addRenderableWidget(Button.builder(
                                Component.literal(ext.name() + "   §7" + ext.neededBy() + " に必要"), b -> {
                                })
                        .bounds(x, y, ROW_W - SMALL * 2 - 4, 20)
                        .tooltip(Tooltip.create(Component.literal(ext.name() + "（MOD ID: " + ext.id() + "）\n"
                                + "MODライブラリにも Modrinth にもないので、自分で入れてください。")))
                        .build());
                label.active = false;
                addRenderableWidget(Button.builder(Component.literal("コピー"), b -> {
                            minecraft.keyboardHandler.setClipboard(ext.name());
                            b.setMessage(Component.literal("§aコピー済"));
                        })
                        .bounds(x + ROW_W - SMALL * 2 - 2, y, SMALL, 20)
                        .tooltip(Tooltip.create(Component.literal("「" + ext.name() + "」をクリップボードにコピーします")))
                        .build());
                addRenderableWidget(Button.builder(Component.literal("開く"), b -> openLink(ext.url()))
                        .bounds(x + ROW_W - SMALL, y, SMALL, 20)
                        .tooltip(Tooltip.create(Component.literal("「" + ext.name() + "」を探すページを開きます")))
                        .build());
            }
        }
        if (pages > 1) {
            int py = TOP + perPage * rowH() + 2;
            Button prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> { page--; rebuildWidgets(); })
                    .bounds(x, py, 20, 20).build());
            Button nextPage = addRenderableWidget(Button.builder(Component.literal("▶"), b -> { page++; rebuildWidgets(); })
                    .bounds(x + ROW_W - 20, py, 20, 20).build());
            prev.active = page > 0;
            nextPage.active = page < pages - 1;
        }

        if (mode != Mode.LIST) return; // DOWNLOADING: no buttons, the rows show progress
        if (!deps.library.isEmpty()) {
            mainButton = addRenderableWidget(Button.builder(Component.literal("選んだMODも一緒に入れる"), b -> startDownload())
                    .bounds(width / 2 - 155, by - 24, 310, 20).build());
            mainButton.active = anySelected();
            addRenderableWidget(Button.builder(Component.literal("必要なMODなしで入れる"), b -> finish(true))
                    .bounds(width / 2 - 155, by, 150, 20)
                    .tooltip(Tooltip.create(Component.literal("必要なMODが無いと、ゲームが起動しないことがあります。")))
                    .build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("このまま入れる"), b -> finish(true))
                    .bounds(width / 2 - 155, by, 150, 20)
                    .tooltip(Tooltip.create(Component.literal("必要なMODは、あとで自分で入れてください。\n入れないと、ゲームが起動しないことがあります。")))
                    .build());
        }
        addRenderableWidget(Button.builder(Component.literal("やめる"), b -> finish(false))
                .bounds(width / 2 + 5, by, 150, 20)
                .tooltip(Tooltip.create(Component.literal("何も入れずに、MODを選ぶ画面に戻ります。")))
                .build());
    }

    private void openLink(String url) {
        minecraft.setScreen(new ConfirmLinkScreen(yes -> {
            if (yes) Util.getPlatform().openUri(url);
            minecraft.setScreen(this);
        }, url, true));
    }

    private int perPage() {
        int bottom = height - 28 - 24 - 6; // keep room for the two button rows
        return Math.max(1, (bottom - TOP - ROW_H) / rowH());
    }

    private boolean anySelected() {
        return deps.library.stream().anyMatch(c -> c.selected);
    }

    private void startDownload() {
        mode = Mode.DOWNLOADING;
        rebuildWidgets();
        Thread t = new Thread(() -> {
            List<UpdateCandidate> chosen = new ArrayList<>();
            for (UpdateCandidate c : deps.library) if (c.selected) chosen.add(c);
            UpdaterCore.fetchAll(chosen);
            for (UpdateCandidate c : chosen) if (c.fetched) extra.add(c);
            // what the newly fetched mods need in turn (anything asked about already is left out)
            List<UpdateCandidate> all = new ArrayList<>(batch);
            all.addAll(extra);
            UpdaterCore.MissingDeps more = UpdaterCore.missingDeps(all);
            more.library.removeIf(c -> asked.contains(c.id));
            more.external.removeIf(x -> asked.contains(x.id()));
            next = more;
            fetchedAll = true;
        }, "ClaudeUpdater-Dependencies");
        t.setDaemon(true);
        t.start();
    }

    private void finish(boolean install) {
        if (!install) {
            UpdaterCore.discard(extra);
            UpdaterCore.discard(deps.library);
        }
        decision.done(new ArrayList<>(extra), install);
    }

    private Component offerLabel(UpdateCandidate c) {
        String ver = "v" + c.release.version;
        String from = "§8（" + c.source + "）";
        if (mode == Mode.LIST) {
            return Component.literal((c.selected ? "☑ " : "☐ ") + c.name + " " + from + "  §7" + c.neededBy + " に必要");
        }
        if (!c.selected) return Component.literal("§7" + c.name + "   入れません");
        String s;
        switch (c.state) {
            case WAITING: s = "§7待機中"; break;
            case DOWNLOADING: s = "§eダウンロード中…"; break;
            case READY: case DONE: s = "§aダウンロードしました"; break;
            default: s = "§c失敗しました"; break;
        }
        return Component.literal(c.name + "   " + ver + "   " + s);
    }

    private Component offerTooltip(UpdateCandidate c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" v").append(c.release.version).append("\n§7").append(c.neededBy).append(" を動かすのに必要です");
        sb.append("\n§7").append("Modrinth".equals(c.source) ? "Modrinth からダウンロードします" : "MODライブラリからダウンロードします");
        if (c.summary != null && !c.summary.isBlank()) sb.append("\n§f").append(c.summary);
        sb.append("\n§8").append(c.release.fileName);
        if (c.error != null) sb.append("\n§c").append(c.error);
        return Component.literal(sb.toString());
    }

    @Override
    public void tick() {
        super.tick();
        if (needRebuild) {
            needRebuild = false;
            rebuildWidgets();
            return;
        }
        if (fetchedAll) {
            fetchedAll = false;
            UpdaterCore.MissingDeps more = next;
            if (more != null && !more.isEmpty()) {
                minecraft.setScreen(new DependencyScreen(parent, more, batch, extra, asked, decision));
            } else {
                finish(true);
            }
            return;
        }
        if (mode != Mode.LIST) {
            for (Map.Entry<Button, UpdateCandidate> e : rows.entrySet()) {
                e.getKey().setMessage(offerLabel(e.getValue()));
                e.getKey().setTooltip(Tooltip.create(offerTooltip(e.getValue())));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "必要なMOD", width / 2, 14, 0xFFFFFF);
        String l1;
        String l2;
        if (mode == Mode.DOWNLOADING) {
            l1 = "必要なMODをダウンロードしています…";
            l2 = null;
        } else {
            l1 = "§e選んだMODを動かすには、ほかのMODも必要です。§rまだ何も入れていません。";
            if (!deps.library.isEmpty()) {
                l2 = deps.external.isEmpty() ? "MODライブラリか Modrinth にあるので、一緒に入れるのがおすすめです。"
                        : "チェックしたMODは一緒に入れられます。ほかのMODは自分で入れてください。";
            } else {
                l2 = "MODライブラリにも Modrinth にもないので、名前をコピーするか「開く」で探して入れてください。";
            }
        }
        g.drawCenteredString(font, l1, width / 2, 30, 0xCCCCCC);
        if (l2 != null) g.drawCenteredString(font, l2, width / 2, 42, 0x999999);

        int perPage = perPage();
        int pages = Math.max(1, (rowsData.size() + perPage - 1) / perPage);
        if (pages > 1) {
            g.drawCenteredString(font, (page + 1) + " / " + pages, width / 2, TOP + perPage * rowH() + 8, 0xAAAAAA);
        }
        // under each mod that cannot be downloaded here: what Modrinth said
        for (int i = page * perPage; i < Math.min(rowsData.size(), (page + 1) * perPage); i++) {
            Row r = rowsData.get(i);
            if (r.external() == null || r.external().note() == null) continue;
            int y = TOP + (i - page * perPage) * rowH() + 23;
            g.drawCenteredString(font, r.external().note(), width / 2, y, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return mode != Mode.DOWNLOADING;
    }

    @Override
    public void onClose() {
        finish(false); // Esc = cancel, nothing is installed
    }
}
