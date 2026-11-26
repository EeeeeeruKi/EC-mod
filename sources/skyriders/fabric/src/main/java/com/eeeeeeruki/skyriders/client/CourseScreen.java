package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.net.CourseEditPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** コースの管理画面（コース編集ツールの右クリックで開く） */
public class CourseScreen extends Screen {
    private JsonObject data;
    private TextFieldWidget nameField;
    private String typed = "";

    private CourseScreen(JsonObject data) {
        super(Text.translatable("screen.skyriders.course.title"));
        this.data = data;
    }

    /** サーバーから中身が届いた：開いていれば更新、無ければ開く */
    public static void receive(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof CourseScreen s) {
            s.data = obj;
            s.typed = s.nameField != null ? s.nameField.getText() : "";
            s.clearAndInit();
        } else {
            mc.setScreen(new CourseScreen(obj));
        }
    }

    private static void send(int action, String text, int value) {
        ClientPlayNetworking.send(new CourseEditPayload(action, text, value));
    }

    private String selected() {
        return this.data.get("selected").getAsString();
    }

    private JsonObject selectedCourse() {
        for (JsonElement e : this.data.getAsJsonArray("courses")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("name").getAsString().equals(selected())) return o;
        }
        return new JsonObject();
    }

    private List<String> courseNames() {
        List<String> names = new ArrayList<>();
        for (JsonElement e : this.data.getAsJsonArray("courses")) names.add(e.getAsJsonObject().get("name").getAsString());
        return names;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 34;

        // コースを切り替える
        List<String> names = courseNames();
        int idx = Math.max(0, names.indexOf(selected()));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> {
            if (!names.isEmpty()) send(CourseEditPayload.SELECT, names.get((idx - 1 + names.size()) % names.size()), 0);
        }).dimensions(cx - 110, y, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> {
            if (!names.isEmpty()) send(CourseEditPayload.SELECT, names.get((idx + 1) % names.size()), 0);
        }).dimensions(cx + 90, y, 20, 20).build());

        // 新しいコースを作る
        y += 26;
        this.nameField = new TextFieldWidget(this.textRenderer, cx - 110, y, 150, 20, Text.translatable("screen.skyriders.course.new_name"));
        this.nameField.setMaxLength(32);
        this.nameField.setPlaceholder(Text.translatable("screen.skyriders.course.new_name"));
        this.nameField.setText(this.typed);
        this.addDrawableChild(this.nameField);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.skyriders.course.create"), b -> {
            String t = this.nameField.getText().strip();
            if (!t.isEmpty()) {
                this.nameField.setText("");
                send(CourseEditPayload.SELECT, t, 0);
            }
        }).dimensions(cx + 44, y, 66, 20).build());

        // 周回数
        y += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> send(CourseEditPayload.LAPS, "", -1))
                .dimensions(cx + 30, y, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> send(CourseEditPayload.LAPS, "", 1))
                .dimensions(cx + 90, y, 20, 20).build());

        // 置くチェックポイントの番号
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> send(CourseEditPayload.NUMBER, "", -1))
                .dimensions(cx + 30, y, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> send(CourseEditPayload.NUMBER, "", 1))
                .dimensions(cx + 90, y, 20, 20).build());

        // 下のボタン
        int by = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.skyriders.course.reset"),
                b -> send(CourseEditPayload.RESET_RECORDS, "", 0)).dimensions(cx - 155, by, 100, 20).build());
        ButtonWidget cancel = ButtonWidget.builder(Text.translatable("screen.skyriders.course.cancel_race"),
                b -> send(CourseEditPayload.CANCEL_RACE, "", 0)).dimensions(cx - 50, by, 100, 20).build();
        cancel.active = this.data.get("racing").getAsBoolean();
        this.addDrawableChild(cancel);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
                .dimensions(cx + 55, by, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        JsonObject c = selectedCourse();
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, 0xFFFFFF);

        int y = 34;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.course", selected()), cx, y + 6, 0xFFE070);

        y += 56;
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.laps"), cx - 110, y + 6, 0xFFFFFF);
        String laps = c.has("laps") ? c.get("laps").getAsString() : "3";
        ctx.drawCenteredTextWithShadow(this.textRenderer, laps, cx + 70, y + 6, 0xFFFFFF);

        y += 24;
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.number"), cx - 110, y + 6, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.data.get("number").getAsString(), cx + 70, y + 6, 0xFFFFFF);

        // コースの中身
        y += 30;
        int goals = c.has("goals") ? c.get("goals").getAsInt() : 0;
        JsonArray cps = c.has("checkpoints") ? c.getAsJsonArray("checkpoints") : new JsonArray();
        JsonArray missing = c.has("missing") ? c.getAsJsonArray("missing") : new JsonArray();
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.goals", goals), cx - 110, y, goals > 0 ? 0xA0FFA0 : 0xFF8080);
        y += 12;
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.checkpoints",
                cps.isEmpty() ? "-" : join(cps)), cx - 110, y, cps.isEmpty() ? 0xFF8080 : 0xA0FFA0);
        if (!missing.isEmpty()) {
            y += 12;
            ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.missing", join(missing)), cx - 110, y, 0xFF8080);
        }

        // ベストタイム
        y += 18;
        ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.records"), cx - 110, y, 0xFFE070);
        JsonArray recs = c.has("records") ? c.getAsJsonArray("records") : new JsonArray();
        if (recs.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skyriders.course.no_records"), cx - 100, y + 12, 0xA0A0A0);
        }
        int maxRows = Math.max(1, (this.height - 40 - (y + 12)) / 11);
        for (int i = 0; i < recs.size() && i < maxRows; i++) {
            JsonObject r = recs.get(i).getAsJsonObject();
            String line = (i + 1) + ". " + r.get("time").getAsString() + "  " + r.get("player").getAsString();
            ctx.drawTextWithShadow(this.textRenderer, line, cx - 100, y + 12 + i * 11, i == 0 ? 0xFFD040 : 0xFFFFFF);
        }
    }

    private static String join(JsonArray a) {
        StringBuilder sb = new StringBuilder();
        for (JsonElement e : a) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(e.getAsInt());
        }
        return sb.toString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
