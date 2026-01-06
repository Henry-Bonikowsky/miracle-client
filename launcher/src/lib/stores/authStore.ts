import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { invoke } from '@tauri-apps/api/core';

export interface MinecraftProfile {
  id: string;
  name: string;
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  // Backward compatibility with old snake_case format
  access_token?: string;
  refresh_token?: string;
  expires_at?: number;
}

export interface DeviceCodeInfo {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
  message?: string;
}

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  profile: MinecraftProfile | null;  // Active account
  accounts: MinecraftProfile[];       // All saved accounts
  error: string | null;
  deviceCode: DeviceCodeInfo | null;

  startLogin: () => Promise<void>;
  completeLogin: () => Promise<void>;
  logout: () => Promise<void>;
  removeAccount: (id: string) => void;
  switchAccount: (id: string) => Promise<void>;
  refreshToken: () => Promise<void>;
  checkAuth: () => Promise<void>;
  getAccounts: () => MinecraftProfile[];
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      isAuthenticated: false,
      isLoading: false,
      profile: null,
      accounts: [],
      error: null,
      deviceCode: null,

      startLogin: async () => {
        set({ isLoading: true, error: null });
        try {
          const deviceCode = await invoke<DeviceCodeInfo>('auth_start_device_flow');
          set({ deviceCode, isLoading: false });
        } catch (error) {
          set({
            error: error as string,
            isLoading: false,
          });
          throw error;
        }
      },

      completeLogin: async () => {
        const { deviceCode, accounts } = get();
        if (!deviceCode) {
          throw new Error('No device code available');
        }

        set({ isLoading: true, error: null });
        try {
          const profile = await invoke<MinecraftProfile>('auth_poll_device_flow', {
            deviceCode: deviceCode.device_code,
            interval: deviceCode.interval,
          });

          // Add to accounts if not already present, or update existing
          const existingIndex = accounts.findIndex(a => a.id === profile.id);
          let newAccounts: MinecraftProfile[];
          if (existingIndex >= 0) {
            newAccounts = [...accounts];
            newAccounts[existingIndex] = profile;
          } else {
            newAccounts = [...accounts, profile];
          }

          set({
            isAuthenticated: true,
            profile,
            accounts: newAccounts,
            isLoading: false,
            deviceCode: null,
          });
        } catch (error) {
          set({
            error: error as string,
            isLoading: false,
            deviceCode: null,
          });
          throw error;
        }
      },

      logout: async () => {
        set({ isLoading: true });
        try {
          await invoke('auth_logout');
        } finally {
          set({
            isAuthenticated: false,
            profile: null,
            isLoading: false,
            deviceCode: null,
          });
        }
      },

      removeAccount: (id: string) => {
        const { accounts, profile } = get();
        const newAccounts = accounts.filter(a => a.id !== id);

        // If removing the active account, switch to another or logout
        if (profile?.id === id) {
          if (newAccounts.length > 0) {
            set({ accounts: newAccounts, profile: newAccounts[0], isAuthenticated: true });
          } else {
            set({ accounts: [], profile: null, isAuthenticated: false });
          }
        } else {
          set({ accounts: newAccounts });
        }
      },

      switchAccount: async (id: string) => {
        const { accounts } = get();
        const account = accounts.find(a => a.id === id);
        if (!account) return;

        // Refresh the token if needed before switching
        const expiresAt = account.expiresAt || account.expires_at || 0;
        if (Date.now() >= expiresAt) {
          const refreshToken = account.refreshToken || account.refresh_token;
          if (refreshToken) {
            try {
              const newProfile = await invoke<MinecraftProfile>('auth_refresh', {
                refreshToken,
              });
              // Update the account in the list
              const newAccounts = accounts.map(a => a.id === id ? newProfile : a);
              set({ profile: newProfile, accounts: newAccounts, isAuthenticated: true });
              return;
            } catch {
              // Token refresh failed, remove this account
              get().removeAccount(id);
              return;
            }
          }
        }

        set({ profile: account, isAuthenticated: true });
      },

      refreshToken: async () => {
        const { profile, accounts } = get();
        if (!profile) return;

        const refreshToken = profile.refreshToken || profile.refresh_token;
        if (!refreshToken) {
          await get().logout();
          return;
        }

        try {
          const newProfile = await invoke<MinecraftProfile>('auth_refresh', {
            refreshToken: refreshToken,
          });
          // Update in accounts list too
          const newAccounts = accounts.map(a => a.id === profile.id ? newProfile : a);
          set({ profile: newProfile, accounts: newAccounts });
        } catch {
          await get().logout();
        }
      },

      checkAuth: async () => {
        set({ isLoading: true });
        const { profile } = get();

        if (!profile) {
          set({ isLoading: false, isAuthenticated: false });
          return;
        }

        const expiresAt = profile.expiresAt || profile.expires_at || 0;
        if (Date.now() >= expiresAt) {
          await get().refreshToken();
        } else {
          set({ isAuthenticated: true, isLoading: false });
        }
      },

      getAccounts: () => get().accounts,
    }),
    {
      name: 'miracle-auth',
      partialize: (state) => ({
        profile: state.profile,
        accounts: state.accounts,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
