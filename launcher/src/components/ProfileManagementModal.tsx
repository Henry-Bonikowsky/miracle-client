import { useState, useEffect } from 'react';
import { X, Plus, Copy, Trash2, Download, Upload, Folder, Sparkles, Globe, Hash, ChevronDown, Package, FileArchive, AlertTriangle, RefreshCw } from 'lucide-react';
import clsx from 'clsx';
import { useProfileStore, Profile } from '@/lib/stores/profileStore';
import { useAuthStore } from '@/lib/stores/authStore';
import { useGameStore } from '@/lib/stores/gameStore';
import { useModal } from '@/components/ui';
import { open } from '@tauri-apps/plugin-dialog';
import { invoke } from '@tauri-apps/api/core';

interface ModpackPreview {
  name: string;
  minecraft_version: string;
  mod_count: number;
  format: string;
  loader: string | null;
  warnings: string[];
}

interface DetectedInstance {
  name: string;
  path: string;
  source: string;
  minecraft_version: string | null;
  loader: string | null;
  mod_count: number;
}

type ToastType = 'success' | 'error' | 'warning' | 'info';

interface ProfileManagementModalProps {
  isOpen: boolean;
  selectedVersion: string;
  onClose: () => void;
  showToast: (message: string, type: ToastType, onClick?: () => void) => void;
  initialTab?: 'profiles' | 'create' | 'import';
}

export default function ProfileManagementModal({
  isOpen,
  selectedVersion,
  onClose,
  showToast,
  initialTab = 'profiles',
}: ProfileManagementModalProps) {
  const modal = useModal();
  const [activeTab, setActiveTab] = useState<'profiles' | 'create' | 'import'>(initialTab);
  const [newProfileName, setNewProfileName] = useState('');
  const [selectedPreset, setSelectedPreset] = useState<'none' | 'skyblock' | 'pvp'>('none');
  const [importCode, setImportCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [expandedProfileId, setExpandedProfileId] = useState<string | null>(null);
  const [modpackPreview, setModpackPreview] = useState<ModpackPreview | null>(null);
  const [selectedModpackPath, setSelectedModpackPath] = useState<string | null>(null);
  const [detectedInstances, setDetectedInstances] = useState<DetectedInstance[]>([]);
  const [isDetecting, setIsDetecting] = useState(false);

  const { profile: authProfile } = useAuthStore();

  const {
    profiles,
    activeProfileId,
    performanceMods,
    fetchProfiles,
    createProfile,
    createPresetProfile,
    deleteProfile,
    duplicateProfile,
    exportProfile,
    importProfile,
    setActiveProfile,
    fetchPerformanceMods,
    shareProfileOnline,
    importSharedProfile,
  } = useProfileStore();

  const { installedMods, fetchMods } = useGameStore();

  const versionProfiles = profiles.filter(p => p.version === selectedVersion);

  useEffect(() => {
    if (isOpen) {
      fetchProfiles(selectedVersion);
      fetchPerformanceMods();
      fetchMods();
      // Reset to initial tab when modal opens
      setActiveTab(initialTab);
    }
  }, [isOpen, selectedVersion, fetchProfiles, fetchPerformanceMods, fetchMods, initialTab]);

  // Refresh profiles when switching to profiles tab
  useEffect(() => {
    if (isOpen && activeTab === 'profiles') {
      fetchProfiles(selectedVersion);
    }
  }, [isOpen, activeTab, selectedVersion, fetchProfiles]);

  // Detect instances when Import tab is opened
  useEffect(() => {
    if (isOpen && activeTab === 'import' && detectedInstances.length === 0 && !isDetecting) {
      detectInstances();
    }
  }, [isOpen, activeTab]);

  const detectInstances = async () => {
    setIsDetecting(true);
    try {
      const instances = await invoke<DetectedInstance[]>('detect_installed_instances');
      setDetectedInstances(instances);
    } catch (error) {
      console.error('Failed to detect instances:', error);
    } finally {
      setIsDetecting(false);
    }
  };

  const handleImportDetectedInstance = async (instance: DetectedInstance) => {
    setIsLoading(true);
    try {
      interface ImportResult {
        profile_id: string;
        name: string;
        mods_installed: number;
        mods_failed: number;
        warnings: string[];
      }

      await invoke<ImportResult>('import_modpack_file', {
        filePath: instance.path,
      });

      const importedVersion = instance.minecraft_version || 'unknown version';
      showToast(
        `Imported "${instance.name}" for MC ${importedVersion}. ${importedVersion !== selectedVersion ? `Switch to ${importedVersion} to see it.` : ''}`,
        'success'
      );
      setActiveTab('profiles');
      // Fetch profiles for both current and imported version
      fetchProfiles(selectedVersion);
      if (instance.minecraft_version && instance.minecraft_version !== selectedVersion) {
        fetchProfiles(instance.minecraft_version);
      }
    } catch (error) {
      showToast(`Failed to import: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateProfile = async () => {
    if (!newProfileName.trim()) {
      showToast('Please enter a profile name', 'error');
      return;
    }

    setIsLoading(true);
    try {
      if (selectedPreset === 'none') {
        await createProfile(newProfileName.trim(), selectedVersion);
        showToast(`Created profile "${newProfileName}"`, 'success');
      } else {
        await createPresetProfile(selectedVersion, selectedPreset);
        showToast(`Created ${selectedPreset} preset profile`, 'success');
      }
      setNewProfileName('');
      setSelectedPreset('none');
      setActiveTab('profiles');
    } catch (error) {
      showToast(`Failed to create profile: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteProfile = async (profile: Profile) => {
    if (profile.is_default) {
      showToast('Cannot delete the default profile', 'error');
      return;
    }

    const confirmed = await modal.confirm({
      title: 'Delete Profile',
      message: `Are you sure you want to delete "${profile.name}"? This will remove all mods in this profile.`,
      confirmLabel: 'Delete',
      variant: 'danger',
    });

    if (!confirmed) return;

    setIsLoading(true);
    try {
      await deleteProfile(profile.id);
      showToast(`Deleted profile "${profile.name}"`, 'success');
    } catch (error) {
      showToast(`Failed to delete profile: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDuplicateProfile = async (profile: Profile) => {
    const newName = await modal.prompt({
      title: 'Duplicate Profile',
      message: 'Enter a name for the duplicated profile:',
      placeholder: 'Profile name',
      defaultValue: `${profile.name} (Copy)`,
      confirmLabel: 'Duplicate',
    });

    if (!newName) return;

    setIsLoading(true);
    try {
      await duplicateProfile(profile.id, newName);
      showToast(`Duplicated profile as "${newName}"`, 'success');
    } catch (error) {
      showToast(`Failed to duplicate profile: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleExportProfile = async (profile: Profile) => {
    setIsLoading(true);
    try {
      const exported = await exportProfile(profile.id);
      const code = btoa(JSON.stringify(exported));
      await navigator.clipboard.writeText(code);
      showToast('Profile code copied to clipboard! Share it with friends.', 'success');
    } catch (error) {
      showToast(`Failed to export profile: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleShareOnline = async (profile: Profile) => {
    setIsLoading(true);
    try {
      const result = await shareProfileOnline(
        profile.id,
        authProfile?.id,
        authProfile?.name
      );

      if (result.success && result.short_code) {
        await navigator.clipboard.writeText(result.short_code);
        showToast(`Share code: ${result.short_code} (copied to clipboard!)`, 'success');
      } else {
        showToast(result.message || 'Failed to share profile', 'error');
      }
    } catch (error) {
      showToast(`Failed to share profile: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleImportProfile = async () => {
    if (!importCode.trim()) {
      showToast('Please enter a profile code', 'error');
      return;
    }

    setIsLoading(true);
    try {
      const code = importCode.trim();

      // Check if it's a short code (8 alphanumeric chars) or a base64 code
      const isShortCode = /^[A-Z0-9]{8}$/i.test(code);

      if (isShortCode) {
        // Import from Supabase using short code
        const profile = await importSharedProfile(code, selectedVersion);
        showToast(`Imported shared profile "${profile.name}"`, 'success');
      } else {
        // Try base64 decode
        const decoded = JSON.parse(atob(code));
        const profile = await importProfile(decoded.name, selectedVersion, decoded.mods);
        showToast(`Imported profile "${profile.name}"`, 'success');
      }

      setImportCode('');
      setActiveTab('profiles');
    } catch (error) {
      showToast('Invalid profile code. Please check and try again.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSetActive = async (profile: Profile) => {
    try {
      await setActiveProfile(selectedVersion, profile.id);
      showToast(`Switched to "${profile.name}"`, 'success');
    } catch (error) {
      showToast(`Failed to switch profile: ${error}`, 'error');
    }
  };

  const handleSelectModpackFile = async () => {
    try {
      const path = await open({
        filters: [
          { name: 'Modpacks', extensions: ['mrpack', 'zip'] },
        ],
        title: 'Select Modpack File',
      });

      if (!path) return;

      setIsLoading(true);
      setSelectedModpackPath(path as string);

      const preview = await invoke<ModpackPreview>('preview_modpack_file', {
        filePath: path,
      });

      setModpackPreview(preview);
    } catch (error) {
      showToast(`Failed to read modpack: ${error}`, 'error');
      setModpackPreview(null);
      setSelectedModpackPath(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectModpackFolder = async () => {
    try {
      const path = await open({
        directory: true,
        title: 'Select MultiMC/Prism Instance Folder',
      });

      if (!path) return;

      setIsLoading(true);
      setSelectedModpackPath(path as string);

      const preview = await invoke<ModpackPreview>('preview_modpack_file', {
        filePath: path,
      });

      setModpackPreview(preview);
    } catch (error) {
      showToast(`Failed to read instance: ${error}`, 'error');
      setModpackPreview(null);
      setSelectedModpackPath(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleImportModpackFile = async () => {
    if (!selectedModpackPath) return;

    setIsLoading(true);
    try {
      interface ImportResult {
        profile_id: string;
        name: string;
        mods_installed: number;
        mods_failed: number;
        warnings: string[];
      }

      const result = await invoke<ImportResult>('import_modpack_file', {
        filePath: selectedModpackPath,
      });

      showToast(`Imported "${result.name}" successfully!`, 'success');
      setModpackPreview(null);
      setSelectedModpackPath(null);
      setActiveTab('profiles');
      fetchProfiles(selectedVersion);
    } catch (error) {
      showToast(`Failed to import modpack: ${error}`, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancelModpackImport = () => {
    setModpackPreview(null);
    setSelectedModpackPath(null);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[600px] max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-theme-muted/20/50">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold">Manage Profiles</h2>
              <p className="text-theme-muted text-sm mt-1">
                Version {selectedVersion}
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-surface-secondary/50 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Tabs */}
          <div className="flex gap-2 mt-4 items-center">
            {(['profiles', 'create', 'import'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={clsx(
                  'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                  activeTab === tab
                    ? 'bg-miracle-500 text-theme-primary'
                    : 'bg-surface-secondary/50 text-theme-secondary hover:bg-miracle-500/20'
                )}
              >
                {tab === 'profiles' && 'Profiles'}
                {tab === 'create' && 'Create New'}
                {tab === 'import' && 'Import'}
              </button>
            ))}
            <button
              onClick={() => {
                fetchProfiles(selectedVersion);
                fetchMods();
                showToast('Profiles refreshed', 'info');
              }}
              className="ml-auto p-2 rounded-lg hover:bg-surface-secondary/50 text-theme-muted hover:text-theme-secondary transition-colors"
              title="Refresh profiles"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {/* Profiles Tab */}
          {activeTab === 'profiles' && (
            <div className="space-y-3">
              {/* Performance Mods Notice */}
              <div className="glass rounded-lg border border-miracle-500/30 bg-miracle-500/10 p-4 mb-4">
                <div className="flex items-center gap-2 text-miracle-400 font-medium mb-2">
                  <Sparkles className="w-4 h-4" />
                  Performance Mods (Auto-included)
                </div>
                <p className="text-sm text-theme-secondary">
                  Every profile includes: {performanceMods.join(', ') || 'Loading...'}
                </p>
              </div>

              {versionProfiles.length === 0 ? (
                <div className="text-center py-8 text-theme-muted">
                  <Folder className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>No profiles yet</p>
                  <p className="text-sm mt-1">Create one to get started!</p>
                </div>
              ) : (
                versionProfiles.map((profile) => {
                  const isExpanded = expandedProfileId === profile.id;
                  return (
                    <div
                      key={profile.id}
                      className={clsx(
                        'glass rounded-lg border transition-colors',
                        profile.id === activeProfileId
                          ? 'border-miracle-500/50 bg-miracle-500/5'
                          : 'border-theme-muted/20/50'
                      )}
                    >
                      <div className="p-4">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <button
                              onClick={() => setExpandedProfileId(isExpanded ? null : profile.id)}
                              className="p-1 hover:bg-surface-secondary/50 rounded transition-colors"
                            >
                              <ChevronDown className={clsx('w-4 h-4 transition-transform', isExpanded && 'rotate-180')} />
                            </button>
                            <div>
                              <div className="font-medium flex items-center gap-2">
                                {profile.name}
                                {profile.id === activeProfileId && (
                                  <span className="text-xs px-2 py-0.5 rounded bg-miracle-500/20 text-miracle-400">
                                    Active
                                  </span>
                                )}
                                {profile.is_default && (
                                  <span className="text-xs px-2 py-0.5 rounded bg-surface-secondary text-theme-secondary">
                                    Default
                                  </span>
                                )}
                                {profile.is_preset && (
                                  <span className="text-xs px-2 py-0.5 rounded bg-purple-500/20 text-purple-400">
                                    {profile.preset_type}
                                  </span>
                                )}
                              </div>
                              <button
                                onClick={() => setExpandedProfileId(isExpanded ? null : profile.id)}
                                className="text-sm text-theme-muted mt-1 hover:text-theme-secondary transition-colors"
                              >
                                {profile.id === activeProfileId ? installedMods.length : profile.mods.length} mod{(profile.id === activeProfileId ? installedMods.length : profile.mods.length) !== 1 ? 's' : ''} + performance mods
                                {!isExpanded && ' (click to view)'}
                              </button>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            {profile.id !== activeProfileId && (
                              <button
                                onClick={() => handleSetActive(profile)}
                                className="px-3 py-1.5 text-xs rounded-lg bg-miracle-500/20 text-miracle-400 hover:bg-miracle-500/30 transition-colors"
                              >
                                Use
                              </button>
                            )}
                            <button
                              onClick={() => handleShareOnline(profile)}
                              className="p-2 rounded-lg hover:bg-green-500/20 text-green-400 transition-colors"
                              title="Share Online (Get Short Code)"
                            >
                              <Globe className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleExportProfile(profile)}
                              className="p-2 rounded-lg hover:bg-surface-secondary/50 transition-colors"
                              title="Export (Copy Full Code)"
                            >
                              <Upload className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDuplicateProfile(profile)}
                              className="p-2 rounded-lg hover:bg-surface-secondary/50 transition-colors"
                              title="Duplicate"
                            >
                              <Copy className="w-4 h-4" />
                            </button>
                            {!profile.is_default && (
                              <button
                                onClick={() => handleDeleteProfile(profile)}
                                className="p-2 rounded-lg hover:bg-red-500/20 text-red-400 transition-colors"
                                title="Delete"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Expanded Mods List */}
                      {isExpanded && (
                        <div className="border-t border-theme-muted/20/50 p-4 bg-surface-secondary/30">
                          <div className="text-xs text-theme-muted mb-2 font-medium">Installed Mods:</div>
                          {profile.id === activeProfileId ? (
                            // Show actual installed mods for active profile
                            installedMods.length === 0 ? (
                              <p className="text-sm text-theme-muted italic">No mods installed</p>
                            ) : (
                              <div className="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto">
                                {installedMods.map((mod) => (
                                  <div
                                    key={mod.id}
                                    className={clsx(
                                      'flex items-center gap-2 px-2 py-1.5 rounded text-sm',
                                      mod.enabled ? 'bg-surface-secondary/50' : 'bg-surface-secondary/30 opacity-50'
                                    )}
                                  >
                                    <Package className="w-3 h-3 text-theme-muted flex-shrink-0" />
                                    <span className="truncate">{mod.name}</span>
                                    {!mod.enabled && <span className="text-xs text-theme-muted">(off)</span>}
                                  </div>
                                ))}
                              </div>
                            )
                          ) : (
                            // Show profile.mods for non-active profiles
                            profile.mods.length === 0 ? (
                              <p className="text-sm text-theme-muted italic">No mod data (activate profile to see mods)</p>
                            ) : (
                              <div className="grid grid-cols-2 gap-2">
                                {profile.mods.map((mod) => (
                                  <div
                                    key={mod}
                                    className="flex items-center gap-2 px-2 py-1.5 rounded bg-surface-secondary/50 text-sm"
                                  >
                                    <Package className="w-3 h-3 text-theme-muted" />
                                    <span className="truncate">{mod}</span>
                                  </div>
                                ))}
                              </div>
                            )
                          )}
                          <div className="text-xs text-theme-muted mt-3 mb-2 font-medium">Performance Mods (auto-included):</div>
                          <div className="grid grid-cols-2 gap-2">
                            {performanceMods.map((mod) => (
                              <div
                                key={mod}
                                className="flex items-center gap-2 px-2 py-1.5 rounded bg-miracle-500/10 text-sm text-miracle-400"
                              >
                                <Sparkles className="w-3 h-3" />
                                <span className="truncate">{mod}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* Create Tab */}
          {activeTab === 'create' && (
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium mb-2">Profile Name</label>
                <input
                  type="text"
                  value={newProfileName}
                  onChange={(e) => setNewProfileName(e.target.value)}
                  placeholder="My Custom Profile"
                  className="w-full px-4 py-3 rounded-lg bg-surface-secondary border border-theme-muted/20 focus:border-miracle-500 focus:outline-none transition-colors"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">Start From</label>
                <div className="grid grid-cols-3 gap-3">
                  {[
                    { id: 'none', name: 'Empty', desc: 'Start fresh' },
                    { id: 'skyblock', name: 'Skyblock', desc: 'Hypixel mods' },
                    { id: 'pvp', name: 'PvP', desc: 'Competitive' },
                  ].map((preset) => (
                    <button
                      key={preset.id}
                      onClick={() => setSelectedPreset(preset.id as typeof selectedPreset)}
                      className={clsx(
                        'p-4 rounded-lg border text-left transition-colors',
                        selectedPreset === preset.id
                          ? 'border-miracle-500 bg-miracle-500/10'
                          : 'border-theme-muted/20 hover:border-miracle-500/30'
                      )}
                    >
                      <div className="font-medium">{preset.name}</div>
                      <div className="text-xs text-theme-muted mt-1">{preset.desc}</div>
                    </button>
                  ))}
                </div>
              </div>

              <button
                onClick={handleCreateProfile}
                disabled={isLoading || (!newProfileName.trim() && selectedPreset === 'none')}
                className={clsx(
                  'w-full py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2',
                  isLoading || (!newProfileName.trim() && selectedPreset === 'none')
                    ? 'bg-surface-secondary text-theme-muted cursor-not-allowed'
                    : 'bg-miracle-500 hover:bg-miracle-600 text-theme-primary'
                )}
              >
                <Plus className="w-4 h-4" />
                Create Profile
              </button>
            </div>
          )}

          {/* Import Tab */}
          {activeTab === 'import' && (
            <div className="space-y-6">
              {/* Detected Instances Section */}
              {(detectedInstances.length > 0 || isDetecting) && (
                <div className="glass rounded-lg border border-green-500/30 bg-green-500/5 p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2 text-green-400 font-medium">
                      <Sparkles className="w-4 h-4" />
                      Detected Instances
                    </div>
                    <button
                      onClick={detectInstances}
                      disabled={isDetecting}
                      className="text-xs text-theme-muted hover:text-theme-secondary transition-colors"
                    >
                      {isDetecting ? 'Scanning...' : 'Refresh'}
                    </button>
                  </div>

                  {isDetecting ? (
                    <p className="text-sm text-theme-secondary">Scanning for installed instances...</p>
                  ) : (
                    <div className="space-y-2 max-h-48 overflow-y-auto">
                      {detectedInstances.map((instance, i) => (
                        <div
                          key={i}
                          className="flex items-center justify-between p-3 rounded-lg bg-surface-secondary/50 hover:bg-surface-secondary/70 transition-colors"
                        >
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="font-medium truncate">{instance.name}</span>
                              <span className={clsx(
                                'text-xs px-1.5 py-0.5 rounded',
                                instance.source === 'modrinth' && 'bg-green-500/20 text-green-400',
                                instance.source === 'curseforge' && 'bg-orange-500/20 text-orange-400',
                                instance.source === 'prism' && 'bg-blue-500/20 text-blue-400',
                                instance.source === 'multimc' && 'bg-purple-500/20 text-purple-400',
                                instance.source === 'atlauncher' && 'bg-cyan-500/20 text-cyan-400',
                              )}>
                                {instance.source}
                              </span>
                            </div>
                            <div className="text-xs text-theme-muted mt-0.5">
                              <span className={instance.minecraft_version ? 'text-miracle-400' : 'text-yellow-400'}>
                                {instance.minecraft_version ? `MC ${instance.minecraft_version}` : 'Unknown version'}
                              </span>
                              {instance.loader && ` · ${instance.loader}`}
                              {instance.mod_count > 0 && ` · ${instance.mod_count} mods`}
                            </div>
                          </div>
                          <button
                            onClick={() => handleImportDetectedInstance(instance)}
                            disabled={isLoading}
                            className="ml-3 px-3 py-1.5 text-xs rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors font-medium"
                          >
                            Import
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Manual Import Section */}
              <div className="glass rounded-lg border border-miracle-500/30 bg-miracle-500/5 p-4">
                <div className="flex items-center gap-2 text-miracle-400 font-medium mb-3">
                  <FileArchive className="w-4 h-4" />
                  {detectedInstances.length > 0 ? 'Or Import Manually' : 'Import from Other Launchers'}
                </div>

                {!modpackPreview ? (
                  <div className="space-y-3">
                    <p className="text-sm text-theme-secondary">
                      Select a modpack file or instance folder to import.
                    </p>
                    <div className="flex gap-2">
                      <button
                        onClick={handleSelectModpackFile}
                        disabled={isLoading}
                        className="flex-1 py-2.5 rounded-lg bg-miracle-500/20 text-miracle-400 hover:bg-miracle-500/30 transition-colors flex items-center justify-center gap-2 text-sm font-medium"
                      >
                        <FileArchive className="w-4 h-4" />
                        Select .mrpack / .zip
                      </button>
                      <button
                        onClick={handleSelectModpackFolder}
                        disabled={isLoading}
                        className="flex-1 py-2.5 rounded-lg bg-surface-secondary/50 text-theme-secondary hover:bg-miracle-500/20 transition-colors flex items-center justify-center gap-2 text-sm font-medium"
                      >
                        <Folder className="w-4 h-4" />
                        MultiMC Folder
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {/* Preview Card */}
                    <div className="bg-surface-secondary/50 rounded-lg p-3">
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-medium">{modpackPreview.name}</span>
                        <span className="text-xs px-2 py-0.5 rounded bg-surface-secondary text-theme-secondary">
                          {modpackPreview.format}
                        </span>
                      </div>
                      <div className="text-sm text-theme-muted space-y-1">
                        <div>Minecraft {modpackPreview.minecraft_version}</div>
                        <div>{modpackPreview.mod_count} mods</div>
                        {modpackPreview.loader && (
                          <div>Loader: {modpackPreview.loader}</div>
                        )}
                      </div>
                    </div>

                    {/* Warnings */}
                    {modpackPreview.warnings.length > 0 && (
                      <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-3">
                        <div className="flex items-center gap-2 text-yellow-400 text-sm font-medium mb-1">
                          <AlertTriangle className="w-4 h-4" />
                          Warnings
                        </div>
                        <ul className="text-xs text-yellow-300/80 space-y-1">
                          {modpackPreview.warnings.map((warning, i) => (
                            <li key={i}>{warning}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Actions */}
                    <div className="flex gap-2">
                      <button
                        onClick={handleCancelModpackImport}
                        className="flex-1 py-2 rounded-lg bg-surface-secondary/50 text-theme-secondary hover:bg-miracle-500/20 transition-colors text-sm"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handleImportModpackFile}
                        disabled={isLoading}
                        className="flex-1 py-2 rounded-lg bg-miracle-500 text-theme-primary hover:bg-miracle-600 transition-colors text-sm font-medium flex items-center justify-center gap-2"
                      >
                        {isLoading ? (
                          <>Importing...</>
                        ) : (
                          <>
                            <Download className="w-4 h-4" />
                            Import Modpack
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-theme-muted/20/50"></div>
                </div>
                <div className="relative flex justify-center">
                  <span className="bg-surface-primary px-3 text-xs text-theme-muted">or import a shared profile</span>
                </div>
              </div>

              {/* Profile Code Import */}
              <div>
                <label className="block text-sm font-medium mb-2">Profile Code</label>
                <div className="relative">
                  <input
                    type="text"
                    value={importCode}
                    onChange={(e) => setImportCode(e.target.value.toUpperCase())}
                    placeholder="Enter 8-character code (e.g., ABC12345)"
                    maxLength={8}
                    className="w-full px-4 py-3 rounded-lg bg-surface-secondary border border-theme-muted/20 focus:border-miracle-500 focus:outline-none transition-colors text-center text-lg tracking-widest font-mono"
                  />
                  <Hash className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-theme-muted" />
                </div>
                <p className="text-xs text-theme-muted mt-2 text-center">
                  Or paste a full export code below
                </p>
                <textarea
                  value={importCode.length > 8 ? importCode : ''}
                  onChange={(e) => setImportCode(e.target.value)}
                  placeholder="Paste full export code here..."
                  rows={3}
                  className="w-full mt-2 px-4 py-3 rounded-lg bg-surface-secondary border border-theme-muted/20 focus:border-miracle-500 focus:outline-none transition-colors resize-none text-xs"
                />
              </div>

              <button
                onClick={handleImportProfile}
                disabled={isLoading || !importCode.trim()}
                className={clsx(
                  'w-full py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2',
                  isLoading || !importCode.trim()
                    ? 'bg-surface-secondary text-theme-muted cursor-not-allowed'
                    : 'bg-miracle-500 hover:bg-miracle-600 text-theme-primary'
                )}
              >
                <Download className="w-4 h-4" />
                Import Profile
              </button>

              <div className="glass rounded-lg border border-theme-muted/20/50 p-4 text-sm text-theme-secondary">
                <p className="font-medium text-theme-primary mb-2">Supported formats:</p>
                <ul className="space-y-1 list-disc list-inside text-xs">
                  <li><span className="text-green-400">.mrpack</span> - Modrinth modpacks</li>
                  <li><span className="text-orange-400">.zip</span> - CurseForge modpacks</li>
                  <li><span className="text-blue-400">Folder</span> - MultiMC/Prism Launcher instances</li>
                  <li><span className="text-purple-400">Short code</span> - 8-character share code (e.g., ABC12345)</li>
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
