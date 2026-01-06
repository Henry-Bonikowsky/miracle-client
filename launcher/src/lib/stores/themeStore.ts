import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { invoke } from '@tauri-apps/api/core';

export interface Theme {
  id: string;
  name: string;
  description: string;
  colors: {
    // Primary accent color scale
    accent50: string;
    accent100: string;
    accent200: string;
    accent300: string;
    accent400: string;
    accent500: string;
    accent600: string;
    accent700: string;
    accent800: string;
    accent900: string;
    accent950: string;
    // Background gradient
    bgFrom: string;
    bgTo: string;
    // Glass effect
    glassBg: string;
    glassBorder: string;
    // Text colors (for light mode support)
    textPrimary: string;
    textSecondary: string;
    textMuted: string;
    // Surface colors for cards/panels in light mode
    surfacePrimary: string;
    surfaceSecondary: string;
  };
}

export const themes: Theme[] = [
  {
    id: 'midnight',
    name: 'Midnight',
    description: 'Deep blue ocean vibes',
    colors: {
      accent50: '#f0f9ff',
      accent100: '#e0f2fe',
      accent200: '#bae6fd',
      accent300: '#7dd3fc',
      accent400: '#38bdf8',
      accent500: '#0ea5e9',
      accent600: '#0284c7',
      accent700: '#0369a1',
      accent800: '#075985',
      accent900: '#0c4a6e',
      accent950: '#082f49',
      bgFrom: '#060a10',
      bgTo: '#0c4a6e',
      glassBg: 'rgba(12, 30, 50, 0.5)',
      glassBorder: 'rgba(56, 189, 248, 0.12)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'amethyst',
    name: 'Amethyst',
    description: 'Royal purple elegance',
    colors: {
      accent50: '#faf5ff',
      accent100: '#f3e8ff',
      accent200: '#e9d5ff',
      accent300: '#d8b4fe',
      accent400: '#c084fc',
      accent500: '#a855f7',
      accent600: '#9333ea',
      accent700: '#7e22ce',
      accent800: '#6b21a8',
      accent900: '#581c87',
      accent950: '#3b0764',
      bgFrom: '#0c0610',
      bgTo: '#581c87',
      glassBg: 'rgba(30, 15, 40, 0.5)',
      glassBorder: 'rgba(168, 85, 247, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'forest',
    name: 'Forest',
    description: 'Natural emerald greens',
    colors: {
      accent50: '#ecfdf5',
      accent100: '#d1fae5',
      accent200: '#a7f3d0',
      accent300: '#6ee7b7',
      accent400: '#34d399',
      accent500: '#10b981',
      accent600: '#059669',
      accent700: '#047857',
      accent800: '#065f46',
      accent900: '#064e3b',
      accent950: '#022c22',
      bgFrom: '#060f0c',
      bgTo: '#064e3b',
      glassBg: 'rgba(10, 35, 28, 0.5)',
      glassBorder: 'rgba(16, 185, 129, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'sunset',
    name: 'Sunset',
    description: 'Warm amber glow',
    colors: {
      accent50: '#fffbeb',
      accent100: '#fef3c7',
      accent200: '#fde68a',
      accent300: '#fcd34d',
      accent400: '#fbbf24',
      accent500: '#f59e0b',
      accent600: '#d97706',
      accent700: '#b45309',
      accent800: '#92400e',
      accent900: '#78350f',
      accent950: '#451a03',
      bgFrom: '#100a06',
      bgTo: '#78350f',
      glassBg: 'rgba(40, 25, 12, 0.5)',
      glassBorder: 'rgba(245, 158, 11, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'rose',
    name: 'Rose',
    description: 'Soft pink warmth',
    colors: {
      accent50: '#fff1f2',
      accent100: '#ffe4e6',
      accent200: '#fecdd3',
      accent300: '#fda4af',
      accent400: '#fb7185',
      accent500: '#f43f5e',
      accent600: '#e11d48',
      accent700: '#be123c',
      accent800: '#9f1239',
      accent900: '#881337',
      accent950: '#4c0519',
      bgFrom: '#10060a',
      bgTo: '#881337',
      glassBg: 'rgba(40, 15, 25, 0.5)',
      glassBorder: 'rgba(244, 63, 94, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'crimson',
    name: 'Crimson',
    description: 'Bold red intensity',
    colors: {
      accent50: '#fef2f2',
      accent100: '#fee2e2',
      accent200: '#fecaca',
      accent300: '#fca5a5',
      accent400: '#f87171',
      accent500: '#ef4444',
      accent600: '#dc2626',
      accent700: '#b91c1c',
      accent800: '#991b1b',
      accent900: '#7f1d1d',
      accent950: '#450a0a',
      bgFrom: '#100606',
      bgTo: '#7f1d1d',
      glassBg: 'rgba(40, 12, 12, 0.5)',
      glassBorder: 'rgba(239, 68, 68, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'slate',
    name: 'Slate',
    description: 'Clean monochrome',
    colors: {
      accent50: '#f8fafc',
      accent100: '#f1f5f9',
      accent200: '#e2e8f0',
      accent300: '#cbd5e1',
      accent400: '#94a3b8',
      accent500: '#64748b',
      accent600: '#475569',
      accent700: '#334155',
      accent800: '#1e293b',
      accent900: '#0f172a',
      accent950: '#020617',
      bgFrom: '#08090c',
      bgTo: '#1e293b',
      glassBg: 'rgba(20, 25, 35, 0.5)',
      glassBorder: 'rgba(148, 163, 184, 0.15)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'neon',
    name: 'Neon',
    description: 'Electric cyan glow',
    colors: {
      accent50: '#ecfeff',
      accent100: '#cffafe',
      accent200: '#a5f3fc',
      accent300: '#67e8f9',
      accent400: '#22d3ee',
      accent500: '#06b6d4',
      accent600: '#0891b2',
      accent700: '#0e7490',
      accent800: '#155e75',
      accent900: '#164e63',
      accent950: '#083344',
      bgFrom: '#060c10',
      bgTo: '#164e63',
      glassBg: 'rgba(10, 30, 40, 0.5)',
      glassBorder: 'rgba(6, 182, 212, 0.2)',
      textPrimary: '#ffffff',
      textSecondary: '#a1a1aa',
      textMuted: '#71717a',
      surfacePrimary: '#18181b',
      surfaceSecondary: '#27272a',
    },
  },
  {
    id: 'light',
    name: 'Light',
    description: 'Clean and bright',
    colors: {
      accent50: '#f0f9ff',
      accent100: '#e0f2fe',
      accent200: '#bae6fd',
      accent300: '#7dd3fc',
      accent400: '#38bdf8',
      accent500: '#0284c7',
      accent600: '#0369a1',
      accent700: '#075985',
      accent800: '#0c4a6e',
      accent900: '#082f49',
      accent950: '#041e36',
      bgFrom: '#f8fafc',
      bgTo: '#e0f2fe',
      glassBg: 'rgba(255, 255, 255, 0.8)',
      glassBorder: 'rgba(15, 23, 42, 0.15)',
      textPrimary: '#0f172a',
      textSecondary: '#334155',
      textMuted: '#64748b',
      surfacePrimary: '#ffffff',
      surfaceSecondary: '#f1f5f9',
    },
  },
  {
    id: 'high-contrast',
    name: 'High Contrast',
    description: 'Maximum visibility',
    colors: {
      accent50: '#ffffff',
      accent100: '#ffffff',
      accent200: '#ffffff',
      accent300: '#fef08a',
      accent400: '#facc15',
      accent500: '#eab308',
      accent600: '#ca8a04',
      accent700: '#a16207',
      accent800: '#854d0e',
      accent900: '#713f12',
      accent950: '#422006',
      bgFrom: '#000000',
      bgTo: '#000000',
      glassBg: 'rgba(0, 0, 0, 0.9)',
      glassBorder: 'rgba(255, 255, 255, 0.5)',
      textPrimary: '#ffffff',
      textSecondary: '#ffffff',
      textMuted: '#d4d4d8',
      surfacePrimary: '#000000',
      surfaceSecondary: '#18181b',
    },
  },
];

interface ThemeState {
  currentTheme: string;
  setTheme: (themeId: string) => void;
  getTheme: () => Theme;
  syncFromMod: () => Promise<void>;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      currentTheme: 'midnight',

      setTheme: (themeId: string) => {
        const theme = themes.find((t) => t.id === themeId);
        if (theme) {
          set({ currentTheme: themeId });
          applyTheme(theme);
        }
      },

      getTheme: () => {
        const { currentTheme } = get();
        return themes.find((t) => t.id === currentTheme) || themes[0];
      },

      syncFromMod: async () => {
        try {
          const modTheme = await invoke<string | null>('get_mod_theme');
          if (modTheme && themes.find((t) => t.id === modTheme)) {
            const { currentTheme, setTheme } = get();
            if (modTheme !== currentTheme) {
              console.log('Syncing theme from mod:', modTheme);
              setTheme(modTheme);
            }
          }
        } catch (error) {
          console.error('Failed to sync theme from mod:', error);
        }
      },
    }),
    {
      name: 'miracle-theme',
      onRehydrateStorage: () => (state) => {
        if (state) {
          const theme = themes.find((t) => t.id === state.currentTheme) || themes[0];
          applyTheme(theme);
        }
      },
    }
  )
);

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  const { colors } = theme;

  // Set CSS variables
  root.style.setProperty('--accent-50', colors.accent50);
  root.style.setProperty('--accent-100', colors.accent100);
  root.style.setProperty('--accent-200', colors.accent200);
  root.style.setProperty('--accent-300', colors.accent300);
  root.style.setProperty('--accent-400', colors.accent400);
  root.style.setProperty('--accent-500', colors.accent500);
  root.style.setProperty('--accent-600', colors.accent600);
  root.style.setProperty('--accent-700', colors.accent700);
  root.style.setProperty('--accent-800', colors.accent800);
  root.style.setProperty('--accent-900', colors.accent900);
  root.style.setProperty('--accent-950', colors.accent950);
  root.style.setProperty('--bg-from', colors.bgFrom);
  root.style.setProperty('--bg-to', colors.bgTo);
  root.style.setProperty('--glass-bg', colors.glassBg);
  root.style.setProperty('--glass-border', colors.glassBorder);
  root.style.setProperty('--text-primary', colors.textPrimary);
  root.style.setProperty('--text-secondary', colors.textSecondary);
  root.style.setProperty('--text-muted', colors.textMuted);
  root.style.setProperty('--surface-primary', colors.surfacePrimary);
  root.style.setProperty('--surface-secondary', colors.surfaceSecondary);
}
