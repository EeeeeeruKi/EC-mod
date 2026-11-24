package io.github.eeeeeeruki.ponderererer.element;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Turns a part of a block entity model (wind generator blades, turbine rotors...) at a set speed.
 * Ponder does not tick block entities, so the angle field the renderer reads is written every frame instead;
 * this keeps the motion smooth. Draws nothing itself.
 */
public class SpinElement extends PonderElementBase implements PonderOverlayElement {
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private final BlockPos pos;
    private final String[] fields;
    private float speed, base;
    private int ticks;

    public SpinElement(BlockPos pos, float degreesPerTick, String... fields) {
        this.pos = pos.immutable();
        this.speed = degreesPerTick;
        this.fields = fields;
        setVisible(true);
    }

    /** Changes the speed without a jump in the angle. */
    public void setSpeed(float degreesPerTick) {
        base += (speed - degreesPerTick) * ticks;
        speed = degreesPerTick;
    }

    @Override
    public void tick(PonderScene scene) {
        ticks++;
    }

    @Override
    public void whileSkipping(PonderScene scene) {
        ticks++;
    }

    @Override
    public void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks) {
        BlockEntity be = scene.getWorld().getBlockEntity(pos);
        if (be == null) return;
        float angle = ((base + speed * (ticks + partialTicks)) % 360f + 360f) % 360f;
        for (String name : fields) {
            Field f = find(be.getClass(), name);
            if (f == null) continue;
            try {
                f.setFloat(be, angle);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private static Field find(Class<?> type, String name) {
        String key = type.getName() + "#" + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                if (f.getType() != float.class) return null;
                f.setAccessible(true);
                FIELDS.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }
}
