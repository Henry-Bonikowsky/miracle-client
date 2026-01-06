import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';

export interface ClipInfo {
  id: string;
  filename: string;
  path: string;
  thumbnailPath?: string;
  durationMs: number;
  sizeBytes: number;
  createdAt: number;
  width: number;
  height: number;
}

interface ClipsState {
  clips: ClipInfo[];
  isLoading: boolean;
  error: string | null;
  selectedClip: ClipInfo | null;
  isModalOpen: boolean;

  // Actions
  fetchClips: () => Promise<void>;
  refreshClips: () => Promise<void>;
  deleteClip: (clipId: string) => Promise<void>;
  selectClip: (clip: ClipInfo | null) => void;
  openModal: (clip: ClipInfo) => void;
  closeModal: () => void;
  openClipsFolder: () => Promise<void>;
  getClipsDirectory: () => Promise<string>;
}

// Convert snake_case from Rust to camelCase
function convertClipInfo(rustClip: {
  id: string;
  filename: string;
  path: string;
  thumbnail_path?: string;
  duration_ms: number;
  size_bytes: number;
  created_at: number;
  width: number;
  height: number;
}): ClipInfo {
  return {
    id: rustClip.id,
    filename: rustClip.filename,
    path: rustClip.path,
    thumbnailPath: rustClip.thumbnail_path,
    durationMs: rustClip.duration_ms,
    sizeBytes: rustClip.size_bytes,
    createdAt: rustClip.created_at,
    width: rustClip.width,
    height: rustClip.height,
  };
}

export const useClipsStore = create<ClipsState>((set, _get) => ({
  clips: [],
  isLoading: false,
  error: null,
  selectedClip: null,
  isModalOpen: false,

  fetchClips: async () => {
    set({ isLoading: true, error: null });
    try {
      const rustClips = await invoke<Array<{
        id: string;
        filename: string;
        path: string;
        thumbnail_path?: string;
        duration_ms: number;
        size_bytes: number;
        created_at: number;
        width: number;
        height: number;
      }>>('list_clips');

      const clips = rustClips.map(convertClipInfo);
      set({ clips, isLoading: false });
    } catch (error) {
      console.error('Failed to fetch clips:', error);
      set({ error: String(error), isLoading: false });
    }
  },

  refreshClips: async () => {
    try {
      const rustClips = await invoke<Array<{
        id: string;
        filename: string;
        path: string;
        thumbnail_path?: string;
        duration_ms: number;
        size_bytes: number;
        created_at: number;
        width: number;
        height: number;
      }>>('refresh_clips');

      const clips = rustClips.map(convertClipInfo);
      set({ clips });
    } catch (error) {
      console.error('Failed to refresh clips:', error);
    }
  },

  deleteClip: async (clipId: string) => {
    try {
      await invoke('delete_clip', { clipId });
      // Remove from local state
      set((state) => ({
        clips: state.clips.filter((c) => c.id !== clipId),
        selectedClip: state.selectedClip?.id === clipId ? null : state.selectedClip,
        isModalOpen: state.selectedClip?.id === clipId ? false : state.isModalOpen,
      }));
    } catch (error) {
      console.error('Failed to delete clip:', error);
      throw error;
    }
  },

  selectClip: (clip: ClipInfo | null) => {
    set({ selectedClip: clip });
  },

  openModal: (clip: ClipInfo) => {
    set({ selectedClip: clip, isModalOpen: true });
  },

  closeModal: () => {
    set({ isModalOpen: false });
  },

  openClipsFolder: async () => {
    try {
      await invoke('open_clips_folder');
    } catch (error) {
      console.error('Failed to open clips folder:', error);
      throw error;
    }
  },

  getClipsDirectory: async () => {
    try {
      return await invoke<string>('get_clips_directory');
    } catch (error) {
      console.error('Failed to get clips directory:', error);
      throw error;
    }
  },
}));

// Utility functions
export function formatDuration(ms: number): string {
  if (ms <= 0) return '0:00';

  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

export function formatDate(timestamp: number): string {
  const date = new Date(timestamp * 1000);
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
