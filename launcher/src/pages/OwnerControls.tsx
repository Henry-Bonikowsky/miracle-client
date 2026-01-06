/**
 * Owner Controls - Admin/Debug Panel
 * Only accessible by the owner
 */

import { useState } from 'react';
import { Shield, Users, Package, Bug, Database, Settings, Trash2 } from 'lucide-react';
import { useToast } from '@/components/ToastContainer';
import { invoke } from '@tauri-apps/api/core';

export default function OwnerControlsPage() {
  const { showToast } = useToast();
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const handleDeleteAllMods = async () => {
    setShowDeleteModal(false);
    setIsDeleting(true);
    try {
      const result = await invoke<string>('delete_all_mod_folders');
      showToast(result, 'success');
    } catch (error) {
      showToast(`Failed to delete mod folders: ${error}`, 'error');
    } finally {
      setIsDeleting(false);
    }
  };
  return (
    <div className="h-full flex flex-col p-6 overflow-y-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Shield className="w-6 h-6 text-yellow-400" />
          <span className="gradient-text">Owner Controls</span>
        </h1>
        <p className="text-theme-muted mt-1">Administrative tools and debugging</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Rank Management */}
        <div className="glass rounded-xl border border-theme-muted/20/50 p-6 hover:border-miracle-500/30 transition-colors">
          <Users className="w-8 h-8 text-miracle-400 mb-4" />
          <h2 className="text-lg font-semibold mb-2">Rank Management</h2>
          <p className="text-sm text-theme-muted mb-4">
            Manually assign ranks to users
          </p>
          <button className="w-full py-2 rounded-lg bg-miracle-600 hover:bg-miracle-500 transition-colors text-sm font-medium">
            Manage Ranks
          </button>
        </div>

        {/* Cosmetics Management */}
        <div className="glass rounded-xl border border-theme-muted/20/50 p-6 hover:border-miracle-500/30 transition-colors">
          <Package className="w-8 h-8 text-miracle-400 mb-4" />
          <h2 className="text-lg font-semibold mb-2">Cosmetics</h2>
          <p className="text-sm text-theme-muted mb-4">
            Add, edit, or remove cosmetics
          </p>
          <button className="w-full py-2 rounded-lg bg-miracle-600 hover:bg-miracle-500 transition-colors text-sm font-medium">
            Manage Cosmetics
          </button>
        </div>

        {/* Debug Tools */}
        <div className="glass rounded-xl border border-theme-muted/20/50 p-6 hover:border-miracle-500/30 transition-colors">
          <Bug className="w-8 h-8 text-miracle-400 mb-4" />
          <h2 className="text-lg font-semibold mb-2">Debug Tools</h2>
          <p className="text-sm text-theme-muted mb-4">
            Testing and debugging utilities
          </p>
          <div className="space-y-2">
            <button
              onClick={() => setShowDeleteModal(true)}
              disabled={isDeleting}
              className="w-full py-2 rounded-lg bg-red-600 hover:bg-red-500 transition-colors text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              <Trash2 className="w-4 h-4" />
              {isDeleting ? 'Deleting...' : 'Delete All Mod Folders'}
            </button>
          </div>
        </div>

        {/* Database */}
        <div className="glass rounded-xl border border-theme-muted/20/50 p-6 hover:border-miracle-500/30 transition-colors">
          <Database className="w-8 h-8 text-miracle-400 mb-4" />
          <h2 className="text-lg font-semibold mb-2">Database</h2>
          <p className="text-sm text-theme-muted mb-4">
            View and manage user data
          </p>
          <button className="w-full py-2 rounded-lg bg-miracle-600 hover:bg-miracle-500 transition-colors text-sm font-medium">
            Open Database
          </button>
        </div>

        {/* System Settings */}
        <div className="glass rounded-xl border border-theme-muted/20/50 p-6 hover:border-miracle-500/30 transition-colors">
          <Settings className="w-8 h-8 text-miracle-400 mb-4" />
          <h2 className="text-lg font-semibold mb-2">System Settings</h2>
          <p className="text-sm text-theme-muted mb-4">
            Configure launcher settings
          </p>
          <button className="w-full py-2 rounded-lg bg-miracle-600 hover:bg-miracle-500 transition-colors text-sm font-medium">
            Configure
          </button>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[500px]">
            <div className="p-6 border-b border-theme-muted/20/50">
              <h2 className="text-xl font-bold text-red-400 flex items-center gap-2">
                <Trash2 className="w-5 h-5" />
                Delete All Mod Folders
              </h2>
            </div>

            <div className="p-6 space-y-4">
              <p className="text-white">
                Are you sure you want to delete <span className="text-red-400 font-semibold">ALL mod folders</span> for <span className="text-red-400 font-semibold">ALL versions</span>?
              </p>
              <p className="text-theme-muted text-sm">
                This will permanently remove all downloaded mods from every Minecraft version. This action cannot be undone.
              </p>
            </div>

            <div className="p-6 border-t border-theme-muted/20/50 flex gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="flex-1 py-2 rounded-lg bg-surface-secondary hover:bg-miracle-500/10 transition-colors text-sm font-medium"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteAllMods}
                className="flex-1 py-2 rounded-lg bg-red-600 hover:bg-red-500 transition-colors text-sm font-medium flex items-center justify-center gap-2"
              >
                <Trash2 className="w-4 h-4" />
                Delete All
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
