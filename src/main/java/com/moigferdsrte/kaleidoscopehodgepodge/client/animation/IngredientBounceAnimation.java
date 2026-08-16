package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

public final class IngredientBounceAnimation {
    public static final long DURATION_MILLIS = 750L;
    private static final double OSCILLATIONS = 2.5;
    private static final double DAMPING = 4.5;
    private static final float INITIAL_COMPRESSION = 0.25F;

    public static Scale sample(long elapsedMillis) {
        if (elapsedMillis < 0L || elapsedMillis >= DURATION_MILLIS) return Scale.IDENTITY;
        double progress = (double) elapsedMillis / DURATION_MILLIS;
        double envelope = Math.exp(-DAMPING * progress) * (1.0 - progress);
        float displacement = (float) (INITIAL_COMPRESSION * envelope
                * Math.cos(progress * Math.PI * 2.0 * OSCILLATIONS));
        float vertical = 1.0F - displacement;
        float horizontal = 1.0F + displacement * 0.24F;
        return new Scale(horizontal, vertical);
    }

    public record Scale(float horizontal, float vertical) {
        public static final Scale IDENTITY = new Scale(1.0F, 1.0F);
    }

    private IngredientBounceAnimation() {
    }
}
