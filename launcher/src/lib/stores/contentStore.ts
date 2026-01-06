import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';

export type ContentType = 'mod' | 'resourcepack' | 'shader' | 'datapack' | 'modpack';
export type SortBy = 'downloads' | 'updated' | 'newest' | 'relevance';
export type ContentSource = 'modrinth' | 'curseforge';

export interface ModrinthSearchResult {
  slug: string;
  title: string;
  description: string;
  categories: string[];
  client_side: string;
  server_side: string;
  project_type: string;
  downloads: number;
  icon_url: string | null;
  color: number | null;
  author: string;
  display_categories: string[];
  versions: string[];
  follows: number;
  date_created: string;
  date_modified: string;
  latest_version: string | null;
  license: string;
  gallery: string[];
  project_id: string;
}

export interface CurseForgeSearchResult {
  id: number;
  name: string;
  slug?: string;
  summary?: string;
  downloads?: number;
  date_created?: string | null;
  date_modified?: string | null;
  date_released?: string | null;
  categories?: { id: number; name: string; slug: string; class_id?: number }[];
  authors?: { id: number; name: string }[];
  logo?: { url: string; thumbnailUrl: string } | null;
  class_id?: number;
}

export interface ContentItem {
  id: string;
  slug: string;
  name: string;
  description: string;
  author: string;
  downloads: number;
  iconUrl: string | null;
  categories: string[];
  source: ContentSource;
  curseforgeId?: number;
}

export interface ModrinthCategory {
  icon: string;
  name: string;
  project_type: string;
  header: string;
}

interface ContentState {
  // Current state
  contentType: ContentType;
  searchQuery: string;
  category: string;
  sortBy: SortBy;
  source: ContentSource;
  version: string;

  // Results
  results: ContentItem[];
  isLoading: boolean;
  error: string | null;
  currentPage: number;
  totalResults: number;
  hasMore: boolean;

  // Categories cache
  modrinthCategories: ModrinthCategory[];

  // Actions
  setContentType: (type: ContentType) => void;
  setSearchQuery: (query: string) => void;
  setCategory: (category: string) => void;
  setSortBy: (sort: SortBy) => void;
  setSource: (source: ContentSource) => void;
  setVersion: (version: string) => void;

  search: () => Promise<void>;
  loadMore: () => Promise<void>;
  fetchCategories: () => Promise<void>;

  installContent: (item: ContentItem, profileId?: string) => Promise<string>;
  installModpack: (item: ContentItem) => Promise<string>;
}

export interface ModpackInfo {
  name: string;
  description: string;
  icon_url: string | null;
  mod_count: number;
  minecraft_version: string;
  source: string;
}

const RESULTS_PER_PAGE = 20;

export const useContentStore = create<ContentState>((set, get) => ({
  // Initial state
  contentType: 'mod',
  searchQuery: '',
  category: '',
  sortBy: 'downloads',
  source: 'modrinth',
  version: '',

  results: [],
  isLoading: false,
  error: null,
  currentPage: 0,
  totalResults: 0,
  hasMore: false,

  modrinthCategories: [],

  // Setters
  setContentType: (type) => set({ contentType: type, results: [], currentPage: 0, category: '' }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  setCategory: (category) => set({ category }),
  setSortBy: (sort) => set({ sortBy: sort }),
  setSource: (source) => set({ source, results: [], currentPage: 0 }),
  setVersion: (version) => set({ version }),

  // Search
  search: async () => {
    const state = get();
    set({ isLoading: true, error: null, results: [], currentPage: 0 });

    try {
      if (state.source === 'modrinth') {
        const response = await invoke<{
          hits: ModrinthSearchResult[];
          offset: number;
          limit: number;
          total_hits: number;
        }>('search_modrinth', {
          query: state.searchQuery,
          contentType: state.contentType,
          categories: state.category ? [state.category] : [],
          sort: state.sortBy,
          version: state.version,
          offset: 0,
          limit: RESULTS_PER_PAGE,
        });

        const items: ContentItem[] = response.hits.map((hit) => ({
          id: hit.project_id,
          slug: hit.slug,
          name: hit.title,
          description: hit.description,
          author: hit.author,
          downloads: hit.downloads,
          iconUrl: hit.icon_url,
          categories: hit.display_categories,
          source: 'modrinth' as ContentSource,
        }));

        set({
          results: items,
          totalResults: response.total_hits,
          hasMore: response.total_hits > RESULTS_PER_PAGE,
          isLoading: false,
        });
      } else {
        const response = await invoke<{
          data: CurseForgeSearchResult[];
          pagination: { index: number; pageSize: number; resultCount: number; totalCount: number };
        }>('search_curseforge', {
          query: state.searchQuery,
          contentType: state.contentType,
          category: state.category ? parseInt(state.category) : null,
          sort: state.sortBy === 'relevance' ? 'popularity' : state.sortBy,
          version: state.version,
          offset: 0,
          limit: RESULTS_PER_PAGE,
        });

        const items: ContentItem[] = response.data.map((item) => ({
          id: item.id.toString(),
          slug: item.slug || '',
          name: item.name,
          description: item.summary || '',
          author: item.authors?.[0]?.name || 'Unknown',
          downloads: item.downloads || 0,
          iconUrl: item.logo?.thumbnailUrl || null,
          categories: item.categories?.map((c) => c.name) || [],
          source: 'curseforge' as ContentSource,
          curseforgeId: item.id,
        }));

        set({
          results: items,
          totalResults: response.pagination.totalCount,
          hasMore: response.pagination.totalCount > RESULTS_PER_PAGE,
          isLoading: false,
        });
      }
    } catch (error) {
      set({ error: String(error), isLoading: false });
    }
  },

  // Load more results
  loadMore: async () => {
    const state = get();
    if (state.isLoading || !state.hasMore) return;

    const nextPage = state.currentPage + 1;
    const offset = nextPage * RESULTS_PER_PAGE;

    set({ isLoading: true });

    try {
      if (state.source === 'modrinth') {
        const response = await invoke<{
          hits: ModrinthSearchResult[];
          offset: number;
          limit: number;
          total_hits: number;
        }>('search_modrinth', {
          query: state.searchQuery,
          contentType: state.contentType,
          categories: state.category ? [state.category] : [],
          sort: state.sortBy,
          version: state.version,
          offset,
          limit: RESULTS_PER_PAGE,
        });

        const items: ContentItem[] = response.hits.map((hit) => ({
          id: hit.project_id,
          slug: hit.slug,
          name: hit.title,
          description: hit.description,
          author: hit.author,
          downloads: hit.downloads,
          iconUrl: hit.icon_url,
          categories: hit.display_categories,
          source: 'modrinth' as ContentSource,
        }));

        set({
          results: [...state.results, ...items],
          currentPage: nextPage,
          hasMore: offset + items.length < response.total_hits,
          isLoading: false,
        });
      } else {
        const response = await invoke<{
          data: CurseForgeSearchResult[];
          pagination: { index: number; pageSize: number; resultCount: number; totalCount: number };
        }>('search_curseforge', {
          query: state.searchQuery,
          contentType: state.contentType,
          category: state.category ? parseInt(state.category) : null,
          sort: state.sortBy === 'relevance' ? 'popularity' : state.sortBy,
          version: state.version,
          offset,
          limit: RESULTS_PER_PAGE,
        });

        const items: ContentItem[] = response.data.map((item) => ({
          id: item.id.toString(),
          slug: item.slug || '',
          name: item.name,
          description: item.summary || '',
          author: item.authors?.[0]?.name || 'Unknown',
          downloads: item.downloads || 0,
          iconUrl: item.logo?.thumbnailUrl || null,
          categories: item.categories?.map((c) => c.name) || [],
          source: 'curseforge' as ContentSource,
          curseforgeId: item.id,
        }));

        set({
          results: [...state.results, ...items],
          currentPage: nextPage,
          hasMore: offset + items.length < response.pagination.totalCount,
          isLoading: false,
        });
      }
    } catch (error) {
      set({ error: String(error), isLoading: false });
    }
  },

  // Fetch categories
  fetchCategories: async () => {
    try {
      const categories = await invoke<ModrinthCategory[]>('get_modrinth_categories');
      set({ modrinthCategories: categories });
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    }
  },

  // Install content
  installContent: async (item: ContentItem, profileId?: string) => {
    const state = get();

    if (item.source === 'modrinth') {
      return await invoke<string>('download_modrinth_content', {
        projectSlug: item.slug,
        contentType: state.contentType,
        gameVersion: state.version,
        profileId,
      });
    } else {
      return await invoke<string>('download_curseforge_content', {
        projectId: item.curseforgeId,
        contentType: state.contentType,
        gameVersion: state.version,
        profileId,
      });
    }
  },

  // Install modpack (creates a new profile)
  installModpack: async (item: ContentItem) => {
    const state = get();

    return await invoke<string>('install_modpack', {
      projectSlug: item.slug,
      source: item.source,
      gameVersion: state.version,
    });
  },
}));
