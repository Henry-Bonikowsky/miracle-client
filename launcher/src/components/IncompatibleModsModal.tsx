import { X, AlertTriangle, XCircle } from 'lucide-react';
import { ModInfo } from '@/lib/stores/gameStore';

interface IncompatibleModsModalProps {
  incompatibleMods: ModInfo[];
  targetVersion: string;
  onClose: () => void;
  onConfirm: () => void;
}

export default function IncompatibleModsModal({
  incompatibleMods,
  targetVersion,
  onClose,
  onConfirm,
}: IncompatibleModsModalProps) {
  return (
    <div className="fixed inset-0 z-[9998] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[600px] max-h-[70vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-6 border-b border-theme-muted/20/50 flex items-start justify-between flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-full bg-yellow-500/20 border border-yellow-500/30 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6 text-yellow-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold">Incompatible Mods</h2>
              <p className="text-sm text-theme-muted mt-0.5">
                Not available for Minecraft {targetVersion}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-surface-secondary/50 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <p className="text-theme-secondary mb-4 leading-relaxed">
            The following mods don't have versions available for Minecraft <span className="text-theme-primary font-semibold">{targetVersion}</span>.
            They won't be available in this version, but will remain in your other versions.
          </p>

          <div className="space-y-2">
            {incompatibleMods.map((mod) => (
              <div
                key={mod.id}
                className="glass rounded-lg border border-theme-muted/20/30 p-4 flex items-center gap-3"
              >
                <XCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                <div className="flex-1">
                  <div className="font-semibold text-theme-primary">{mod.name}</div>
                  <div className="text-xs text-theme-muted mt-1">v{mod.version}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-theme-muted/20/50 flex gap-3 flex-shrink-0">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-3 rounded-lg border border-theme-muted/20/50 hover:bg-surface-secondary/50 text-theme-secondary hover:text-theme-primary transition-all font-medium"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 px-4 py-3 rounded-lg bg-gradient-to-r from-miracle-500 to-miracle-600 hover:from-miracle-400 hover:to-miracle-500 text-theme-primary font-medium transition-all shadow-lg"
          >
            Continue without these mods
          </button>
        </div>
      </div>
    </div>
  );
}
