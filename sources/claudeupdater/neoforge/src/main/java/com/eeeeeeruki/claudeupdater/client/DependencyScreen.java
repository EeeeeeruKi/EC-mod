package com.eeeeeeruki.claudeupdater.client;

import com.eeeeeeruki.claudeupdater.core.ModSites;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shown after a download when the new jars need other mods that are not there: the ones in the library can be
 * downloaded together (selected by default), the others can have their name copied or a search page opened.
 */
public class DependencyScreen extends Screen {
    private enum Mode { LIST, DOWNLOADING, FINISHED }

    private static final int ROW_W = 320;
    private static final int ROW_H = 22;
    private static final int TOP = 56;
    private static final int SMALL = 48;

    /** One row: a library mod to download, or a mod the player has to get elsewhere. */
    private record Row(UpdateCandidate library, UpdaterCore.MissingDeps.External external) {
    }

    private final Screen parent;
    private final UpdaterCore.MissingDeps deps;
    private final List<Row> rowsData = new ArrayList<>();
    private volatile Mode mode = Mode.LIST;
    private volatile boolean needRebuild;
    private volatile UpdaterCore.MissingDeps next;
    private int page;
    private final Map<Button, UpdateCandidate> rows = new LinkedHashMap<>();
    private Button mainButton;
    /** CurseForge or Modrinth when the game was surely started from that launcher, otherwise OTHER. */
    private final ModSites.Launcher site;
    /** Per missing mod ID: whether it is on the launcher's site, filled in by a background lookup. */
    private final Map<String, ModSites.Result> notes = new ConcurrentHashMap<>();

    public DependencyScreen(Screen parent, UpdaterCore.MissingDeps deps) {
        super(Component.literal("必要なMOD"));
        this.parent = parent;
        this.deps = deps;
        for (UpdateCandidate c : deps.library) rowsData.add(new Row(c, null));
        for (UpdaterCore.MissingDeps.External x : deps.external) rowsData.add(new Row(null, x));
        site = ModSites.launcher(UpdaterCore.env().gameDir());
        if (site.site != null && !deps.external.isEmpty()) {
            Thread t = new Thread(() -> {
                for (UpdaterCore.MissingDeps.External x : deps.external) {
                    notes.put(x.id(), ModSites.lookup(site, x.id(), x.name(),
                            UpdaterCore.env().mcVersion(), UpdaterCore.env().loader()));
                }
            }, "ClaudeUpdater-SiteLookup");
            t.setDaemon(true);
            t.start();
        }
    }

    /** Rows get room for a sentence underneath when the launcher's site is being checked. */
    private int rowH() {
        return site.site != null && !deps.external.isEmpty() ? 34 : ROW_H;
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
            if (r.library() != null) {
                UpdateCandidate c = r.library();
                Button b = Button.builder(libraryLabel(c), btn -> {
                    if (mode != Mode.LIST) return;
                    c.selected = !c.selected;
                    btn.setMessage(libraryLabel(c));
                    if (mainButton != null) mainButton.active = anySelected();
                }).bounds(x, y, ROW_W, 20).tooltip(Tooltip.create(libraryTooltip(c))).build();
                rows.put(addRenderableWidget(b), c);
            } else {
                UpdaterCore.MissingDeps.External ext = r.external();
                Button label = addRenderableWidget(Button.builder(
                                Component.literal(ext.name() + "   §7" + ext.neededBy() + " に必要"), b -> {
                                })
                        .bounds(x, y, ROW_W - SMALL * 2 - 4, 20)
                        .tooltip(Tooltip.create(Component.literal(ext.name() + "（MOD ID: " + ext.id() + "）\n"
                                + "MODライブラリにはありません。名前をコピーして探すか、「開く」で Modrinth の検索ページを開けます。")))
                        .build());
                label.active = false;
                addRenderableWidget(Button.builder(Component.literal("コピー"), b -> {
                            minecraft.keyboardHandler.setClipboard(ext.name());
                            b.setMessage(Component.literal("§aコピー済"));
                        })
                        .bounds(x + ROW_W - SMALL * 2 - 2, y, SMALL, 20)
                        .tooltip(Tooltip.create(Component.literal("「" + ext.name() + "」をクリップボードにコピーします")))
                        .build());
                addRenderableWidget(Button.builder(Component.literal("開く"), b -> {
                            ModSites.Result n = notes.get(ext.id());
                            openLink(n != null ? n.url() : ext.url());
                        })
                        .bounds(x + ROW_W - SMALL, y, SMALL, 20)
                        .tooltip(Tooltip.create(Component.literal((site.site != null ? site.site : "Modrinth")
                                + " で「" + ext.name() + "」のページを開きます")))
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

        if (mode == Mode.LIST && !deps.library.isEmpty()) {
            mainButton = addRenderableWidget(Button.builder(Component.literal("選んだMODもダウンロード"), b -> startDownload())
                    .bounds(width / 2 - 155, by, 150, 20).build());
            mainButton.active = anySelected();
            addRenderableWidget(Button.builder(Component.literal("ダウンロードしない"), b -> onClose())
                    .bounds(width / 2 + 5, by, 150, 20).build());
        } else if (mode != Mode.DOWNLOADING) {
            addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                    .bounds(width / 2 - 75, by, 150, 20).build());
        }
    }

    private void openLink(String url) {
        minecraft.setScreen(new ConfirmLinkScreen(yes -> {
            if (yes) Util.getPlatform().openUri(url);
            minecraft.setScreen(this);
        }, url, true));
    }

    private int perPage() {
        int bottom = height - 28 - 6;
        return Math.max(1, (bottom - TOP - ROW_H) / rowH());
    }

    private boolean anySelected() {
        return deps.library.stream().anyMatch(c -> c.selected);
    }

    private void startDownload() {
        mode = Mode.DOWNLOADING;
        rebuildWidgets();
        Thread t = new Thread(() -> {
            UpdaterCore.downloadAll(deps.library);
            next = UpdaterCore.missingDeps(deps.library); // what the new ones need in turn
            mode = Mode.FINISHED;
            needRebuild = true;
        }, "ClaudeUpdater-Dependencies");
        t.setDaemon(true);
        t.start();
    }

    private Component libraryLabel(UpdateCandidate c) {
        String ver = "v" + c.release.version;
        if (mode == Mode.LIST) {
            return Component.literal((c.selected ? "☑ " : "☐ ") + c.name + "   §7" + ver + "  " + c.neededBy + " に必要");
        }
        if (!c.selected) return Component.literal("§7" + c.name + "   入れません");
        String s;
        switch (c.state) {
            case WAITING: s = "§7待機中"; break;
            case DOWNLOADING: s = "§eダウンロード中…"; break;
            case DONE: s = "§a完了"; break;
            default: s = "§c失敗しました"; break;
        }
        return Component.literal(c.name + "   " + ver + "   " + s);
    }

    private Component libraryTooltip(UpdateCandidate c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" v").append(c.release.version).append("\n§7").append(c.neededBy).append(" を動かすのに必要です");
        if (c.summary != null && !c.summary.isBlank()) sb.append("\n§f").append(c.summary);
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
        if (mode != Mode.LIST) {
            for (Map.Entry<Button, UpdateCandidate> e : rows.entrySet()) {
                e.getKey().setMessage(libraryLabel(e.getValue()));
                e.getKey().setTooltip(Tooltip.create(libraryTooltip(e.getValue())));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "必要なMOD", width / 2, 14, 0xFFFFFF);
        String l1;
        String l2 = null;
        switch (mode) {
            case LIST:
                l1 = "ダウンロードしたMODを動かすには、ほかのMODも必要です。";
                if (!deps.library.isEmpty()) {
                    l2 = deps.external.isEmpty() ? "MODライブラリにあるので、一緒にダウンロードしておくのがおすすめです。"
                            : "ライブラリにあるMODは一緒にダウンロードできます。ほかは自分で入れてください。";
                } else {
                    l2 = "MODライブラリにはないので、名前をコピーするか「開く」で探して入れてください。";
                }
                break;
            case DOWNLOADING:
                l1 = "ダウンロードしています…";
                break;
            default:
                boolean ok = deps.library.stream().anyMatch(c -> c.state == UpdateCandidate.State.DONE);
                l1 = ok ? "mods フォルダに入れました。ゲームを起動し直すと使えるようになります。"
                        : "ダウンロードできませんでした。カーソルを合わせると理由が表示されます。";
                if (!deps.external.isEmpty()) l2 = "ライブラリにないMODは、自分で入れてください。";
                break;
        }
        g.drawCenteredString(font, l1, width / 2, 30, 0xCCCCCC);
        if (l2 != null) g.drawCenteredString(font, l2, width / 2, 42, 0x999999);

        int perPage = perPage();
        int pages = Math.max(1, (rowsData.size() + perPage - 1) / perPage);
        if (pages > 1) {
            g.drawCenteredString(font, (page + 1) + " / " + pages, width / 2, TOP + perPage * rowH() + 8, 0xAAAAAA);
        }
        // under each mod that is not in the library: is it on the launcher's own site?
        if (site.site != null) {
            for (int i = page * perPage; i < Math.min(rowsData.size(), (page + 1) * perPage); i++) {
                Row r = rowsData.get(i);
                if (r.external() == null) continue;
                int y = TOP + (i - page * perPage) * rowH() + 23;
                ModSites.Result n = notes.get(r.external().id());
                String text = n != null ? n.sentence() : "§7" + site.site + " にあるか調べています…";
                g.drawCenteredString(font, text, width / 2, y, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return mode != Mode.DOWNLOADING;
    }

    @Override
    public void onClose() {
        UpdaterCore.MissingDeps more = next;
        if (more != null && !more.isEmpty()) minecraft.setScreen(new DependencyScreen(parent, more));
        else minecraft.setScreen(parent);
    }
}
