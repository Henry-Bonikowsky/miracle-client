package gg.miracle.gui.theme;

/**
 * Dynamic theme system for Miracle Client's Premium Dark UI.
 * Supports all 10 launcher themes with CSS-like color variables.
 */
public final class MiracleTheme {

    private MiracleTheme() {} // Prevent instantiation

    // ==================== THEME ENUM ====================

    public enum ThemeId {
        MIDNIGHT("Midnight", "Deep blue ocean vibes"),
        AMETHYST("Amethyst", "Royal purple elegance"),
        FOREST("Forest", "Natural emerald greens"),
        SUNSET("Sunset", "Warm amber glow"),
        ROSE("Rose", "Soft pink warmth"),
        CRIMSON("Crimson", "Bold red intensity"),
        SLATE("Slate", "Clean monochrome"),
        NEON("Neon", "Electric cyan glow"),
        LIGHT("Light", "Clean and bright"),
        HIGH_CONTRAST("High Contrast", "Maximum visibility");

        private final String displayName;
        private final String description;

        ThemeId(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ==================== THEME DATA CLASS ====================

    public static class Theme {
        // Accent color scale
        public final int accent50, accent100, accent200, accent300, accent400;
        public final int accent500, accent600, accent700, accent800, accent900, accent950;
        // Background gradient
        public final int bgFrom, bgTo;
        // Glass effect
        public final int glassBg, glassBorder;
        // Text colors
        public final int textPrimary, textSecondary, textMuted;
        // Surface colors
        public final int surfacePrimary, surfaceSecondary;

        public Theme(
            int accent50, int accent100, int accent200, int accent300, int accent400,
            int accent500, int accent600, int accent700, int accent800, int accent900, int accent950,
            int bgFrom, int bgTo,
            int glassBg, int glassBorder,
            int textPrimary, int textSecondary, int textMuted,
            int surfacePrimary, int surfaceSecondary
        ) {
            this.accent50 = accent50;
            this.accent100 = accent100;
            this.accent200 = accent200;
            this.accent300 = accent300;
            this.accent400 = accent400;
            this.accent500 = accent500;
            this.accent600 = accent600;
            this.accent700 = accent700;
            this.accent800 = accent800;
            this.accent900 = accent900;
            this.accent950 = accent950;
            this.bgFrom = bgFrom;
            this.bgTo = bgTo;
            this.glassBg = glassBg;
            this.glassBorder = glassBorder;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.textMuted = textMuted;
            this.surfacePrimary = surfacePrimary;
            this.surfaceSecondary = surfaceSecondary;
        }
    }

    // ==================== THEME DEFINITIONS ====================

    private static final Theme MIDNIGHT_THEME = new Theme(
        0xFFF0F9FF, 0xFFE0F2FE, 0xFFBAE6FD, 0xFF7DD3FC, 0xFF38BDF8,  // accent 50-400
        0xFF0EA5E9, 0xFF0284C7, 0xFF0369A1, 0xFF075985, 0xFF0C4A6E, 0xFF082F49,  // accent 500-950
        0xFF060A10, 0xFF0C4A6E,  // background gradient
        0x800C1E32, 0x1F38BDF8,  // glass bg (50% opacity), border (12% opacity)
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,  // text colors
        0xFF18181B, 0xFF27272A   // surface colors
    );

    private static final Theme AMETHYST_THEME = new Theme(
        0xFFFAF5FF, 0xFFF3E8FF, 0xFFE9D5FF, 0xFFD8B4FE, 0xFFC084FC,
        0xFFA855F7, 0xFF9333EA, 0xFF7E22CE, 0xFF6B21A8, 0xFF581C87, 0xFF3B0764,
        0xFF0C0610, 0xFF581C87,
        0x801E0F28, 0x26A855F7,  // 15% border
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme FOREST_THEME = new Theme(
        0xFFECFDF5, 0xFFD1FAE5, 0xFFA7F3D0, 0xFF6EE7B7, 0xFF34D399,
        0xFF10B981, 0xFF059669, 0xFF047857, 0xFF065F46, 0xFF064E3B, 0xFF022C22,
        0xFF060F0C, 0xFF064E3B,
        0x800A231C, 0x2610B981,
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme SUNSET_THEME = new Theme(
        0xFFFFFBEB, 0xFFFEF3C7, 0xFFFDE68A, 0xFFFCD34D, 0xFFFBBF24,
        0xFFF59E0B, 0xFFD97706, 0xFFB45309, 0xFF92400E, 0xFF78350F, 0xFF451A03,
        0xFF100A06, 0xFF78350F,
        0x8028190C, 0x26F59E0B,
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme ROSE_THEME = new Theme(
        0xFFFFF1F2, 0xFFFFE4E6, 0xFFFECDD3, 0xFFFDA4AF, 0xFFFB7185,
        0xFFF43F5E, 0xFFE11D48, 0xFFBE123C, 0xFF9F1239, 0xFF881337, 0xFF4C0519,
        0xFF10060A, 0xFF881337,
        0x80280F19, 0x26F43F5E,
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme CRIMSON_THEME = new Theme(
        0xFFFEF2F2, 0xFFFEE2E2, 0xFFFECACA, 0xFFFCA5A5, 0xFFF87171,
        0xFFEF4444, 0xFFDC2626, 0xFFB91C1C, 0xFF991B1B, 0xFF7F1D1D, 0xFF450A0A,
        0xFF100606, 0xFF7F1D1D,
        0x80280C0C, 0x26EF4444,
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme SLATE_THEME = new Theme(
        0xFFF8FAFC, 0xFFF1F5F9, 0xFFE2E8F0, 0xFFCBD5E1, 0xFF94A3B8,
        0xFF64748B, 0xFF475569, 0xFF334155, 0xFF1E293B, 0xFF0F172A, 0xFF020617,
        0xFF08090C, 0xFF1E293B,
        0x80141923, 0x2694A3B8,
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme NEON_THEME = new Theme(
        0xFFECFEFF, 0xFFCFFAFE, 0xFFA5F3FC, 0xFF67E8F9, 0xFF22D3EE,
        0xFF06B6D4, 0xFF0891B2, 0xFF0E7490, 0xFF155E75, 0xFF164E63, 0xFF083344,
        0xFF060C10, 0xFF164E63,
        0x800A1E28, 0x3306B6D4,  // 20% border for neon
        0xFFFFFFFF, 0xFFA1A1AA, 0xFF71717A,
        0xFF18181B, 0xFF27272A
    );

    private static final Theme LIGHT_THEME = new Theme(
        0xFFF0F9FF, 0xFFE0F2FE, 0xFFBAE6FD, 0xFF7DD3FC, 0xFF38BDF8,
        0xFF0284C7, 0xFF0369A1, 0xFF075985, 0xFF0C4A6E, 0xFF082F49, 0xFF041E36,
        0xFFF8FAFC, 0xFFE0F2FE,
        0xCCFFFFFF, 0x260F172A,  // 80% bg, 15% border
        0xFF0F172A, 0xFF334155, 0xFF64748B,  // dark text for light theme
        0xFFFFFFFF, 0xFFF1F5F9
    );

    private static final Theme HIGH_CONTRAST_THEME = new Theme(
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFEF08A, 0xFFFACC15,
        0xFFEAB308, 0xFFCA8A04, 0xFFA16207, 0xFF854D0E, 0xFF713F12, 0xFF422006,
        0xFF000000, 0xFF000000,
        0xE6000000, 0x80FFFFFF,  // 90% bg, 50% border
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFD4D4D8,
        0xFF000000, 0xFF18181B
    );

    // ==================== CURRENT THEME STATE ====================

    private static ThemeId currentThemeId = ThemeId.MIDNIGHT;
    private static Theme currentTheme = MIDNIGHT_THEME;

    public static ThemeId getCurrentThemeId() {
        return currentThemeId;
    }

    public static Theme current() {
        return currentTheme;
    }

    public static void setTheme(ThemeId themeId) {
        currentThemeId = themeId;
        currentTheme = getThemeData(themeId);
    }

    public static void setTheme(String themeName) {
        try {
            setTheme(ThemeId.valueOf(themeName.toUpperCase().replace("-", "_").replace(" ", "_")));
        } catch (IllegalArgumentException e) {
            setTheme(ThemeId.MIDNIGHT);
        }
    }

    public static Theme getThemeData(ThemeId id) {
        return switch (id) {
            case MIDNIGHT -> MIDNIGHT_THEME;
            case AMETHYST -> AMETHYST_THEME;
            case FOREST -> FOREST_THEME;
            case SUNSET -> SUNSET_THEME;
            case ROSE -> ROSE_THEME;
            case CRIMSON -> CRIMSON_THEME;
            case SLATE -> SLATE_THEME;
            case NEON -> NEON_THEME;
            case LIGHT -> LIGHT_THEME;
            case HIGH_CONTRAST -> HIGH_CONTRAST_THEME;
        };
    }

    public static ThemeId[] getAllThemes() {
        return ThemeId.values();
    }

    // ==================== ANIMATION TIMING (ms) ====================

    /** Quick transitions (hover) */
    public static final int ANIM_QUICK = 150;

    /** Normal transitions (toggle, slide) */
    public static final int ANIM_NORMAL = 200;

    /** Slow transitions (expand/collapse) */
    public static final int ANIM_SLOW = 250;

    /** Glow pulse duration */
    public static final int ANIM_PULSE = 2000;

    // ==================== DIMENSIONS ====================

    /** Default corner radius */
    public static final int RADIUS_SMALL = 4;

    /** Medium corner radius */
    public static final int RADIUS_MEDIUM = 8;

    /** Large corner radius */
    public static final int RADIUS_LARGE = 12;

    /** Toggle switch width */
    public static final int TOGGLE_WIDTH = 36;

    /** Toggle switch height */
    public static final int TOGGLE_HEIGHT = 18;

    /** Slider height */
    public static final int SLIDER_HEIGHT = 6;

    /** Slider thumb size */
    public static final int SLIDER_THUMB = 14;

    // ==================== UTILITY METHODS ====================

    /**
     * Interpolate between two colors.
     * @param color1 Starting color (ARGB)
     * @param color2 Ending color (ARGB)
     * @param progress Progress from 0.0 to 1.0
     * @return Interpolated color
     */
    public static int lerpColor(int color1, int color2, float progress) {
        progress = Math.max(0, Math.min(1, progress));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Apply alpha to a color.
     * @param color Base color (ARGB or RGB)
     * @param alpha Alpha value 0-255
     * @return Color with new alpha
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Get a darkened version of a color.
     * @param color Base color
     * @param amount Darkening amount (0.0 to 1.0)
     * @return Darkened color
     */
    public static int darken(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * (1 - amount));
        int g = (int) (((color >> 8) & 0xFF) * (1 - amount));
        int b = (int) ((color & 0xFF) * (1 - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Get a brightened version of a color.
     * @param color Base color
     * @param amount Brightening amount (0.0 to 1.0)
     * @return Brightened color
     */
    public static int brighten(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (int) Math.min(255, ((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * amount);
        int g = (int) Math.min(255, ((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * amount);
        int b = (int) Math.min(255, (color & 0xFF) + (255 - (color & 0xFF)) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================== LEGACY COMPATIBILITY ====================
    // These constants are kept for backwards compatibility during migration

    /** @deprecated Use current().accent500 instead */
    @Deprecated public static final int GOLD_PRIMARY = 0xFFD4AF37;
    /** @deprecated Use current().accent400 instead */
    @Deprecated public static final int GOLD_LIGHT = 0xFFF4E5B0;
    /** @deprecated Use current().accent600 instead */
    @Deprecated public static final int GOLD_DARK = 0xFFB8962E;
    /** @deprecated Use current().accent400 instead */
    @Deprecated public static final int CYAN_ACCENT = 0xFF00D9FF;
    /** @deprecated Use withAlpha(current().accent400, 0x66) instead */
    @Deprecated public static final int CYAN_GLOW = 0x6600D9FF;
    /** @deprecated Use current().accent300 instead */
    @Deprecated public static final int CYAN_BRIGHT = 0xFF00F0FF;
    /** @deprecated Use current().surfacePrimary with alpha instead */
    @Deprecated public static final int SURFACE_DARK = 0xF00D0D1A;
    /** @deprecated Use current().surfaceSecondary with alpha instead */
    @Deprecated public static final int SURFACE_MEDIUM = 0xF0151525;
    /** @deprecated Use current().surfaceSecondary instead */
    @Deprecated public static final int SURFACE_LIGHT = 0xF01E1E32;
    /** @deprecated Use current().surfacePrimary instead */
    @Deprecated public static final int SURFACE_SETTING = 0xFF141422;
    /** @deprecated Use current().textPrimary instead */
    @Deprecated public static final int TEXT_PRIMARY = 0xFFF5F5F5;
    /** @deprecated Use current().textSecondary instead */
    @Deprecated public static final int TEXT_SECONDARY = 0xFFB0B0C0;
    /** @deprecated Use current().textMuted instead */
    @Deprecated public static final int TEXT_MUTED = 0xFF606080;
    /** @deprecated Use current().accent400 instead */
    @Deprecated public static final int TEXT_GOLD = 0xFFE8C869;
    /** @deprecated Use 0xFF00E676 directly or theme accent */
    @Deprecated public static final int ENABLED = 0xFF00E676;
    /** @deprecated Use withAlpha(0x00E676, 0x66) */
    @Deprecated public static final int ENABLED_GLOW = 0x6600E676;
    /** @deprecated Use current().surfacePrimary instead */
    @Deprecated public static final int DISABLED = 0xFF3A3A50;
    /** @deprecated Use brighten(current().surfacePrimary, 0.1f) instead */
    @Deprecated public static final int HOVER = 0xFF252540;
    /** @deprecated Use brighten(current().surfacePrimary, 0.15f) instead */
    @Deprecated public static final int PRESSED = 0xFF303050;
    /** @deprecated Use withAlpha(current().accent900, 0xAA) instead */
    @Deprecated public static final int SELECTED = 0xFF2A1A45;
    /** @deprecated Use current().glassBorder instead */
    @Deprecated public static final int BORDER_SUBTLE = 0xFF2A2A40;
    /** @deprecated Use current().glassBorder instead */
    @Deprecated public static final int BORDER_ACCENT = 0xFF3D2A5C;
    /** @deprecated Use withAlpha(current().accent500, 0x44) instead */
    @Deprecated public static final int BORDER_GOLD_GLOW = 0x44D4AF37;
    /** @deprecated Use current().bgFrom instead */
    @Deprecated public static final int GRADIENT_START = 0xFF1A0A2E;
    /** @deprecated Use lerpColor(current().bgFrom, current().bgTo, 0.5f) instead */
    @Deprecated public static final int GRADIENT_MID = 0xFF16213E;
    /** @deprecated Use current().bgTo instead */
    @Deprecated public static final int GRADIENT_END = 0xFF0F0F23;
    /** @deprecated Use current().bgFrom instead */
    @Deprecated public static final int SIDEBAR_START = 0xFF120820;
    /** @deprecated Use current().bgTo instead */
    @Deprecated public static final int SIDEBAR_END = 0xFF0D0D18;
}
