package gg.miracle.gui.animation;

/**
 * Easing functions for smooth animations.
 * All functions take a value from 0.0 to 1.0 and return an eased value.
 */
public enum Easing {

    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },

    EASE_IN_QUAD {
        @Override
        public float apply(float t) {
            return t * t;
        }
    },

    EASE_OUT_QUAD {
        @Override
        public float apply(float t) {
            return t * (2 - t);
        }
    },

    EASE_IN_OUT_QUAD {
        @Override
        public float apply(float t) {
            return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
    },

    EASE_IN_CUBIC {
        @Override
        public float apply(float t) {
            return t * t * t;
        }
    },

    EASE_OUT_CUBIC {
        @Override
        public float apply(float t) {
            float t1 = t - 1;
            return t1 * t1 * t1 + 1;
        }
    },

    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            return t < 0.5f
                    ? 4 * t * t * t
                    : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
        }
    },

    EASE_IN_QUART {
        @Override
        public float apply(float t) {
            return t * t * t * t;
        }
    },

    EASE_OUT_QUART {
        @Override
        public float apply(float t) {
            float t1 = t - 1;
            return 1 - t1 * t1 * t1 * t1;
        }
    },

    EASE_IN_OUT_QUART {
        @Override
        public float apply(float t) {
            float t1 = t - 1;
            return t < 0.5f
                    ? 8 * t * t * t * t
                    : 1 - 8 * t1 * t1 * t1 * t1;
        }
    },

    EASE_IN_EXPO {
        @Override
        public float apply(float t) {
            return t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1));
        }
    },

    EASE_OUT_EXPO {
        @Override
        public float apply(float t) {
            return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
        }
    },

    EASE_IN_OUT_EXPO {
        @Override
        public float apply(float t) {
            if (t == 0) return 0;
            if (t == 1) return 1;
            return t < 0.5f
                    ? (float) Math.pow(2, 20 * t - 10) / 2
                    : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
        }
    },

    EASE_IN_BACK {
        @Override
        public float apply(float t) {
            float s = 1.70158f;
            return t * t * ((s + 1) * t - s);
        }
    },

    EASE_OUT_BACK {
        @Override
        public float apply(float t) {
            float s = 1.70158f;
            float t1 = t - 1;
            return t1 * t1 * ((s + 1) * t1 + s) + 1;
        }
    },

    EASE_IN_OUT_BACK {
        @Override
        public float apply(float t) {
            float s = 1.70158f * 1.525f;
            float t1 = t * 2;
            if (t1 < 1) {
                return 0.5f * (t1 * t1 * ((s + 1) * t1 - s));
            }
            t1 -= 2;
            return 0.5f * (t1 * t1 * ((s + 1) * t1 + s) + 2);
        }
    },

    EASE_IN_ELASTIC {
        @Override
        public float apply(float t) {
            if (t == 0 || t == 1) return t;
            return -(float) (Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.1f) * 5 * Math.PI));
        }
    },

    EASE_OUT_ELASTIC {
        @Override
        public float apply(float t) {
            if (t == 0 || t == 1) return t;
            return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.1f) * 5 * Math.PI) + 1);
        }
    },

    BOUNCE_OUT {
        @Override
        public float apply(float t) {
            if (t < 1 / 2.75f) {
                return 7.5625f * t * t;
            } else if (t < 2 / 2.75f) {
                t -= 1.5f / 2.75f;
                return 7.5625f * t * t + 0.75f;
            } else if (t < 2.5 / 2.75f) {
                t -= 2.25f / 2.75f;
                return 7.5625f * t * t + 0.9375f;
            } else {
                t -= 2.625f / 2.75f;
                return 7.5625f * t * t + 0.984375f;
            }
        }
    },

    BOUNCE_IN {
        @Override
        public float apply(float t) {
            return 1 - BOUNCE_OUT.apply(1 - t);
        }
    };

    /**
     * Apply the easing function to a progress value.
     * @param t Progress from 0.0 to 1.0
     * @return Eased value (may exceed 0-1 range for some easings like elastic/back)
     */
    public abstract float apply(float t);
}
