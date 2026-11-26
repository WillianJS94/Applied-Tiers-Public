package io.github.willianjs94.appliedtiers.blockentity;

import java.util.ArrayDeque;
import java.util.Deque;

import appeng.api.stacks.AEItemKey;
import net.minecraft.world.item.ItemStack;

/** A transient, bounded queue of completed results for a single assembler on the client. */
public final class AssemblerAnimation {
    private static final int MAX_PENDING = 4;
    private static final int MAX_PENDING_AGE = 48;
    public static final int MAX_SPEED = 50;

    public record Visual(AEItemKey key, ItemStack item, int speed, long receivedAt) {
        int duration() {
            return (100 + speed - 1) / speed + 2;
        }
    }

    private final Deque<Visual> pending = new ArrayDeque<>();
    private Visual active;
    private long expiresAt;
    private long nextParticles;

    public void accept(ItemStack item, int speed, long now) {
        var key = AEItemKey.of(item);
        if (key == null || speed <= 0 || speed > MAX_SPEED) {
            return;
        }

        advance(now);
        var visual = new Visual(key, item.copyWithCount(1), speed, now);
        if (active == null) {
            start(visual, now);
        } else if (active.key().equals(key)) {
            if (pending.isEmpty()) {
                expiresAt = Math.max(expiresAt, now + visual.duration());
                active = visual;
            }
        } else if (pending.size() < MAX_PENDING && pending.stream().noneMatch(entry -> entry.key().equals(key))) {
            pending.addLast(visual);
        }
    }

    public Visual current(long now) {
        advance(now);
        return active;
    }

    public boolean takeParticleBurst(long now) {
        if (active == null || now < nextParticles) {
            return false;
        }
        nextParticles = now + 4;
        return true;
    }

    private void advance(long now) {
        if (active != null && now >= expiresAt) {
            active = null;
        }
        pending.removeIf(entry -> now - entry.receivedAt() >= MAX_PENDING_AGE);
        if (active == null && !pending.isEmpty()) {
            start(pending.removeFirst(), now);
        }
    }

    private void start(Visual visual, long now) {
        active = visual;
        expiresAt = now + visual.duration();
    }
}
