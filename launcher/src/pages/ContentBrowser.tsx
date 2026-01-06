import { useEffect, useState, useCallback, useMemo } from 'react';
import { Search, ChevronDown, Loader2, Package, Palette, Sun, Database, User, Layers } from 'lucide-react';
import clsx from 'clsx';
import { invoke } from '@tauri-apps/api/core';
import {
  useContentStore,
  ContentType,
  SortBy,
  ContentItem,
} from '@/lib/stores/contentStore';
import { useProfileStore } from '@/lib/stores/profileStore';
import { useGameStore } from '@/lib/stores/gameStore';
import ContentCard from '@/components/ContentCard';
import { useToast } from '@/components/ToastContainer';
import { EmptyState, ContentCardSkeletonGrid, ErrorState } from '@/components/ui';

interface MinecraftVersion {
  id: string;
  type: string;
  releaseTime: string;
}

const CONTENT_TYPES: { id: ContentType; label: string; icon: React.ElementType }[] = [
  { id: 'mod', label: 'Mods', icon: Package },
  { id: 'modpack', label: 'Modpacks', icon: Layers },
  { id: 'resourcepack', label: 'Resource Packs', icon: Palette },
  { id: 'shader', label: 'Shaders', icon: Sun },
  { id: 'datapack', label: 'Data Packs', icon: Database },
];

const SORT_OPTIONS: { id: SortBy; label: string }[] = [
  { id: 'downloads', label: 'Downloads' },
  { id: 'updated', label: 'Recently Updated' },
  { id: 'newest', label: 'Newest' },
  { id: 'relevance', label: 'Relevance' },
];

export default function ContentBrowser() {
  const [versions, setVersions] = useState<MinecraftVersion[]>([]);
  const [loadingVersions, setLoadingVersions] = useState(true);
  const [showVersionDropdown, setShowVersionDropdown] = useState(false);
  const [showSortDropdown, setShowSortDropdown] = useState(false);
  const [showSourceDropdown, setShowSourceDropdown] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const { showToast } = useToast();

  const {
    contentType,
    setContentType,
    searchQuery,
    setSearchQuery,
    sortBy,
    setSortBy,
    source,
    setSource,
    version,
    setVersion,
    results,
    isLoading,
    error,
    hasMore,
    totalResults,
    search,
    loadMore,
    installContent,
    installModpack,
    fetchCategories,
  } = useContentStore();

  const {
    profiles,
    activeProfileId,
    fetchProfiles,
    setActiveProfile,
    fetchActiveProfile,
  } = useProfileStore();

  const { installedMods, fetchMods, selectedVersion: gameSelectedVersion, setSelectedVersion: setGameSelectedVersion } = useGameStore();

  // Get profiles for the current version
  const versionProfiles = useMemo(() =>
    profiles.filter(p => p.version === version),
    [profiles, version]
  );

  // Get the active profile object
  const activeProfile = useMemo(() =>
    profiles.find(p => p.id === activeProfileId),
    [profiles, activeProfileId]
  );

  // Get installed mod names/filenames for checking duplicates
  const installedModNames = useMemo(() => {
    return new Set(installedMods.map(m => m.name.toLowerCase()));
  }, [installedMods]);

  // Fetch versions on mount and sync with gameStore
  useEffect(() => {
    const fetchVersions = async () => {
      try {
        const vers = await invoke<MinecraftVersion[]>('get_minecraft_versions');
        setVersions(vers);
        // Use gameStore's selectedVersion if available, otherwise use first version
        if (vers.length > 0) {
          if (gameSelectedVersion && vers.some(v => v.id === gameSelectedVersion)) {
            setVersion(gameSelectedVersion);
          } else if (!version) {
            setVersion(vers[0].id);
          }
        }
      } catch (err) {
        console.error('Failed to fetch versions:', err);
      } finally {
        setLoadingVersions(false);
      }
    };
    fetchVersions();
    fetchCategories();
  }, [gameSelectedVersion]);

  // Fetch profiles when version changes
  useEffect(() => {
    if (version) {
      fetchProfiles(version);
      fetchActiveProfile(version);
    }
  }, [version, fetchProfiles, fetchActiveProfile]);

  // Fetch mods when active profile changes
  useEffect(() => {
    if (version && activeProfileId) {
      fetchMods(activeProfileId);
    }
  }, [version, activeProfileId, fetchMods]);

  // Search when filters change
  useEffect(() => {
    if (version) {
      search();
    }
  }, [contentType, sortBy, source, version]);

  // Debounced search for query
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery !== searchInput) {
        setSearchQuery(searchInput);
        if (version) {
          search();
        }
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchQuery(searchInput);
    search();
  };

  const handleInstall = useCallback(
    async (item: ContentItem) => {
      // For modpacks, create a new profile
      if (contentType === 'modpack') {
        try {
          await installModpack(item);
          // Refresh profiles to show the new modpack profile
          if (version) {
            await fetchProfiles(version);
          }
          showToast(`Installed modpack "${item.name}" as a new profile!`, 'success');
        } catch (err) {
          showToast(`Failed to install modpack: ${err}`, 'error');
          throw err;
        }
        return;
      }

      // For other content types, require a profile
      if (!activeProfileId) {
        showToast('Please select a profile first', 'error');
        return;
      }
      try {
        await installContent(item, activeProfileId);
        // Refresh profiles to update installed mods list
        if (version) {
          await fetchProfiles(version);
        }
        showToast(`Installed ${item.name} to ${activeProfile?.name || 'profile'}`, 'success');
      } catch (err) {
        showToast(`Failed to install: ${err}`, 'error');
        throw err;
      }
    },
    [contentType, installContent, installModpack, activeProfileId, activeProfile, version, fetchProfiles, showToast]
  );

  const handleProfileChange = async (profileId: string) => {
    try {
      await setActiveProfile(version, profileId);
      await fetchMods(profileId); // Immediately refresh mods for new profile
      setShowProfileDropdown(false);
    } catch (err) {
      showToast(`Failed to switch profile: ${err}`, 'error');
    }
  };

  // Check if a content item is already installed
  const isInstalled = useCallback((item: ContentItem) => {
    // Check by name (case insensitive)
    return installedModNames.has(item.name.toLowerCase());
  }, [installedModNames]);

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Content Browser</h1>
        <p className="text-theme-muted mt-1">
          Browse and install mods, modpacks, resource packs, shaders, and data packs
        </p>
      </div>

      {/* Content Type Tabs */}
      <div className="flex gap-2 p-1 bg-surface-secondary/50 rounded-xl w-fit">
        {CONTENT_TYPES.map((type) => (
          <button
            key={type.id}
            onClick={() => setContentType(type.id)}
            className={clsx(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              contentType === type.id
                ? 'bg-miracle-500 text-white'
                : 'text-theme-secondary hover:text-white hover:bg-surface-secondary/50'
            )}
          >
            <type.icon className="w-4 h-4" />
            {type.label}
          </button>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        {/* Search */}
        <form onSubmit={handleSearch} className="flex-1 min-w-[300px]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-theme-muted" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder={`Search ${CONTENT_TYPES.find((t) => t.id === contentType)?.label.toLowerCase()}...`}
              className="w-full pl-10 pr-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg focus:border-miracle-500 focus:outline-none transition-colors"
            />
          </div>
        </form>

        {/* Version Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowVersionDropdown(!showVersionDropdown)}
            className="flex items-center gap-2 px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg hover:border-miracle-500/50 transition-colors min-w-[140px]"
          >
            <span className="text-theme-muted text-sm">Version:</span>
            <span className="font-medium">{version || 'Select'}</span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showVersionDropdown && (
            <div className="absolute top-full mt-2 left-0 w-[200px] max-h-[300px] overflow-y-auto glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              {loadingVersions ? (
                <div className="p-4 text-center text-theme-muted">
                  <Loader2 className="w-5 h-5 animate-spin mx-auto" />
                </div>
              ) : (
                versions.map((ver) => (
                  <button
                    key={ver.id}
                    onClick={() => {
                      setVersion(ver.id);
                      setGameSelectedVersion(ver.id); // Sync with gameStore
                      setShowVersionDropdown(false);
                    }}
                    className={clsx(
                      'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors',
                      version === ver.id && 'text-miracle-400'
                    )}
                  >
                    {ver.id}
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Profile Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowProfileDropdown(!showProfileDropdown)}
            className={clsx(
              'flex items-center gap-2 px-4 py-2.5 border rounded-lg transition-colors min-w-[180px]',
              activeProfile
                ? 'bg-miracle-500/10 border-miracle-500/50 hover:border-miracle-500'
                : 'bg-surface-secondary border-theme-muted/20 hover:border-miracle-500/50'
            )}
          >
            <User className="w-4 h-4 text-theme-muted" />
            <span className="text-theme-muted text-sm">Profile:</span>
            <span className={clsx('font-medium', activeProfile && 'text-miracle-400')}>
              {activeProfile?.name || 'Select'}
            </span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showProfileDropdown && (
            <div className="absolute top-full mt-2 left-0 w-[220px] glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              {versionProfiles.length === 0 ? (
                <div className="p-4 text-center text-theme-muted text-sm">
                  No profiles for {version}
                </div>
              ) : (
                versionProfiles.map((profile) => (
                  <button
                    key={profile.id}
                    onClick={() => handleProfileChange(profile.id)}
                    className={clsx(
                      'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors flex items-center justify-between',
                      profile.id === activeProfileId && 'text-miracle-400'
                    )}
                  >
                    <span>{profile.name}</span>
                    <span className="text-xs text-theme-muted">
                      {profile.id === activeProfileId ? installedMods.length : profile.mods.length} mods
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Sort Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowSortDropdown(!showSortDropdown)}
            className="flex items-center gap-2 px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg hover:border-miracle-500/50 transition-colors min-w-[180px]"
          >
            <span className="text-theme-muted text-sm">Sort:</span>
            <span className="font-medium">
              {SORT_OPTIONS.find((s) => s.id === sortBy)?.label}
            </span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showSortDropdown && (
            <div className="absolute top-full mt-2 left-0 w-full glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              {SORT_OPTIONS.map((option) => (
                <button
                  key={option.id}
                  onClick={() => {
                    setSortBy(option.id);
                    setShowSortDropdown(false);
                  }}
                  className={clsx(
                    'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors',
                    sortBy === option.id && 'text-miracle-400'
                  )}
                >
                  {option.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Source Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowSourceDropdown(!showSourceDropdown)}
            className="flex items-center gap-2 px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg hover:border-miracle-500/50 transition-colors min-w-[160px]"
          >
            <span className="text-theme-muted text-sm">Source:</span>
            <span
              className={clsx(
                'font-medium',
                source === 'modrinth' ? 'text-green-400' : 'text-orange-400'
              )}
            >
              {source === 'modrinth' ? 'Modrinth' : 'CurseForge'}
            </span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showSourceDropdown && (
            <div className="absolute top-full mt-2 left-0 w-full glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              <button
                onClick={() => {
                  setSource('modrinth');
                  setShowSourceDropdown(false);
                }}
                className={clsx(
                  'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors',
                  source === 'modrinth' && 'text-green-400'
                )}
              >
                Modrinth
              </button>
              <button
                onClick={() => {
                  setSource('curseforge');
                  setShowSourceDropdown(false);
                }}
                className={clsx(
                  'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors',
                  source === 'curseforge' && 'text-orange-400'
                )}
              >
                CurseForge
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Results Count */}
      {!isLoading && results.length > 0 && (
        <p className="text-theme-muted text-sm">
          Showing {results.length} of {totalResults.toLocaleString()} results
        </p>
      )}

      {/* Error */}
      {error && (
        <ErrorState
          message={error}
          onRetry={() => search()}
        />
      )}

      {/* Results Grid */}
      {isLoading && results.length === 0 ? (
        <ContentCardSkeletonGrid count={6} />
      ) : results.length === 0 && !error ? (
        <EmptyState
          icon={(CONTENT_TYPES.find((t) => t.id === contentType)?.icon || Package) as any}
          title="No results found"
          description="Try adjusting your search or filters to find what you're looking for."
        />
      ) : !error ? (
        <>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {results.map((item) => (
              <ContentCard
                key={item.id}
                item={item}
                onInstall={handleInstall}
                isInstalled={isInstalled(item)}
              />
            ))}
          </div>

          {/* Load More */}
          {hasMore && (
            <div className="flex justify-center pt-4">
              <button
                onClick={loadMore}
                disabled={isLoading}
                className="px-6 py-2.5 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 transition-colors flex items-center gap-2"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Loading...
                  </>
                ) : (
                  'Load More'
                )}
              </button>
            </div>
          )}
        </>
      ) : null}

      {/* Click outside to close dropdowns */}
      {(showVersionDropdown || showSortDropdown || showSourceDropdown || showProfileDropdown) && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => {
            setShowVersionDropdown(false);
            setShowSortDropdown(false);
            setShowSourceDropdown(false);
            setShowProfileDropdown(false);
          }}
        />
      )}
    </div>
  );
}
