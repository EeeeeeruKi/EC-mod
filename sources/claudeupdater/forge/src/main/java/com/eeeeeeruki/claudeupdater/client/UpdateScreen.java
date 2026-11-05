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

/** Lists the mods that have a newer version, lets the player pick which to download, and shows progress. */
public class UpdateScreen extends Screen {
    private enum Mode { CHECKING, NOTHING, LIST, DOWNLOADING, FINISHED }

    private static final int ROW_W = 320;
    private static final int ROW_H = 22;
    private static final int TOP = 56;

    private final Screen parent;
    private final boolean manual; // opened from the 設定 button: hidden mods are listed too
    private volatile List<UpdateCandidate> list;
    private volatile Mode mode;
    private volatile String error;
    private int page;
    private final Map<Button, UpdateCandidate> rows = new LinkedHashMap<>();
    private Button mainButton;

    /** Shows the result of the startup check. */
    public UpdateScreen(Screen parent, List<UpdateCandidate> found) {
        super(Component.literal("MODの更新"));
        this.parent = parent;
        this.manual = false;
        this.list = new ArrayList<>(found);
        this.mode = Mode.LIST;
    }

    /** Checks again right now (opened from the mod list's 設定 button). */
    public UpdateScreen(Screen parent) {
        super(Component.literal("MODの更新"));
        this.parent = parent;
        this.manual = true;
        this.list = new ArrayList<>();
        this.mode = Mode.CHECKING;
        Thread t = new Thread(() -> {
            try {
                list = new ArrayList<>(UpdaterCore.check(UpdaterCore.installed(), true));
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
                list = new ArrayList<>();
            }
            mode = list.isEmpty() ? Mode.NOTHING : Mode.LIST;
            needRebuild = true;
        }, "ClaudeUpdater-Recheck");
        t.setDaemon(true);
        t.start();
    }

    private volatile boolean needRebuild;

    @Override
    protected void init() {
        rows.clear();
        mainButton = null;
        int x = width / 2 - ROW_W / 2;
        int by = height - 28;

        if (mode == Mode.CHECKING) {
            addRenderableWidget(Button.builder(Component.literal("戻る"), b -> onClose())
                    .bounds(width / 2 - 75, by, 150, 20).build());
            return;
        }
        if (mode == Mode.NOTHING) {
            addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
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
            mainButton = addRenderableWidget(Button.builder(Component.literal("選んだMODを更新"), b -> startDownload())
                    .bounds(width / 2 - 155, by, 150, 20).build());
            mainButton.active = anySelected();
            addRenderableWidget(Button.builder(Component.literal("今回は更新しない"), b -> onClose())
                    .bounds(width / 2 + 5, by, 150, 20).build());
            if (!manual) {
                addRenderableWidget(Button.builder(Component.literal("二度と表示しない"), b -> hideAll())
                        .bounds(width / 2 - 155, by - 24, 310, 20)
                        .tooltip(Tooltip.create(Component.literal(
                                "ここにあるMODの更新は、起動時に二度と表示しません。\n"
                                + "あとから新しく入れたMODに更新があるときは、そのMODだけが表示されます。\n"
                                + "§7タイトル画面で Ctrl+U を押すか、更新ボタン（矢印のアイコン）から、いつでも確認できます。")))
                        .build());
            } else if (list.stream().anyMatch(UpdaterCore::isHidden)) {
                addRenderableWidget(Button.builder(Component.literal("起動時にまた表示する"), b -> unhideAll())
                        .bounds(width / 2 - 155, by - 24, 310, 20)
                        .tooltip(Tooltip.create(Component.literal("「二度と表示しない」にしたMODを、起動時の更新画面にまた表示します。")))
                        .build());
            }
        } else if (mode == Mode.FINISHED) {
            boolean any = list.stream().anyMatch(c -> c.state == UpdateCandidate.State.DONE);
            if (any) {
                addRenderableWidget(Button.builder(Component.literal("ゲームを終了して入れ替える"), b -> minecraft.stop())
                        .bounds(width / 2 - 155, by, 150, 20).build());
                addRenderableWidget(Button.builder(Component.literal("あとで（終了時に入れ替え）"), b -> onClose())
                        .bounds(width / 2 + 5, by, 150, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose())
                        .bounds(width / 2 - 75, by, 150, 20).build());
            }
        }
        // DOWNLOADING: no buttons, the rows show progress
    }

    private int perPage() {
        int bottom = height - 28 - 24 - 6; // keep room for the two button rows
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
        }, "ClaudeUpdater-Download");
        t.setDaemon(true);
        t.start();
    }

    private void hideAll() {
        UpdaterCore.hide(list);
        onClose();
    }

    private void unhideAll() {
        UpdaterCore.unhide(list);
        rebuildWidgets();
    }

    private Component rowLabel(UpdateCandidate c) {
        String ver = c.installed.version() + " → " + c.release.version;
        switch (mode) {
            case LIST:
                return Component.literal((c.selected ? "☑ " : "☐ ") + c.name + "   " + ver
                        + (manual && UpdaterCore.isHidden(c) ? "   §8起動時は非表示" : ""));
            default:
                if (!c.selected) return Component.literal("§7" + c.name + "   更新しません");
                String s;
                switch (c.state) {
                    case WAITING: s = "§7待機中"; break;
                    case DOWNLOADING: s = "§eダウンロード中…"; break;
                    case DONE: s = "§a完了"; break;
                    default: s = "§c失敗しました"; break;
                }
                return Component.literal(c.name + "   " + ver + "   " + s);
        }
    }

    private Component tooltip(UpdateCandidate c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" ").append(c.release.version);
        if (c.release.released != null) sb.append("（").append(c.release.released).append("）");
        if (c.release.changelog != null && !c.release.changelog.isBlank()) sb.append("\n").append(c.release.changelog);
        sb.append("\n§7今のファイル: ").append(c.installed.jar().getFileName());
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
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "MODの更新", width / 2, 14, 0xFFFFFF);
        String l1;
        String l2 = null;
        switch (mode) {
            case CHECKING:
                l1 = "新しい版があるか確認しています…";
                break;
            case NOTHING:
                l1 = error != null ? "§c確認できませんでした: " + error : "すべてのMODが最新です。";
                break;
            case LIST:
                l1 = "新しい版が見つかりました。更新するMODを選んでください。";
                l2 = "入れ替えはゲームを終了したあとに行われ、次の起動から新しい版になります。";
                break;
            case DOWNLOADING:
                l1 = "ダウンロードしています…";
                break;
            default:
                boolean ok = list.stream().anyMatch(c -> c.state == UpdateCandidate.State.DONE);
                l1 = ok ? "ダウンロードが終わりました。ゲームを終了すると新しい版に入れ替わります。"
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
