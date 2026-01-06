import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';

export interface User {
  id: string;
  minecraft_uuid: string;
  username: string;
  is_online: boolean | null;
  current_server: string | null;
  last_seen: string | null;
}

export interface Friend {
  friendship_id: string;
  user: User;
  status: 'pending' | 'accepted';
  is_incoming: boolean;
}

export interface FriendRequestResult {
  success: boolean;
  message: string;
}

interface FriendsState {
  friends: Friend[];
  searchResults: User[];
  isLoading: boolean;
  isSearching: boolean;
  error: string | null;

  // Actions
  registerUser: (uuid: string, username: string) => Promise<User | null>;
  fetchFriends: (uuid: string) => Promise<void>;
  searchUsers: (query: string, excludeUuid: string) => Promise<void>;
  sendFriendRequest: (fromUuid: string, fromUsername: string, toUserId: string) => Promise<FriendRequestResult>;
  acceptRequest: (friendshipId: string) => Promise<FriendRequestResult>;
  removeFriend: (friendshipId: string) => Promise<FriendRequestResult>;
  updateStatus: (uuid: string, isOnline: boolean, server?: string) => Promise<void>;
  clearSearch: () => void;
}

export const useFriendsStore = create<FriendsState>((set, get) => ({
  friends: [],
  searchResults: [],
  isLoading: false,
  isSearching: false,
  error: null,

  registerUser: async (uuid, username) => {
    try {
      const user = await invoke<User>('friends_register_user', {
        minecraftUuid: uuid,
        username,
      });
      return user;
    } catch (err) {
      console.error('Failed to register user:', err);
      return null;
    }
  },

  fetchFriends: async (uuid) => {
    set({ isLoading: true, error: null });
    try {
      const friends = await invoke<Friend[]>('friends_get_list', {
        minecraftUuid: uuid,
      });
      set({ friends, isLoading: false });
    } catch (err) {
      console.error('Failed to fetch friends:', err);
      set({ error: String(err), isLoading: false });
    }
  },

  searchUsers: async (query, excludeUuid) => {
    if (query.length < 2) {
      set({ searchResults: [] });
      return;
    }

    set({ isSearching: true });
    try {
      const results = await invoke<User[]>('friends_search_users', {
        query,
        excludeUuid,
      });
      set({ searchResults: results, isSearching: false });
    } catch (err) {
      console.error('Failed to search users:', err);
      set({ searchResults: [], isSearching: false });
    }
  },

  sendFriendRequest: async (fromUuid, fromUsername, toUserId) => {
    try {
      const result = await invoke<FriendRequestResult>('friends_send_request', {
        fromUuid,
        fromUsername,
        toUserId,
      });
      // Refresh friends list after sending request
      if (result.success) {
        get().fetchFriends(fromUuid);
      }
      return result;
    } catch (err) {
      console.error('Failed to send friend request:', err);
      return { success: false, message: String(err) };
    }
  },

  acceptRequest: async (friendshipId) => {
    try {
      const result = await invoke<FriendRequestResult>('friends_accept_request', {
        friendshipId,
      });
      return result;
    } catch (err) {
      console.error('Failed to accept friend request:', err);
      return { success: false, message: String(err) };
    }
  },

  removeFriend: async (friendshipId) => {
    try {
      const result = await invoke<FriendRequestResult>('friends_remove', {
        friendshipId,
      });
      return result;
    } catch (err) {
      console.error('Failed to remove friend:', err);
      return { success: false, message: String(err) };
    }
  },

  updateStatus: async (uuid, isOnline, server) => {
    try {
      await invoke('friends_update_status', {
        minecraftUuid: uuid,
        isOnline,
        currentServer: server || null,
      });
    } catch (err) {
      console.error('Failed to update status:', err);
    }
  },

  clearSearch: () => {
    set({ searchResults: [] });
  },
}));
