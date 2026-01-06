import { useState, useEffect } from 'react';
import { X, Trash2, Package, Loader2, ExternalLink, RefreshCw, ArrowUp } from 'lucide-react';
import clsx from 'clsx';
import { invoke } from '@tauri-apps/api/core';
import { ModInfo } from '@/lib/stores/gameStore';
import { useModal } from '@/components/ui';

interface ModUpdateCheck {
  filename: string;
  mod_name: string;
  current_version: string;
  latest_version: string;
  latest_version_id: string;
  has_update: boolean;
  source: string;
  project_slug: string;
}

interface InstalledModsModalProps {
  isOpen: boolean;
  onClose: () => void;
  mods: ModInfo[];
  profileName: string;
  profileId: string;
  version: string;
  onToggleMod: (modId: string) => Promise<void>;
  onRemoveMod: (modId: string) => Promise<void>;
  onBrowseMore: () => void;
  onRefreshMods: () => Promise<void>;
}

export default function InstalledModsModal({
  isOpen,
  onClose,
  mods,
  profileName,
  profileId,
  version,
  onToggleMod,
  onRemoveMod,
  onBrowseMore,
  onRefreshMods,
}: InstalledModsModalProps) {
  const modal = useModal();
  const [loadingMods, setLoadingMods] = useState<Set<string>>(new Set());
  const [removingMods, setRemovingMods] = useState<Set<string>>(new Set());
  const [updates, setUpdates] = useState<ModUpdateCheck[]>([]);
  const [checkingUpdates, setCheckingUpdates] = useState(false);
  const [updatingMods, setUpdatingMods] = useState<Set<string>>(new Set());
  const [updatingAll, setUpdatingAll] = useState(false);

  // Reset updates when modal closes or profile changes
  useEffect(() => {
    if (!isOpen) {
      setUpdates([]);
    }
  }, [isOpen, profileId]);

  const checkForUpdates = async () => {
    setCheckingUpdates(true);
    try {
      const result = await invoke<ModUpdateCheck[]>('check_mod_updates', {
        version,
        profileId,
      });
      setUpdates(result);
    } catch (err) {
      console.error('Failed to check for updates:', err);
    } finally {
      setCheckingUpdates(false);
    }
  };

  const handleUpdateMod = async (filename: string) => {
    setUpdatingMods((prev) => new Set(prev).add(filename));
    try {
      await invoke('update_mod', { filename, version, profileId });
      // Remove from updates list
      setUpdates((prev) => prev.filter((u) => u.filename !== filename));
      // Refresh the mods list
      await onRefreshMods();
    } catch (err) {
      console.error('Failed to update mod:', err);
    } finally {
      setUpdatingMods((prev) => {
        const next = new Set(prev);
        next.delete(filename);
        return next;
      });
    }
  };

  const handleUpdateAll = async () => {
    setUpdatingAll(true);
    try {
      await invoke<string[]>('update_all_mods', { version, profileId });
      setUpdates([]);
      await onRefreshMods();
    } catch (err) {
      console.error('Failed to update all mods:', err);
    } finally {
      setUpdatingAll(false);
    }
  };

  // Create a map of filename to update info
  const updateMap = new Map(updates.map((u) => [u.filename, u]));

  if (!isOpen) return null;

  const handleToggle = async (modId: string) => {
    setLoadingMods((prev) => new Set(prev).add(modId));
    try {
      await onToggleMod(modId);
    } finally {
      setLoadingMods((prev) => {
        const next = new Set(prev);
        next.delete(modId);
        return next;
      });
    }
  };

  const handleRemove = async (mod: ModInfo) => {
    const confirmed = await modal.confirm({
      title: 'Remove Mod',
      message: `Are you sure you want to remove "${mod.name}" from this profile?`,
      confirmLabel: 'Remove',
      variant: 'danger',
    });

    if (!confirmed) return;

    setRemovingMods((prev) => new Set(prev).add(mod.id));
    try {
      await onRemoveMod(mod.id);
    } finally {
      setRemovingMods((prev) => {
        const next = new Set(prev);
        next.delete(mod.id);
        return next;
      });
    }
  };

  const enabledMods = mods.filter((m) => m.enabled);
  const disabledMods = mods.filter((m) => !m.enabled);

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[600px] max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-theme-muted/20/50">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold">Installed Mods</h2>
              <p className="text-theme-muted text-sm mt-1">
                Profile: {profileName} • {mods.length} mod{mods.length !== 1 ? 's' : ''}
                {updates.length > 0 && (
                  <span className="ml-2 text-miracle-400">
                    • {updates.length} update{updates.length !== 1 ? 's' : ''} available
                  </span>
                )}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {/* Check Updates Button */}
              <button
                onClick={checkForUpdates}
                disabled={checkingUpdates || mods.length === 0}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-sm flex items-center gap-2 transition-colors',
                  checkingUpdates
                    ? 'bg-surface-secondary text-theme-muted cursor-wait'
                    : 'bg-surface-secondary hover:bg-miracle-500/20 text-theme-secondary'
                )}
              >
                <RefreshCw className={clsx('w-4 h-4', checkingUpdates && 'animate-spin')} />
                {checkingUpdates ? 'Checking...' : 'Check Updates'}
              </button>
              {/* Update All Button */}
              {updates.length > 0 && (
                <button
                  onClick={handleUpdateAll}
                  disabled={updatingAll}
                  className={clsx(
                    'px-3 py-1.5 rounded-lg text-sm flex items-center gap-2 transition-colors',
                    updatingAll
                      ? 'bg-miracle-600 text-white/70 cursor-wait'
                      : 'bg-miracle-500 hover:bg-miracle-600 text-white'
                  )}
                >
                  <ArrowUp className={clsx('w-4 h-4', updatingAll && 'animate-bounce')} />
                  {updatingAll ? 'Updating...' : `Update All (${updates.length})`}
                </button>
              )}
              <button
                onClick={onClose}
                className="p-2 hover:bg-surface-secondary/50 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {mods.length === 0 ? (
            <div className="text-center py-12">
              <Package className="w-16 h-16 mx-auto text-theme-muted mb-4" />
              <h3 className="text-lg font-medium text-theme-secondary">No mods installed</h3>
              <p className="text-theme-muted mt-1 mb-4">
                Browse the catalog to find mods for your profile
              </p>
              <button
                onClick={onBrowseMore}
                className="px-4 py-2 rounded-lg bg-miracle-500 hover:bg-miracle-600 text-white transition-colors"
              >
                Browse Mods
              </button>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Enabled Mods */}
              {enabledMods.length > 0 && (
                <div>
                  <h3 className="text-sm font-medium text-theme-muted mb-3">
                    Enabled ({enabledMods.length})
                  </h3>
                  <div className="space-y-2">
                    {enabledMods.map((mod) => (
                      <ModRow
                        key={mod.id}
                        mod={mod}
                        isLoading={loadingMods.has(mod.id)}
                        isRemoving={removingMods.has(mod.id)}
                        updateInfo={updateMap.get(mod.filename)}
                        isUpdating={updatingMods.has(mod.filename)}
                        onToggle={() => handleToggle(mod.id)}
                        onRemove={() => handleRemove(mod)}
                        onUpdate={() => handleUpdateMod(mod.filename)}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Disabled Mods */}
              {disabledMods.length > 0 && (
                <div>
                  <h3 className="text-sm font-medium text-theme-muted mb-3">
                    Disabled ({disabledMods.length})
                  </h3>
                  <div className="space-y-2">
                    {disabledMods.map((mod) => (
                      <ModRow
                        key={mod.id}
                        mod={mod}
                        isLoading={loadingMods.has(mod.id)}
                        isRemoving={removingMods.has(mod.id)}
                        updateInfo={updateMap.get(mod.filename)}
                        isUpdating={updatingMods.has(mod.filename)}
                        onToggle={() => handleToggle(mod.id)}
                        onRemove={() => handleRemove(mod)}
                        onUpdate={() => handleUpdateMod(mod.filename)}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        {mods.length > 0 && (
          <div className="p-4 border-t border-theme-muted/20/50 flex justify-between items-center">
            <button
              onClick={onBrowseMore}
              className="px-4 py-2 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 transition-colors flex items-center gap-2"
            >
              <ExternalLink className="w-4 h-4" />
              Browse More Mods
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-lg bg-miracle-500 hover:bg-miracle-600 text-white transition-colors"
            >
              Done
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function ModRow({
  mod,
  isLoading,
  isRemoving,
  updateInfo,
  isUpdating,
  onToggle,
  onRemove,
  onUpdate,
}: {
  mod: ModInfo;
  isLoading: boolean;
  isRemoving: boolean;
  updateInfo?: ModUpdateCheck;
  isUpdating: boolean;
  onToggle: () => void;
  onRemove: () => void;
  onUpdate: () => void;
}) {
  return (
    <div
      className={clsx(
        'flex items-center justify-between p-3 rounded-lg border transition-colors',
        updateInfo
          ? 'bg-miracle-500/10 border-miracle-500/30'
          : mod.enabled
          ? 'bg-surface-secondary/50 border-theme-muted/20/50'
          : 'bg-surface-secondary/20 border-theme-muted/20/30 opacity-60'
      )}
    >
      <div className="flex items-center gap-3 min-w-0">
        <Package className={clsx('w-5 h-5 flex-shrink-0', updateInfo ? 'text-miracle-400' : 'text-theme-muted')} />
        <div className="min-w-0">
          <div className="font-medium truncate flex items-center gap-2">
            {mod.name}
            {updateInfo && (
              <span className="text-xs px-1.5 py-0.5 rounded bg-miracle-500/20 text-miracle-400">
                Update
              </span>
            )}
          </div>
          <div className="text-xs text-theme-muted">
            {updateInfo ? (
              <>
                {updateInfo.current_version} → <span className="text-miracle-400">{updateInfo.latest_version}</span>
              </>
            ) : (
              mod.version
            )}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2 flex-shrink-0">
        {/* Update Button */}
        {updateInfo && (
          <button
            onClick={onUpdate}
            disabled={isUpdating}
            className={clsx(
              'px-3 py-1.5 text-xs rounded-lg transition-colors flex items-center gap-1',
              isUpdating
                ? 'bg-miracle-600 text-white/70 cursor-wait'
                : 'bg-miracle-500 hover:bg-miracle-600 text-white'
            )}
          >
            {isUpdating ? (
              <Loader2 className="w-3 h-3 animate-spin" />
            ) : (
              <>
                <ArrowUp className="w-3 h-3" />
                Update
              </>
            )}
          </button>
        )}

        {/* Toggle Button */}
        <button
          onClick={onToggle}
          disabled={isLoading || isRemoving || isUpdating}
          className={clsx(
            'px-3 py-1.5 text-xs rounded-lg transition-colors',
            isLoading
              ? 'bg-surface-secondary text-theme-muted cursor-wait'
              : mod.enabled
              ? 'bg-yellow-500/20 text-yellow-400 hover:bg-yellow-500/30'
              : 'bg-green-500/20 text-green-400 hover:bg-green-500/30'
          )}
        >
          {isLoading ? (
            <Loader2 className="w-3 h-3 animate-spin" />
          ) : mod.enabled ? (
            'Disable'
          ) : (
            'Enable'
          )}
        </button>

        {/* Remove Button */}
        <button
          onClick={onRemove}
          disabled={isLoading || isRemoving || isUpdating}
          className={clsx(
            'p-1.5 rounded-lg transition-colors',
            isRemoving
              ? 'bg-surface-secondary text-theme-muted cursor-wait'
              : 'hover:bg-red-500/20 text-red-400'
          )}
          title="Remove mod"
        >
          {isRemoving ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Trash2 className="w-4 h-4" />
          )}
        </button>
      </div>
    </div>
  );
}
