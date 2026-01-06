import { getCurrentWindow } from '@tauri-apps/api/window';
import { Minus, Square, X } from 'lucide-react';

const appWindow = getCurrentWindow();

export default function Titlebar() {
  const handleMinimize = async () => await appWindow.minimize();
  const handleMaximize = async () => await appWindow.toggleMaximize();
  const handleClose = async () => await appWindow.close();

  return (
    <div className="h-8 bg-surface-primary flex items-center justify-between border-b border-miracle-500/30 relative z-50">
      <div
        data-tauri-drag-region
        className="flex items-center gap-2 px-3 flex-1 h-full"
      >
        <div className="w-4 h-4 rounded bg-gradient-to-br from-miracle-400 to-miracle-600 shadow-lg shadow-miracle-400/50" />
        <span className="text-xs font-semibold gradient-text">
          Miracle Client
        </span>
      </div>

      <div className="flex items-center h-full">
        <button
          onClick={handleMinimize}
          className="w-12 h-full flex items-center justify-center hover:bg-surface-secondary transition-colors"
          type="button"
        >
          <Minus className="w-4 h-4 text-theme-muted" />
        </button>
        <button
          onClick={handleMaximize}
          className="w-12 h-full flex items-center justify-center hover:bg-surface-secondary transition-colors"
          type="button"
        >
          <Square className="w-3 h-3 text-theme-muted" />
        </button>
        <button
          onClick={handleClose}
          className="w-12 h-full flex items-center justify-center hover:bg-red-600 transition-colors group"
          type="button"
        >
          <X className="w-4 h-4 text-theme-muted group-hover:text-white" />
        </button>
      </div>
    </div>
  );
}
