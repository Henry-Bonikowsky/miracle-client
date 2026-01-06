import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { invoke } from '@tauri-apps/api/core';
import {
  Search,
  Grid,
  List,
  Plus,
  Check,
  Package,
  Layers,
  Clock,
  ChevronDown,
  MoreVertical,
  Copy,
  FileDown,
  Trash2,
  Loader2,
  Play,
  FolderOpen,
  Download,
} from 'lucide-react';
import clsx from 'clsx';
import { useProfileStore, Profile } from '@/lib/stores/profileStore';
import { useGameStore, ModInfo } from '@/lib/stores/gameStore';
import { useToast } from '@/components/ToastContainer';
import { useModal } from '@/components/ui';
import InstalledModsModal from '@/components/InstalledModsModal';
import ProfileManagementModal from '@/components/ProfileManagementModal';

interface MinecraftVersion {
  id: string;
  type: string;
  releaseTime: string;
}

type ViewMode = 'grid' | 'list';
type SortBy = 'name' | 'created' | 'mods';

export default function ProfilesPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const modal = useModal();
  const {
    profiles,
    activeProfileId,
    fetchProfiles,
    setActiveProfile,
    deleteProfile,
    duplicateProfile,
  } = useProfileStore();
  const { setSelectedVersion, selectedVersion } = useGameStore();

  const [versions, setVersions] = useState<MinecraftVersion[]>([]);
  const [currentVersion, setCurrentVersion] = useState<string>('');
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [sortBy, setSortBy] = useState<SortBy>('name');
  const [searchQuery, setSearchQuery] = useState('');
  const [showVersionDropdown, setShowVersionDropdown] = useState(false);
  const [showSortDropdown, setShowSortDropdown] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [loadingProfiles, setLoadingProfiles] = useState<Set<string>>(new Set());
  const [showModsModal, setShowModsModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null);
  const [profileMods, setProfileMods] = useState<ModInfo[]>([]);

  // Fetch versions on mount
  useEffect(() => {
    const fetchVersions = async () => {
      try {
        const vers = await invoke<MinecraftVersion[]>('get_minecraft_versions');
        setVersions(vers.filter(v => v.type === 'release'));
        if (vers.length > 0) {
          const initialVersion = selectedVersion || vers.find(v => v.type === 'release')?.id || vers[0].id;
          setCurrentVersion(initialVersion);
        }
      } catch (err) {
        console.error('Failed to fetch versions:', err);
      }
    };
    fetchVersions();
  }, [selectedVersion]);

  // Fetch profiles when version changes
  useEffect(() => {
    if (currentVersion) {
      fetchProfiles(currentVersion);
    }
  }, [currentVersion, fetchProfiles]);

  // Filter and sort profiles
  const filteredProfiles = useMemo(() => {
    let result = profiles.filter(p => p.version === currentVersion);

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.preset_type?.toLowerCase().includes(query)
      );
    }

    // Sort
    result.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'created':
          return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
        case 'mods':
          return b.mods.length - a.mods.length;
        default:
          return 0;
      }
    });

    return result;
  }, [profiles, currentVersion, searchQuery, sortBy]);

  const handleSelectProfile = async (profileId: string) => {
    setLoadingProfiles(prev => new Set(prev).add(profileId));
    try {
      await setActiveProfile(currentVersion, profileId);
      setSelectedVersion(currentVersion);
      showToast('Profile activated', 'success');
    } catch (err) {
      showToast(`Failed to activate profile: ${err}`, 'error');
    } finally {
      setLoadingProfiles(prev => {
        const next = new Set(prev);
        next.delete(profileId);
        return next;
      });
    }
  };

  const handleDuplicateProfile = async (profileId: string, name: string) => {
    try {
      await duplicateProfile(profileId, `${name} (Copy)`);
      showToast('Profile duplicated', 'success');
    } catch (err) {
      showToast(`Failed to duplicate: ${err}`, 'error');
    }
  };

  const handleDeleteProfile = async (profile: Profile) => {
    if (profile.is_default) {
      showToast('Cannot delete the default profile', 'error');
      return;
    }

    const confirmed = await modal.confirm({
      title: 'Delete Profile',
      message: `Are you sure you want to delete "${profile.name}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      variant: 'danger',
    });

    if (!confirmed) return;

    try {
      await deleteProfile(profile.id);
      showToast('Profile deleted', 'success');
    } catch (err) {
      showToast(`Failed to delete: ${err}`, 'error');
    }
  };

  const handleExportProfile = async (profileId: string) => {
    try {
      const exported = await invoke<{ name: string; version: string; mods: string[] }>('export_profile', { profileId });
      const blob = new Blob([JSON.stringify(exported, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${exported.name.replace(/[^a-z0-9]/gi, '_')}_profile.json`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('Profile exported', 'success');
    } catch (err) {
      showToast(`Failed to export: ${err}`, 'error');
    }
  };

  const handleOpenModsFolder = async (profile: Profile) => {
    try {
      await invoke('open_mods_folder', {
        minecraftVersion: profile.version,
        profileId: profile.id,
      });
    } catch (err) {
      showToast(`Failed to open folder: ${err}`, 'error');
    }
  };

  const handleViewMods = async (profile: Profile) => {
    setSelectedProfile(profile);
    try {
      // Ensure performance mods are installed for this profile first
      await invoke('ensure_performance_mods', {
        minecraftVersion: profile.version,
        profileId: profile.id,
      });

      const mods = await invoke<ModInfo[]>('get_installed_mods', {
        minecraftVersion: profile.version,
        profileId: profile.id,
      });
      setProfileMods(mods);
      setShowModsModal(true);
    } catch (err) {
      showToast(`Failed to load mods: ${err}`, 'error');
    }
  };

  const handleToggleMod = async (modId: string) => {
    if (!selectedProfile) return;
    try {
      await invoke('toggle_mod', {
        modId,
        minecraftVersion: selectedProfile.version,
        profileId: selectedProfile.id,
      });
      // Refresh mods list
      const mods = await invoke<ModInfo[]>('get_installed_mods', {
        minecraftVersion: selectedProfile.version,
        profileId: selectedProfile.id,
      });
      setProfileMods(mods);
    } catch (err) {
      showToast(`Failed to toggle mod: ${err}`, 'error');
    }
  };

  const handleRemoveMod = async (modId: string) => {
    if (!selectedProfile) return;
    try {
      await invoke('uninstall_mod', {
        modId,
        minecraftVersion: selectedProfile.version,
        profileId: selectedProfile.id,
      });
      // Refresh mods list
      const mods = await invoke<ModInfo[]>('get_installed_mods', {
        minecraftVersion: selectedProfile.version,
        profileId: selectedProfile.id,
      });
      setProfileMods(mods);
      showToast('Mod removed', 'success');
    } catch (err) {
      showToast(`Failed to remove mod: ${err}`, 'error');
    }
  };

  const handleRefreshMods = async () => {
    if (!selectedProfile) return;
    const mods = await invoke<ModInfo[]>('get_installed_mods', {
      minecraftVersion: selectedProfile.version,
      profileId: selectedProfile.id,
    });
    setProfileMods(mods);
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const getProfileTypeLabel = (profile: Profile) => {
    if (profile.is_default) return 'Default';
    if (profile.preset_type?.startsWith('modpack:')) return 'Modpack';
    if (profile.is_preset && profile.preset_type) return profile.preset_type.charAt(0).toUpperCase() + profile.preset_type.slice(1);
    return 'Custom';
  };

  const getProfileTypeColor = (profile: Profile) => {
    if (profile.is_default) return 'text-blue-400 bg-blue-500/20';
    if (profile.preset_type?.startsWith('modpack:')) return 'text-purple-400 bg-purple-500/20';
    if (profile.is_preset) return 'text-green-400 bg-green-500/20';
    return 'text-theme-muted bg-surface-secondary';
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Profiles</h1>
          <p className="text-theme-muted mt-1">
            Manage your mod profiles for different playstyles
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowImportModal(true)}
            className="px-4 py-2 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 transition-colors flex items-center gap-2 border border-theme-muted/20"
          >
            <Download className="w-4 h-4" />
            Import
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 rounded-lg bg-miracle-500 hover:bg-miracle-600 text-white transition-colors flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Create Profile
          </button>
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap gap-4 items-center">
        {/* Search */}
        <div className="flex-1 min-w-[250px]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-theme-muted" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search profiles..."
              className="w-full pl-10 pr-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg focus:border-miracle-500 focus:outline-none transition-colors"
            />
          </div>
        </div>

        {/* Version Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowVersionDropdown(!showVersionDropdown)}
            className="flex items-center gap-2 px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg hover:border-miracle-500/50 transition-colors min-w-[140px]"
          >
            <span className="text-theme-muted text-sm">Version:</span>
            <span className="font-medium">{currentVersion || 'Select'}</span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showVersionDropdown && (
            <div className="absolute top-full mt-2 left-0 w-[200px] max-h-[300px] overflow-y-auto glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              {versions.map((ver) => (
                <button
                  key={ver.id}
                  onClick={() => {
                    setCurrentVersion(ver.id);
                    setShowVersionDropdown(false);
                  }}
                  className={clsx(
                    'w-full px-4 py-2 text-left hover:bg-surface-secondary/50 transition-colors',
                    currentVersion === ver.id && 'text-miracle-400'
                  )}
                >
                  {ver.id}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Sort Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowSortDropdown(!showSortDropdown)}
            className="flex items-center gap-2 px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg hover:border-miracle-500/50 transition-colors min-w-[140px]"
          >
            <span className="text-theme-muted text-sm">Sort:</span>
            <span className="font-medium">
              {sortBy === 'name' ? 'Name' : sortBy === 'created' ? 'Newest' : 'Most Mods'}
            </span>
            <ChevronDown className="w-4 h-4 text-theme-muted ml-auto" />
          </button>
          {showSortDropdown && (
            <div className="absolute top-full mt-2 left-0 w-full glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
              {[
                { id: 'name', label: 'Name' },
                { id: 'created', label: 'Newest First' },
                { id: 'mods', label: 'Most Mods' },
              ].map((option) => (
                <button
                  key={option.id}
                  onClick={() => {
                    setSortBy(option.id as SortBy);
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

        {/* View Mode Toggle */}
        <div className="flex items-center gap-1 p-1 bg-surface-secondary rounded-lg border border-theme-muted/20">
          <button
            onClick={() => setViewMode('grid')}
            className={clsx(
              'p-2 rounded transition-colors',
              viewMode === 'grid' ? 'bg-miracle-500 text-white' : 'text-theme-muted hover:text-white'
            )}
          >
            <Grid className="w-4 h-4" />
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={clsx(
              'p-2 rounded transition-colors',
              viewMode === 'list' ? 'bg-miracle-500 text-white' : 'text-theme-muted hover:text-white'
            )}
          >
            <List className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Results Count */}
      <p className="text-theme-muted text-sm">
        {filteredProfiles.length} profile{filteredProfiles.length !== 1 ? 's' : ''} for {currentVersion}
      </p>

      {/* Profiles Grid/List */}
      {filteredProfiles.length === 0 ? (
        <div className="text-center py-20">
          <Layers className="w-16 h-16 mx-auto text-theme-muted mb-4" />
          <h3 className="text-lg font-medium text-theme-secondary">No profiles found</h3>
          <p className="text-theme-muted mt-1">
            {searchQuery ? 'Try a different search term' : 'Create your first profile to get started'}
          </p>
          {!searchQuery && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="mt-4 px-4 py-2 rounded-lg bg-miracle-500 hover:bg-miracle-600 text-white transition-colors"
            >
              Create Profile
            </button>
          )}
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredProfiles.map((profile) => (
            <ProfileCard
              key={profile.id}
              profile={profile}
              isActive={profile.id === activeProfileId}
              isLoading={loadingProfiles.has(profile.id)}
              onViewMods={() => handleViewMods(profile)}
              onSelect={() => handleSelectProfile(profile.id)}
              onDuplicate={() => handleDuplicateProfile(profile.id, profile.name)}
              onExport={() => handleExportProfile(profile.id)}
              onOpenFolder={() => handleOpenModsFolder(profile)}
              onDelete={() => handleDeleteProfile(profile)}
              formatDate={formatDate}
              getTypeLabel={getProfileTypeLabel}
              getTypeColor={getProfileTypeColor}
            />
          ))}
        </div>
      ) : (
        <div className="space-y-2">
          {filteredProfiles.map((profile) => (
            <ProfileListItem
              key={profile.id}
              profile={profile}
              isActive={profile.id === activeProfileId}
              isLoading={loadingProfiles.has(profile.id)}
              onViewMods={() => handleViewMods(profile)}
              onSelect={() => handleSelectProfile(profile.id)}
              onDuplicate={() => handleDuplicateProfile(profile.id, profile.name)}
              onExport={() => handleExportProfile(profile.id)}
              onOpenFolder={() => handleOpenModsFolder(profile)}
              onDelete={() => handleDeleteProfile(profile)}
              formatDate={formatDate}
              getTypeLabel={getProfileTypeLabel}
              getTypeColor={getProfileTypeColor}
            />
          ))}
        </div>
      )}

      {/* Click outside to close dropdowns */}
      {(showVersionDropdown || showSortDropdown) && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => {
            setShowVersionDropdown(false);
            setShowSortDropdown(false);
          }}
        />
      )}

      {/* Create Profile Modal */}
      {showCreateModal && (
        <CreateProfileModal
          version={currentVersion}
          onClose={() => setShowCreateModal(false)}
          onCreated={() => {
            fetchProfiles(currentVersion);
            showToast('Profile created', 'success');
          }}
        />
      )}

      {/* Installed Mods Modal */}
      {selectedProfile && (
        <InstalledModsModal
          isOpen={showModsModal}
          onClose={() => {
            setShowModsModal(false);
            setSelectedProfile(null);
          }}
          mods={profileMods}
          profileName={selectedProfile.name}
          profileId={selectedProfile.id}
          version={selectedProfile.version}
          onToggleMod={handleToggleMod}
          onRemoveMod={handleRemoveMod}
          onBrowseMore={async () => {
            // Switch to this profile before navigating so mods install to the right place
            await setActiveProfile(selectedProfile.version, selectedProfile.id);
            setSelectedVersion(selectedProfile.version);
            setShowModsModal(false);
            navigate('/browse');
          }}
          onRefreshMods={handleRefreshMods}
        />
      )}

      {/* Import Modal */}
      <ProfileManagementModal
        isOpen={showImportModal}
        selectedVersion={currentVersion}
        onClose={() => {
          setShowImportModal(false);
          // Refresh profiles after closing in case something was imported
          fetchProfiles(currentVersion);
        }}
        showToast={showToast}
        initialTab="import"
      />
    </div>
  );
}

// Profile Card Component
interface ProfileCardProps {
  profile: Profile;
  isActive: boolean;
  isLoading: boolean;
  onViewMods: () => void;
  onSelect: () => void;
  onDuplicate: () => void;
  onExport: () => void;
  onOpenFolder: () => void;
  onDelete: () => void;
  formatDate: (date: string) => string;
  getTypeLabel: (profile: Profile) => string;
  getTypeColor: (profile: Profile) => string;
}

function ProfileCard({
  profile,
  isActive,
  isLoading,
  onViewMods,
  onSelect,
  onDuplicate,
  onExport,
  onOpenFolder,
  onDelete,
  formatDate,
  getTypeLabel,
  getTypeColor,
}: ProfileCardProps) {
  const [showMenu, setShowMenu] = useState(false);

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setShowMenu(true);
  };

  return (
    <div
      className={clsx(
        'glass rounded-xl border p-5 transition-all cursor-pointer group',
        isActive
          ? 'border-miracle-500 shadow-lg shadow-miracle-500/20'
          : 'border-theme-muted/20/50 hover:border-miracle-500/50'
      )}
      onClick={onViewMods}
      onContextMenu={handleContextMenu}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={clsx(
            'p-2.5 rounded-lg',
            isActive ? 'bg-miracle-500/20' : 'bg-surface-secondary/50'
          )}>
            <Layers className={clsx('w-5 h-5', isActive ? 'text-miracle-400' : 'text-theme-muted')} />
          </div>
          <div>
            <h3 className="font-bold flex items-center gap-2">
              {profile.name}
              {isActive && <Check className="w-4 h-4 text-miracle-400" />}
            </h3>
            <span className={clsx('text-xs px-2 py-0.5 rounded-full', getTypeColor(profile))}>
              {getTypeLabel(profile)}
            </span>
          </div>
        </div>

        {/* Menu Button */}
        <div className="relative">
          <button
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            className="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-surface-secondary/50 transition-all"
          >
            <MoreVertical className="w-4 h-4 text-theme-muted" />
          </button>
          {showMenu && (
            <>
              <div className="fixed inset-0 z-40" onClick={(e) => {
                e.stopPropagation();
                setShowMenu(false);
              }} />
              <div className="absolute right-0 top-full mt-1 w-40 glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
                {!isActive && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelect();
                      setShowMenu(false);
                    }}
                    className="w-full px-3 py-2 text-left text-sm hover:bg-miracle-500/20 text-miracle-400 transition-colors flex items-center gap-2"
                  >
                    <Play className="w-4 h-4" />
                    Set as Active
                  </button>
                )}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDuplicate();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <Copy className="w-4 h-4" />
                  Duplicate
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onExport();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <FileDown className="w-4 h-4" />
                  Export
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onOpenFolder();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <FolderOpen className="w-4 h-4" />
                  Open Mods Folder
                </button>
                {!profile.is_default && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onDelete();
                      setShowMenu(false);
                    }}
                    className="w-full px-3 py-2 text-left text-sm hover:bg-red-500/20 text-red-400 transition-colors flex items-center gap-2"
                  >
                    <Trash2 className="w-4 h-4" />
                    Delete
                  </button>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Stats */}
      <div className="flex items-center gap-4 text-sm">
        <div className="flex items-center gap-1.5 text-theme-muted">
          <Package className="w-4 h-4" />
          <span>{profile.mods.length} mods</span>
        </div>
        <div className="flex items-center gap-1.5 text-theme-muted">
          <Clock className="w-4 h-4" />
          <span>{formatDate(profile.created_at)}</span>
        </div>
      </div>

      {/* Loading Overlay */}
      {isLoading && (
        <div className="absolute inset-0 bg-surface-primary/50 rounded-xl flex items-center justify-center">
          <Loader2 className="w-6 h-6 animate-spin text-miracle-500" />
        </div>
      )}
    </div>
  );
}

// Profile List Item Component
function ProfileListItem({
  profile,
  isActive,
  isLoading,
  onViewMods,
  onSelect,
  onDuplicate,
  onExport,
  onOpenFolder,
  onDelete,
  formatDate,
  getTypeLabel,
  getTypeColor,
}: ProfileCardProps) {
  const [showMenu, setShowMenu] = useState(false);

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setShowMenu(true);
  };

  return (
    <div
      className={clsx(
        'glass rounded-lg border p-4 transition-all cursor-pointer group flex items-center gap-4',
        isActive
          ? 'border-miracle-500 bg-miracle-500/5'
          : 'border-theme-muted/20/50 hover:border-miracle-500/50'
      )}
      onClick={onViewMods}
      onContextMenu={handleContextMenu}
    >
      {/* Icon */}
      <div className={clsx(
        'p-2.5 rounded-lg flex-shrink-0',
        isActive ? 'bg-miracle-500/20' : 'bg-surface-secondary/50'
      )}>
        <Layers className={clsx('w-5 h-5', isActive ? 'text-miracle-400' : 'text-theme-muted')} />
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <h3 className="font-medium flex items-center gap-2">
          {profile.name}
          {isActive && <Check className="w-4 h-4 text-miracle-400" />}
        </h3>
        <div className="flex items-center gap-3 mt-1">
          <span className={clsx('text-xs px-2 py-0.5 rounded-full', getTypeColor(profile))}>
            {getTypeLabel(profile)}
          </span>
          <span className="text-xs text-theme-muted">{profile.mods.length} mods</span>
          <span className="text-xs text-theme-muted">{formatDate(profile.created_at)}</span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 flex-shrink-0">
        {isActive && (
          <span className="text-xs px-2 py-1 rounded bg-miracle-500/20 text-miracle-400">
            Active
          </span>
        )}

        <div className="relative">
          <button
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            className="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-surface-secondary/50 transition-all"
          >
            <MoreVertical className="w-4 h-4 text-theme-muted" />
          </button>
          {showMenu && (
            <>
              <div className="fixed inset-0 z-40" onClick={(e) => {
                e.stopPropagation();
                setShowMenu(false);
              }} />
              <div className="absolute right-0 top-full mt-1 w-40 glass rounded-lg border border-theme-muted/20 shadow-xl z-50">
                {!isActive && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelect();
                      setShowMenu(false);
                    }}
                    className="w-full px-3 py-2 text-left text-sm hover:bg-miracle-500/20 text-miracle-400 transition-colors flex items-center gap-2"
                  >
                    <Play className="w-4 h-4" />
                    Set as Active
                  </button>
                )}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDuplicate();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <Copy className="w-4 h-4" />
                  Duplicate
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onExport();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <FileDown className="w-4 h-4" />
                  Export
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onOpenFolder();
                    setShowMenu(false);
                  }}
                  className="w-full px-3 py-2 text-left text-sm hover:bg-surface-secondary/50 transition-colors flex items-center gap-2"
                >
                  <FolderOpen className="w-4 h-4" />
                  Open Mods Folder
                </button>
                {!profile.is_default && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onDelete();
                      setShowMenu(false);
                    }}
                    className="w-full px-3 py-2 text-left text-sm hover:bg-red-500/20 text-red-400 transition-colors flex items-center gap-2"
                  >
                    <Trash2 className="w-4 h-4" />
                    Delete
                  </button>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Loading Overlay */}
      {isLoading && (
        <div className="absolute inset-0 bg-surface-primary/50 rounded-lg flex items-center justify-center">
          <Loader2 className="w-6 h-6 animate-spin text-miracle-500" />
        </div>
      )}
    </div>
  );
}

// Create Profile Modal
function CreateProfileModal({
  version,
  onClose,
  onCreated,
}: {
  version: string;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [name, setName] = useState('');
  const [profileType, setProfileType] = useState<'custom' | 'skyblock' | 'pvp'>('custom');
  const [isCreating, setIsCreating] = useState(false);

  const handleCreate = async () => {
    if (!name.trim() && profileType === 'custom') return;

    setIsCreating(true);
    try {
      if (profileType === 'custom') {
        await invoke('create_profile', {
          name: name.trim(),
          minecraftVersion: version,
          baseProfileId: null,
        });
      } else {
        await invoke('create_preset_profile', {
          minecraftVersion: version,
          presetType: profileType,
        });
      }
      onCreated();
      onClose();
    } catch (err) {
      console.error('Failed to create profile:', err);
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div onPointerDown={(e) => { if (e.target === e.currentTarget) onClose(); }} className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="glass rounded-2xl border border-theme-muted/20 shadow-2xl w-[450px] pointer-events-auto">
        <div className="p-6 border-b border-theme-muted/20/50">
          <h2 className="text-xl font-bold">Create Profile</h2>
          <p className="text-theme-muted text-sm mt-1">
            Create a new profile for {version}
          </p>
        </div>

        <div className="p-6 space-y-4">
          {/* Profile Type */}
          <div>
            <label className="block text-sm font-medium mb-2">Profile Type</label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: 'custom', label: 'Custom', desc: 'Start from scratch' },
                { id: 'skyblock', label: 'Skyblock', desc: 'Hypixel Skyblock mods' },
                { id: 'pvp', label: 'PvP', desc: 'Optimized for PvP' },
              ].map((type) => (
                <button
                  key={type.id}
                  onPointerDown={() => setProfileType(type.id as typeof profileType)}
                  className={clsx(
                    'p-3 rounded-lg border text-left transition-colors',
                    profileType === type.id
                      ? 'border-miracle-500 bg-miracle-500/10'
                      : 'border-theme-muted/20 hover:border-miracle-500/50'
                  )}
                >
                  <div className="font-medium text-sm">{type.label}</div>
                  <div className="text-xs text-theme-muted mt-0.5">{type.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Profile Name */}
          {profileType === 'custom' && (
            <div>
              <label className="block text-sm font-medium mb-2">Profile Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="My Custom Profile"
                className="w-full px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg focus:border-miracle-500 focus:outline-none transition-colors"
                autoFocus
              />
            </div>
          )}
        </div>

        <div className="p-4 border-t border-theme-muted/20/50 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 transition-colors active:scale-95"
          >
            Cancel
          </button>
          <button
            onClick={handleCreate}
            disabled={isCreating || (profileType === 'custom' && !name.trim())}
            className={clsx(
              'px-4 py-2 rounded-lg transition-colors flex items-center gap-2',
              isCreating || (profileType === 'custom' && !name.trim())
                ? 'bg-miracle-600 text-white/70 cursor-not-allowed'
                : 'bg-miracle-500 hover:bg-miracle-600 text-white'
            )}
          >
            {isCreating && <Loader2 className="w-4 h-4 animate-spin" />}
            Create Profile
          </button>
        </div>
      </div>
    </div>
  );
}
