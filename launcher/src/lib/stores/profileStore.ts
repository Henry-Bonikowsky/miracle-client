import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { invoke } from '@tauri-apps/api/core';

export interface Profile {
  id: string;
  name: string;
  version: string;
  is_default: boolean;
  is_preset: boolean;
  preset_type: string | null;
  mods: string[];
  created_at: string;
}

export interface ProfileExport {
  name: string;
  version: string;
  mods: string[];
}

export interface SharedProfile {
  id: string;
  short_code: string;
  name: string;
  version: string;
  mods: string[];
  creator_uuid: string | null;
  creator_username: string | null;
  downloads: number;
  created_at: string | null;
}

export interface ShareProfileResult {
  success: boolean;
  short_code: string | null;
  message: string;
}

interface ProfileState {
  // State
  profiles: Profile[];
  activeProfileId: string | null;
  isLoading: boolean;
  performanceMods: string[];
  // Track which profiles have had performance mods installed this session
  _performanceModsInstalledFor: Set<string>;

  // Actions
  fetchProfiles: (version: string) => Promise<void>;
  fetchActiveProfile: (version: string) => Promise<Profile | null>;
  setActiveProfile: (version: string, profileId: string) => Promise<void>;
  createProfile: (name: string, version: string, baseProfileId?: string) => Promise<Profile>;
  createPresetProfile: (version: string, presetType: 'skyblock' | 'pvp') => Promise<Profile>;
  deleteProfile: (profileId: string) => Promise<void>;
  duplicateProfile: (profileId: string, newName: string) => Promise<Profile>;
  exportProfile: (profileId: string) => Promise<ProfileExport>;
  importProfile: (name: string, version: string, mods: string[]) => Promise<Profile>;
  getProfileModsDir: (version: string, profileId: string) => Promise<string>;
  fetchPerformanceMods: () => Promise<void>;

  // Sharing
  shareProfileOnline: (profileId: string, creatorUuid?: string, creatorUsername?: string) => Promise<ShareProfileResult>;
  getSharedProfile: (shortCode: string) => Promise<SharedProfile | null>;
  importSharedProfile: (shortCode: string, targetVersion: string) => Promise<Profile>;
}

export const useProfileStore = create<ProfileState>()(
  persist(
    (set, get) => ({
      // Initial state
      profiles: [],
      activeProfileId: null,
      isLoading: false,
      performanceMods: [],
      _performanceModsInstalledFor: new Set<string>(),

      // Fetch all profiles for a version
      fetchProfiles: async (version: string) => {
        set({ isLoading: true });
        try {
          const profiles = await invoke<Profile[]>('get_profiles', {
            minecraftVersion: version,
          });
          set({ profiles, isLoading: false });
        } catch (error) {
          console.error('Failed to fetch profiles:', error);
          set({ isLoading: false });
        }
      },

      // Fetch the active profile for a version
      fetchActiveProfile: async (version: string) => {
        try {
          const profile = await invoke<Profile>('get_active_profile', {
            minecraftVersion: version,
          });
          set({ activeProfileId: profile.id });

          // Also update profiles list if this profile isn't in it
          const { profiles } = get();
          if (!profiles.find(p => p.id === profile.id)) {
            set({ profiles: [...profiles, profile] });
          }

          // Ensure performance mods are installed for this profile (only once per session)
          const profileKey = `${version}:${profile.id}`;
          const { _performanceModsInstalledFor } = get();
          if (!_performanceModsInstalledFor.has(profileKey)) {
            try {
              const installed = await invoke<string[]>('ensure_performance_mods', {
                minecraftVersion: version,
                profileId: profile.id,
              });
              // Mark as installed for this session
              const newSet = new Set(_performanceModsInstalledFor);
              newSet.add(profileKey);
              set({ _performanceModsInstalledFor: newSet });
              if (installed.length > 0) {
                console.log('Installed performance mods:', installed);
              }
            } catch (err) {
              console.warn('Failed to ensure performance mods:', err);
            }
          }

          return profile;
        } catch (error) {
          console.error('Failed to fetch active profile:', error);
          return null;
        }
      },

      // Set the active profile
      setActiveProfile: async (version: string, profileId: string) => {
        try {
          await invoke('set_active_profile', {
            minecraftVersion: version,
            profileId,
          });
          set({ activeProfileId: profileId });

          // Ensure performance mods are installed for this profile (only once per session)
          const profileKey = `${version}:${profileId}`;
          const { _performanceModsInstalledFor } = get();
          if (!_performanceModsInstalledFor.has(profileKey)) {
            try {
              const installed = await invoke<string[]>('ensure_performance_mods', {
                minecraftVersion: version,
                profileId,
              });
              // Mark as installed for this session
              const newSet = new Set(_performanceModsInstalledFor);
              newSet.add(profileKey);
              set({ _performanceModsInstalledFor: newSet });
              if (installed.length > 0) {
                console.log('Installed performance mods:', installed);
              }
            } catch (err) {
              console.warn('Failed to ensure performance mods:', err);
              // Don't fail the profile switch if performance mods fail
            }
          }
        } catch (error) {
          console.error('Failed to set active profile:', error);
          throw error;
        }
      },

      // Create a new custom profile
      createProfile: async (name: string, version: string, baseProfileId?: string) => {
        try {
          const profile = await invoke<Profile>('create_profile', {
            name,
            minecraftVersion: version,
            baseProfileId: baseProfileId || null,
          });

          // Ensure performance mods are installed for the new profile
          await invoke('ensure_performance_mods', {
            minecraftVersion: version,
            profileId: profile.id,
          });

          // Mark as installed for this session
          const profileKey = `${version}:${profile.id}`;
          const { profiles, _performanceModsInstalledFor } = get();
          const newSet = new Set(_performanceModsInstalledFor);
          newSet.add(profileKey);
          set({ profiles: [...profiles, profile], _performanceModsInstalledFor: newSet });

          return profile;
        } catch (error) {
          console.error('Failed to create profile:', error);
          throw error;
        }
      },

      // Create a preset profile
      createPresetProfile: async (version: string, presetType: 'skyblock' | 'pvp') => {
        try {
          const profile = await invoke<Profile>('create_preset_profile', {
            minecraftVersion: version,
            presetType,
          });

          // Ensure performance mods are installed for the new profile
          await invoke('ensure_performance_mods', {
            minecraftVersion: version,
            profileId: profile.id,
          });

          // Mark as installed for this session
          const profileKey = `${version}:${profile.id}`;
          const { profiles, _performanceModsInstalledFor } = get();
          const newSet = new Set(_performanceModsInstalledFor);
          newSet.add(profileKey);
          set({ profiles: [...profiles, profile], _performanceModsInstalledFor: newSet });

          return profile;
        } catch (error) {
          console.error('Failed to create preset profile:', error);
          throw error;
        }
      },

      // Delete a profile
      deleteProfile: async (profileId: string) => {
        try {
          await invoke('delete_profile', { profileId });

          const { profiles, activeProfileId } = get();
          const newProfiles = profiles.filter(p => p.id !== profileId);

          // If we deleted the active profile, clear it
          const updates: Partial<ProfileState> = { profiles: newProfiles };
          if (activeProfileId === profileId) {
            updates.activeProfileId = null;
          }

          set(updates);
        } catch (error) {
          console.error('Failed to delete profile:', error);
          throw error;
        }
      },

      // Duplicate a profile
      duplicateProfile: async (profileId: string, newName: string) => {
        try {
          const profile = await invoke<Profile>('duplicate_profile', {
            profileId,
            newName,
          });

          const { profiles } = get();
          set({ profiles: [...profiles, profile] });

          return profile;
        } catch (error) {
          console.error('Failed to duplicate profile:', error);
          throw error;
        }
      },

      // Export a profile
      exportProfile: async (profileId: string) => {
        try {
          const exported = await invoke<ProfileExport>('export_profile', {
            profileId,
          });
          return exported;
        } catch (error) {
          console.error('Failed to export profile:', error);
          throw error;
        }
      },

      // Import a profile
      importProfile: async (name: string, version: string, mods: string[]) => {
        try {
          const profile = await invoke<Profile>('import_profile', {
            name,
            minecraftVersion: version,
            mods,
          });

          const { profiles } = get();
          set({ profiles: [...profiles, profile] });

          return profile;
        } catch (error) {
          console.error('Failed to import profile:', error);
          throw error;
        }
      },

      // Get the mods directory for a profile
      getProfileModsDir: async (version: string, profileId: string) => {
        try {
          const dir = await invoke<string>('get_profile_mods_dir', {
            minecraftVersion: version,
            profileId,
          });
          return dir;
        } catch (error) {
          console.error('Failed to get profile mods dir:', error);
          throw error;
        }
      },

      // Fetch the list of performance mods
      fetchPerformanceMods: async () => {
        try {
          const mods = await invoke<string[]>('get_performance_mods');
          set({ performanceMods: mods });
        } catch (error) {
          console.error('Failed to fetch performance mods:', error);
        }
      },

      // Share a profile online (to Supabase)
      shareProfileOnline: async (profileId: string, creatorUuid?: string, creatorUsername?: string) => {
        try {
          const result = await invoke<ShareProfileResult>('share_profile_online', {
            profileId,
            creatorUuid: creatorUuid || null,
            creatorUsername: creatorUsername || null,
          });
          return result;
        } catch (error) {
          console.error('Failed to share profile:', error);
          throw error;
        }
      },

      // Get a shared profile by short code
      getSharedProfile: async (shortCode: string) => {
        try {
          const profile = await invoke<SharedProfile | null>('get_shared_profile', {
            shortCode: shortCode.toUpperCase(),
          });
          return profile;
        } catch (error) {
          console.error('Failed to get shared profile:', error);
          throw error;
        }
      },

      // Import a shared profile by short code
      importSharedProfile: async (shortCode: string, targetVersion: string) => {
        try {
          const profile = await invoke<Profile>('import_shared_profile', {
            shortCode: shortCode.toUpperCase(),
            targetVersion,
          });

          const { profiles } = get();
          set({ profiles: [...profiles, profile] });

          return profile;
        } catch (error) {
          console.error('Failed to import shared profile:', error);
          throw error;
        }
      },
    }),
    {
      name: 'miracle-profiles',
      partialize: (state) => ({
        activeProfileId: state.activeProfileId,
      }),
    }
  )
);
