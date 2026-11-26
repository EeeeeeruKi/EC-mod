package com.eeeeeeruki.skyriders.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * レールの形。ブロックごとに「アンカー（線が通る点）」があり、となりのレール（斜めも含む）と最大2本つながる。
 * ブロックの中の線は、前のレールとの中点 → アンカー → 次のレールとの中点 の2次ベジェ曲線なので、
 * つなぎ目で向きがそろい、どの位置どうしでもなめらかにつながる。
 */
public final class RailGeometry {
    private RailGeometry() {
    }

    /** 真ん中からの寄せ幅（ブロックの端から 0.18） */
    public static final double OFFSET = 0.32;

    public static Vec3d anchor(BlockState state, BlockPos pos) {
        return new Vec3d(
                pos.getX() + 0.5 + (state.get(RailBlock.OX) - 1) * OFFSET,
                pos.getY() + 0.5 + (state.get(RailBlock.OY) - 1) * OFFSET,
                pos.getZ() + 0.5 + (state.get(RailBlock.OZ) - 1) * OFFSET);
    }

    /** ブロックの中の曲線。a 側（t=0）から b 側（t=1）へ */
    public record Curve(Vec3d s, Vec3d p, Vec3d e, @Nullable BlockPos a, @Nullable BlockPos b) {
        public Vec3d point(double t) {
            double u = 1 - t;
            return this.s.multiply(u * u).add(this.p.multiply(2 * u * t)).add(this.e.multiply(t * t));
        }

        public Vec3d tangent(double t) {
            Vec3d d = this.p.subtract(this.s).multiply(2 * (1 - t)).add(this.e.subtract(this.p).multiply(2 * t));
            return d.lengthSquared() < 1e-8 ? this.e.subtract(this.s) : d;
        }

        public double length() {
            double len = 0;
            Vec3d prev = this.s;
            for (int i = 1; i <= 8; i++) {
                Vec3d q = point(i / 8.0);
                len += q.distanceTo(prev);
                prev = q;
            }
            return Math.max(len, 0.05);
        }
    }

    public static boolean isRail(BlockView world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof RailBlock;
    }

    /** となりのレールから、つながる2本（無ければ1本・0本）を選ぶ。まっすぐ・面でとなり合うものを優先 */
    public static BlockPos[] links(BlockView world, BlockPos pos) {
        BlockPos[] cand = new BlockPos[26];
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos q = pos.add(dx, dy, dz);
                    if (isRail(world, q)) cand[n++] = q;
                }
            }
        }
        BlockPos bestA = null, bestB = null;
        double best = -1e9;
        for (int i = 0; i < n; i++) {
            Vec3d di = dir(pos, cand[i]);
            int pi = prio(pos, cand[i]);
            for (int j = i + 1; j < n; j++) {
                Vec3d dj = dir(pos, cand[j]);
                double dot = di.normalize().dotProduct(dj.normalize());
                if (dot > 0.01) continue; // 90度より急な折り返しはつながない
                double score = -dot - 0.3 * (pi + prio(pos, cand[j]) - 2);
                if (score > best) {
                    best = score;
                    bestA = cand[i];
                    bestB = cand[j];
                }
            }
        }
        if (bestA == null && n > 0) {
            // 1本だけ：一番近いもの
            int bp = 99;
            for (int i = 0; i < n; i++) {
                int pi = prio(pos, cand[i]);
                if (pi < bp) {
                    bp = pi;
                    bestA = cand[i];
                }
            }
        }
        if (bestA != null && bestB != null && compare(bestA, bestB) > 0) {
            BlockPos t = bestA;
            bestA = bestB;
            bestB = t;
        }
        return new BlockPos[]{bestA, bestB};
    }

    private static Vec3d dir(BlockPos from, BlockPos to) {
        return new Vec3d(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private static int prio(BlockPos from, BlockPos to) {
        return Math.abs(to.getX() - from.getX()) + Math.abs(to.getY() - from.getY()) + Math.abs(to.getZ() - from.getZ());
    }

    /** a/b の順番を決める（どちらから見ても同じになるように） */
    private static int compare(BlockPos x, BlockPos y) {
        if (x.getX() != y.getX()) return Integer.compare(x.getX(), y.getX());
        if (x.getY() != y.getY()) return Integer.compare(x.getY(), y.getY());
        return Integer.compare(x.getZ(), y.getZ());
    }

    public static Curve curve(BlockView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Vec3d p = anchor(state, pos);
        BlockPos[] l = links(world, pos);
        BlockPos a = l[0], b = l[1];
        Vec3d s, e;
        if (a != null && b != null) {
            s = mid(world, a, p);
            e = mid(world, b, p);
        } else if (a != null) {
            s = mid(world, a, p);
            Vec3d d = p.subtract(s);
            e = p.add(d.lengthSquared() < 1e-6 ? facing(state).multiply(0.5) : d.normalize().multiply(0.5));
        } else {
            Vec3d f = facing(state).multiply(0.5);
            s = p.subtract(f);
            e = p.add(f);
        }
        return new Curve(s, p, e, a, b);
    }

    private static Vec3d mid(BlockView world, BlockPos other, Vec3d p) {
        return anchor(world.getBlockState(other), other).add(p).multiply(0.5);
    }

    private static Vec3d facing(BlockState state) {
        Direction d = state.get(RailBlock.FACING);
        return new Vec3d(d.getOffsetX(), 0, d.getOffsetZ());
    }
}
