import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { invoke } from '@tauri-apps/api/core';
import { useAuthStore } from './authStore';
import { useThemeStore } from './themeStore';

export interface GameVersion {
  id: string;
  type: 'release' | 'snapshot';
  releaseTime: string;
}

export interface ModInfo {
  id: string;
  name: string;
  version: string;
  enabled: boolean;
  filename: string;
  category?: string;
  description?: string;
}

export interface ModCompatibility {
  mod_id: string;
  mod_name: string;
  compatible: boolean;
}

export interface ModUpdateInfo {
  mod_id: string;
  current_version: string;
  latest_version: string;
  download_url: string;
  changelog: string | null;
  mandatory: boolean;
  has_update: boolean;
}

export type LaunchState =
  | 'idle'
  | 'checking'
  | 'downloading_java'
  | 'downloading_minecraft'
  | 'downloading_fabric'
  | 'downloading_mods'
  | 'updating_mod'
  | 'launching'
  | 'running';

interface DownloadProgress {
  current: number;
  total: number;
  speed: number; // bytes per second
  file: string;
}

interface GameState {
  // Versions
  availableVersions: GameVersion[];
  selectedVersion: string;

  // Launch state
  launchState: LaunchState;
  downloadProgress: DownloadProgress | null;

  // Settings
  allocatedRam: number; // in MB
  javaPath: string | null;
  gameDirectory: string | null;
  showGameLogs: boolean;

  // Mods
  installedMods: ModInfo[];
  incompatibleMods: Record<string, string[]>; // version -> array of mod IDs that are incompatible

  // Miracle Client updates
  pendingUpdate: ModUpdateInfo | null;
  lastUpdateCheck: number | null; // timestamp

  // Actions
  fetchVersions: () => Promise<void>;
  setSelectedVersion: (version: string) => void;
  setAllocatedRam: (ram: number) => void;
  launchGame: () => Promise<void>;
  stopGame: () => Promise<void>;
  setDownloadProgress: (progress: DownloadProgress | null) => void;
  setLaunchState: (state: LaunchState) => void;
  fetchMods: (profileId?: string) => Promise<void>;
  toggleMod: (modId: string, profileId?: string) => Promise<void>;
  downloadMod: (modSlug: string, minecraftVersion: string, curseforgeId?: number, profileId?: string) => Promise<string>;
  setShowGameLogs: (value: boolean) => void;
  addIncompatibleMod: (version: string, modId: string) => void;
  removeIncompatibleMod: (version: string, modId: string) => void;
  getIncompatibleMods: (version: string) => string[];
  installFabricApi: (version: string) => Promise<string>;
  checkModCompatibility: (modSlugs: string[], version: string) => Promise<ModCompatibility[]>;
  uninstallMod: (modId: string, version?: string, profileId?: string) => Promise<void>;
  checkMiracleUpdate: () => Promise<ModUpdateInfo | null>;
  downloadMiracleUpdate: () => Promise<string>;
  clearPendingUpdate: () => void;
}

export const useGameStore = create<GameState>()(
  persist(
    (set, get) => ({
      availableVersions: [],
      selectedVersion: '1.21.11',
      launchState: 'idle',
      downloadProgress: null,
      allocatedRam: 4096,
      javaPath: null,
      gameDirectory: null,
      installedMods: [],
      incompatibleMods: {},
      showGameLogs: false,
      pendingUpdate: null,
      lastUpdateCheck: null,

      fetchVersions: async () => {
        try {
          const versions = await invoke<GameVersion[]>('get_minecraft_versions');
          set({ availableVersions: versions });
        } catch (error) {
          console.error('Failed to fetch versions:', error);
        }
      },

      setSelectedVersion: (version: string) => {
        set({ selectedVersion: version });
      },

      setAllocatedRam: (ram: number) => {
        set({ allocatedRam: ram });
      },

      launchGame: async () => {
        const { selectedVersion, allocatedRam, javaPath, showGameLogs } = get();
        const { profile } = useAuthStore.getState();
        const { currentTheme } = useThemeStore.getState();

        console.log('LaunchGame called with:', { selectedVersion, allocatedRam, javaPath, theme: currentTheme });
        console.log('Profile:', profile);

        if (!profile) {
          throw new Error('Not authenticated. Please login first.');
        }

        // Handle both camelCase and snake_case formats (backward compatibility)
        const accessToken = profile.accessToken || profile.access_token;
        // refreshToken kept for future use
        void (profile.refreshToken || profile.refresh_token);
        const expiresAt = profile.expiresAt || profile.expires_at;

        // Validate profile has required fields
        if (!accessToken || !profile.name || !profile.id) {
          throw new Error('Invalid profile data. Please logout and login again.');
        }

        // Check if token is expired - auto refresh if needed
        let currentAccessToken = accessToken;
        if (expiresAt && Date.now() >= expiresAt) {
          console.log('Token expired, refreshing...');
          await useAuthStore.getState().refreshToken();

          // Get updated profile after refresh
          const refreshedProfile = useAuthStore.getState().profile;
          if (!refreshedProfile) {
            throw new Error('Session expired. Please login again.');
          }
          currentAccessToken = refreshedProfile.accessToken || refreshedProfile.access_token || '';
          if (!currentAccessToken) {
            throw new Error('Failed to refresh session. Please login again.');
          }
        }

        // Build params - only include javaPath if it has a value
        const params: any = {
          version: selectedVersion,
          accessToken: currentAccessToken,
          username: profile.name,
          uuid: profile.id,
          ram: allocatedRam,
          showGameLogs: showGameLogs,
          theme: currentTheme, // Sync theme to mod config
        };

        if (javaPath) {
          params.javaPath = javaPath;
        }

        console.log('Launching with params:', params);

        // Write accounts list for mod to read (for in-game account switching)
        const { accounts } = useAuthStore.getState();
        if (accounts && accounts.length > 0) {
          const accountsForMod = accounts.map(acc => ({
            id: acc.id,
            name: acc.name,
            is_active: acc.id === profile.id,
          }));
          try {
            await invoke('write_accounts_for_game', { accounts: accountsForMod });
          } catch (e) {
            console.warn('Failed to write accounts for mod:', e);
          }
        }

        set({ launchState: 'checking' });

        try {
          await invoke('launch_game', params);
          set({ launchState: 'running' });
        } catch (error) {
          console.error('Failed to launch game:', error);
          set({ launchState: 'idle' });
          throw error;
        }
      },

      stopGame: async () => {
        try {
          await invoke('stop_game');
        } finally {
          set({ launchState: 'idle' });
        }
      },

      setDownloadProgress: (progress) => {
        set({ downloadProgress: progress });
      },

      setLaunchState: (state) => {
        set({ launchState: state });
      },

      fetchMods: async (profileId?: string) => {
        try {
          const { selectedVersion } = get();
          const mods = await invoke<ModInfo[]>('get_installed_mods', {
            minecraftVersion: selectedVersion,
            profileId: profileId || null,
          });
          set({ installedMods: mods });
        } catch (error) {
          console.error('Failed to fetch mods:', error);
        }
      },

      toggleMod: async (modId: string, profileId?: string) => {
        try {
          const { selectedVersion } = get();
          await invoke('toggle_mod', {
            modId,
            minecraftVersion: selectedVersion,
            profileId: profileId || null,
          });
          // Refresh mods list
          get().fetchMods(profileId);
        } catch (error) {
          console.error('Failed to toggle mod:', error);
          throw error;
        }
      },

      downloadMod: async (modSlug: string, minecraftVersion: string, curseforgeId?: number, profileId?: string) => {
        try {
          const result = await invoke<string>('download_mod', {
            modSlug,
            minecraftVersion,
            curseforgeId,
            profileId: profileId || null,
          });
          // Refresh mods list after successful download
          await get().fetchMods(profileId);
          return result;
        } catch (error) {
          console.error('Failed to download mod:', error);
          throw error;
        }
      },

      setShowGameLogs: (value: boolean) => {
        set({ showGameLogs: value });
      },

      addIncompatibleMod: (version: string, modId: string) => {
        set((state) => ({
          incompatibleMods: {
            ...state.incompatibleMods,
            [version]: [...(state.incompatibleMods[version] || []), modId],
          },
        }));
      },

      removeIncompatibleMod: (version: string, modId: string) => {
        set((state) => ({
          incompatibleMods: {
            ...state.incompatibleMods,
            [version]: (state.incompatibleMods[version] || []).filter((id) => id !== modId),
          },
        }));
      },

      getIncompatibleMods: (version: string) => {
        return get().incompatibleMods[version] || [];
      },

      installFabricApi: async (version: string) => {
        try {
          const result = await invoke<string>('install_fabric_api', { minecraftVersion: version });
          return result;
        } catch (error) {
          console.error('Failed to install Fabric API:', error);
          throw error;
        }
      },

      checkModCompatibility: async (modSlugs: string[], version: string) => {
        try {
          const result = await invoke<ModCompatibility[]>('check_mod_compatibility', {
            modSlugs,
            minecraftVersion: version,
          });
          return result;
        } catch (error) {
          console.error('Failed to check mod compatibility:', error);
          throw error;
        }
      },

      uninstallMod: async (modId: string, version?: string, profileId?: string) => {
        try {
          await invoke('uninstall_mod', {
            modId,
            minecraftVersion: version || get().selectedVersion,
            profileId: profileId || null,
          });
          // Refresh mod list after uninstall
          await get().fetchMods(profileId);
        } catch (error) {
          console.error('Failed to uninstall mod:', error);
          throw error;
        }
      },

      checkMiracleUpdate: async () => {
        try {
          const { selectedVersion } = get();
          console.log('Checking for Miracle Client updates...');

          const updateInfo = await invoke<ModUpdateInfo>('check_miracle_update', {
            minecraftVersion: selectedVersion,
          });

          set({ lastUpdateCheck: Date.now() });

          if (updateInfo.has_update) {
            console.log('Update available:', updateInfo.latest_version);
            set({ pendingUpdate: updateInfo });
            return updateInfo;
          } else {
            console.log('No updates available');
            set({ pendingUpdate: null });
            return null;
          }
        } catch (error) {
          console.error('Failed to check for Miracle Client updates:', error);
          return null;
        }
      },

      downloadMiracleUpdate: async () => {
        try {
          const { selectedVersion } = get();
          const result = await invoke<string>('download_miracle_update', {
            minecraftVersion: selectedVersion,
          });
          set({ pendingUpdate: null });
          return result;
        } catch (error) {
          console.error('Failed to download Miracle Client update:', error);
          throw error;
        }
      },

      clearPendingUpdate: () => {
        set({ pendingUpdate: null });
      },
    }),
    {
      name: 'miracle-game',
      partialize: (state) => ({
        selectedVersion: state.selectedVersion,
        allocatedRam: state.allocatedRam,
        javaPath: state.javaPath,
        gameDirectory: state.gameDirectory,
        incompatibleMods: state.incompatibleMods,
        showGameLogs: state.showGameLogs,
        lastUpdateCheck: state.lastUpdateCheck,
      }),
    }
  )
);
