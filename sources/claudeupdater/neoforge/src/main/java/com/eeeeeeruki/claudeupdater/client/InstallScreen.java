package com.eeeeeeruki.claudeupdater.client;

import com.eeeeeeruki.claudeupdater.core.UpdateCandidate;
import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hidden screen (open the update menu while holding the ??? key, Ctrl+Shift by default): lists the mods in
 * the index that are not installed yet and downloads the chosen ones into the mods folder.
 */
public class InstallScreen extends Screen {
    private enum Mode { CHECKING, NOTHING, LIST, DOWNLOADING, FINISHED }

    private static final int ROW_W = 320;
    private static final int ROW_H = 22;
    private static final int TOP = 56;

    private final Screen parent;
    private volatile List<UpdateCandidate> list = new ArrayList<>();
    private volatile Mode mode = Mode.CHECKING;
    private volatile String error;
    private volatile boolean needRebuild;
    private int page;
    private final Map<Button, UpdateCandidate> rows = new LinkedHashMap<>();
    private Button mainButton;

    public InstallScreen(Screen parent) {
        super(Component.literal("MODを追加"));
        this.parent = parent;
        Thread t = new Thread(() -> {
            try {
                list = new ArrayList<>(UpdaterCore.checkInstallable());
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            mode = list.isEmpty() ? Mode.NOTHING : Mode.LIST;
            needRebuild = true;
        }, "ClaudeUpdater-Installable");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void init() {
        rows.clear();
        mainButton = null;
        int x = width / 2 - ROW_W / 2;
        int by = height - 28;

        if (mode == Mode.CHECKING || mode == Mode.NOTHING) {
            addRenderableWidget(Button.builder(Component.literal(mode == Mode.CHECKING ? "戻る" : "閉じる"), b -> onClose())
                    .bounds(width / 2 - 75, by, 150, 20).build());
            return;
        }

        int perPage = perPage();
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page >= pages) page = pages - 1;
        for (int i = page * perPage; i < Math.min(list.size(), (page + 1) * perPage); i++) {
            UpdateCandidate c = list.get(i);
            int y = TOP + (i - page * perPage) * ROW_H;
            Button b = Button.builder(rowLabel(c), btn -> {
                if (mode != Mode.LIST) return;
                c.selected = !c.selected;
                btn.setMessage(rowLabel(c));
                if (mainButton != null) mainButton.active = anySelected();
            }).bounds(x, y, ROW_W, 20).tooltip(Tooltip.create(tooltip(c))).build();
            rows.put(addRenderableWidget(b), c);
        }
        if (pages > 1) {
            int py = TOP + perPage * ROW_H + 2;
            Button prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> { page--; rebuildWidgets(); })
                    .bounds(x, py, 20, 20).build());
            Button next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> { page++; rebuildWidgets(); })
                    .bounds(x + ROW_W - 20, py, 20, 20).build());
            prev.active = page > 0;
            next.active = page < pages - 1;
        }

        if (mode == Mode.LIST) {
            mainButton = addRenderableWidget(Button.builder(Component.literal("選んだMODを入れる"), b -> startDownload())
                    .bounds(width / 2 - 155, by, 150, 20).build());
            mainButton.active = anySelected();
            addRenderableWidget(Button.builder(Component.literal("戻る"), b -> onClose())
                    .bounds(width / 2 + 5, by, 150, 20).build());
        } else if (mode == Mode.FINISHED) {
            boolean any = list.stream().anyMatch(c -> c.state == UpdateCandidate.State.DONE);
            if (any) {
                addRenderableWidget(Button.builder(Component.literal("ゲームを終了する"), b -> minecraft.stop())
                        .bounds(width / 2 - 155, by, 150, 20).build());
                addRenderableWidget(Button.builder(Component.literal("あとで"), b -> onClose())
                        .bounds(width / 2 + 5, by, 150, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                        .bounds(width / 2 - 75, by, 150, 20).build());
            }
        }
    }

    private int perPage() {
        int bottom = height - 28 - 6;
        return Math.max(1, (bottom - TOP - ROW_H) / ROW_H);
    }

    private boolean anySelected() {
        return list.stream().anyMatch(c -> c.selected);
    }

    private void startDownload() {
        mode = Mode.DOWNLOADING;
        rebuildWidgets();
        Thread t = new Thread(() -> {
            UpdaterCore.downloadAll(list);
            mode = Mode.FINISHED;
            needRebuild = true;
        }, "ClaudeUpdater-Install");
        t.setDaemon(true);
        t.start();
    }

    private Component rowLabel(UpdateCandidate c) {
        String ver = "v" + c.release.version;
        if (mode == Mode.LIST) {
            return Component.literal((c.selected ? "☑ " : "☐ ") + c.name + "   §7" + ver);
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

    private Component tooltip(UpdateCandidate c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" v").append(c.release.version);
        if (c.release.released != null) sb.append("（").append(c.release.released).append("）");
        if (c.summary != null && !c.summary.isBlank()) sb.append("\n").append(c.summary);
        sb.append("\n§7").append(c.release.fileName != null ? c.release.fileName : c.id);
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
        if (mode == Mode.DOWNLOADING || mode == Mode.FINISHED) {
            for (Map.Entry<Button, UpdateCandidate> e : rows.entrySet()) {
                e.getKey().setMessage(rowLabel(e.getValue()));
                e.getKey().setTooltip(Tooltip.create(tooltip(e.getValue())));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "MODを追加", width / 2, 14, 0xFFFFFF);
        String l1;
        String l2 = null;
        switch (mode) {
            case CHECKING:
                l1 = "追加できるMODを探しています…";
                break;
            case NOTHING:
                l1 = error != null ? "§c確認できませんでした: " + error : "追加できるMODはありません。すべて入っています。";
                break;
            case LIST:
                l1 = "まだ入っていないMODです。入れたいMODを選んでください。";
                l2 = "カーソルを合わせると説明が出ます。次の起動から使えるようになります。";
                break;
            case DOWNLOADING:
                l1 = "ダウンロードしています…";
                break;
            default:
                boolean ok = list.stream().anyMatch(c -> c.state == UpdateCandidate.State.DONE);
                l1 = ok ? "mods フォルダに入れました。ゲームを起動し直すと使えるようになります。"
                        : "ダウンロードできませんでした。カーソルを合わせると理由が表示されます。";
                break;
        }
        g.drawCenteredString(font, l1, width / 2, 30, 0xCCCCCC);
        if (l2 != null) g.drawCenteredString(font, l2, width / 2, 42, 0x999999);

        if (mode != Mode.CHECKING && mode != Mode.NOTHING) {
            int perPage = perPage();
            int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
            if (pages > 1) {
                g.drawCenteredString(font, (page + 1) + " / " + pages, width / 2, TOP + perPage * ROW_H + 8, 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return mode != Mode.DOWNLOADING;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
