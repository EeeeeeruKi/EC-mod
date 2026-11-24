package io.github.eeeeeeruki.ponderererer.world;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Makes cables and pipes (Mekanism transmitters) show their connections inside a Ponder scene.
 *
 * <p>In a real world the server works out which sides a transmitter is connected to and sends it
 * to the client. A Ponder world has no server, so every cable is drawn as a bare core. This class
 * does the same work the client would receive: for each transmitter it looks at the six neighbours
 * and hands the result to the block entity through its normal update-tag path. No compile-time
 * dependency on Mekanism is needed; other mods that use the same tag keys work too.</p>
 *
 * <ul>
 *   <li>a neighbour of the same transmitter family (cable–cable, pipe–pipe, any transporter–transporter)
 *       becomes a transmitter connection,</li>
 *   <li>a neighbour with a block entity that is not a transmitter (machines, tanks, chests, energy cubes)
 *       becomes an acceptor connection,</li>
 *   <li>everything else stays unconnected.</li>
 * </ul>
 */
public final class TransmitterLinker {
    private static final String TRANSMITTER_BASE = "TileEntityTransmitter";

    private TransmitterLinker() {}

    /** Updates every transmitter inside {@code area}. Returns how many were updated. */
    public static int link(PonderLevel world, Selection area) {
        int[] count = {0};
        area.forEach(pos -> {
            if (linkOne(world, pos.immutable())) count[0]++;
        });
        return count[0];
    }

    /** Updates one transmitter. Returns false when there is no transmitter at {@code pos}. */
    public static boolean linkOne(PonderLevel world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        Class<?> family = family(be);
        if (family == null) return false;
        byte connections = 0, acceptors = 0;
        for (Direction side : Direction.values()) {
            BlockEntity other = world.getBlockEntity(pos.relative(side));
            if (other == null) continue;
            Class<?> otherFamily = family(other);
            if (otherFamily == null) acceptors |= (byte) (1 << side.ordinal());
            else if (otherFamily == family) connections |= (byte) (1 << side.ordinal());
        }
        return set(world, be, connections, acceptors);
    }

    /**
     * Sets the connected sides by hand, for layouts the automatic rule gets wrong
     * (e.g. a pipe that should not connect to a machine next to it).
     */
    public static boolean set(PonderLevel world, BlockPos pos, Direction[] transmitterSides, Direction[] acceptorSides) {
        BlockEntity be = world.getBlockEntity(pos);
        if (family(be) == null) return false;
        byte connections = 0, acceptors = 0;
        for (Direction d : transmitterSides) connections |= (byte) (1 << d.ordinal());
        for (Direction d : acceptorSides) acceptors |= (byte) (1 << d.ordinal());
        return set(world, be, connections, acceptors);
    }

    private static boolean set(PonderLevel world, BlockEntity be, byte connections, byte acceptors) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("connections", connections);
        tag.putByte("acceptors", acceptors);
        // keep a painted transporter's colour: a tag without "color" would clear it
        try {
            CompoundTag current = be.getUpdateTag(world.registryAccess());
            if (current.contains("color")) tag.put("color", current.get("color").copy());
        } catch (RuntimeException ignored) {
        }
        try {
            be.handleUpdateTag(tag, world.registryAccess());
            be.requestModelDataUpdate();
            LiveModelData.mark(world, be.getBlockPos());
            return true;
        } catch (RuntimeException e) {
            return false; // never let a cable break the scene
        }
    }

    /**
     * The transmitter "family" is the class right below the shared transmitter base class,
     * so all tiers of a cable match, and logistical / diversion / restrictive transporters match each other.
     * Returns null for anything that is not a transmitter.
     */
    private static Class<?> family(BlockEntity be) {
        if (be == null) return null;
        Class<?> below = null;
        for (Class<?> c = be.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getSimpleName().equals(TRANSMITTER_BASE)) return below;
            below = c;
        }
        return null;
    }
}
