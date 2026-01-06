import { useState } from 'react';
import { useGameStore } from '@/lib/stores/gameStore';
import { useThemeStore, themes } from '@/lib/stores/themeStore';
import { Folder, HardDrive, Cpu, Monitor, Terminal, Palette, Check } from 'lucide-react';
import clsx from 'clsx';
import { open } from '@tauri-apps/plugin-dialog';

export default function SettingsPage() {
  const { allocatedRam, setAllocatedRam, javaPath, gameDirectory, showGameLogs, setShowGameLogs } =
    useGameStore();
  const { currentTheme, setTheme } = useThemeStore();
  const [localRam, setLocalRam] = useState(allocatedRam);

  const handleSelectJava = async () => {
    const path = await open({
      filters: [{ name: 'Java', extensions: ['exe'] }],
    });
    if (path) {
      // Update java path in store
    }
  };

  const handleSelectGameDir = async () => {
    const path = await open({
      directory: true,
    });
    if (path) {
      // Update game directory in store
    }
  };

  const handleRamChange = (value: number) => {
    setLocalRam(value);
    setAllocatedRam(value);
  };

  return (
    <div className="h-full p-6 overflow-auto">
      <h1 className="text-2xl font-bold mb-6 text-theme-primary">Settings</h1>

      <div className="max-w-3xl space-y-6">
        {/* Theme */}
        <SettingsSection title="Theme" icon={Palette}>
          <div className="grid grid-cols-5 gap-3">
            {themes.map((theme) => (
              <button
                key={theme.id}
                onClick={() => setTheme(theme.id)}
                className={clsx(
                  'relative flex flex-col items-center gap-2 p-3 rounded-lg border transition-all',
                  currentTheme === theme.id
                    ? 'border-miracle-500 bg-miracle-500/10'
                    : 'border-theme-muted/20 hover:border-miracle-500/30 bg-surface-secondary/50 hover:bg-surface-secondary'
                )}
              >
                {/* Color preview circle */}
                <div
                  className="w-10 h-10 rounded-full border-2 border-theme-muted/30"
                  style={{
                    background: `linear-gradient(135deg, ${theme.colors.bgFrom} 0%, ${theme.colors.accent500} 100%)`,
                  }}
                />
                <span className="text-xs font-medium">{theme.name}</span>
                {currentTheme === theme.id && (
                  <div className="absolute top-1 right-1 w-4 h-4 rounded-full bg-miracle-500 flex items-center justify-center">
                    <Check className="w-3 h-3 text-white" />
                  </div>
                )}
              </button>
            ))}
          </div>
        </SettingsSection>

        {/* Game Settings */}
        <SettingsSection title="Game Settings" icon={Monitor}>
          <SettingsItem
            label="Game Directory"
            description="Where Minecraft files are stored"
          >
            <button
              onClick={handleSelectGameDir}
              className="flex items-center gap-2 px-4 py-2 bg-surface-secondary hover:bg-miracle-500/20 rounded-lg transition-colors text-theme-primary"
            >
              <Folder className="w-4 h-4" />
              <span className="text-sm">
                {gameDirectory || 'Select folder'}
              </span>
            </button>
          </SettingsItem>
        </SettingsSection>

        {/* Performance Settings */}
        <SettingsSection title="Performance" icon={Cpu}>
          <SettingsItem
            label="Allocated RAM"
            description="Amount of memory allocated to Minecraft"
          >
            <div className="flex items-center gap-4">
              <input
                type="range"
                min={2048}
                max={16384}
                step={512}
                value={localRam}
                onChange={(e) => handleRamChange(Number(e.target.value))}
                className="flex-1 h-2 bg-surface-secondary rounded-lg appearance-none cursor-pointer accent-miracle-500"
              />
              <div className="w-24 text-right">
                <span className="font-semibold text-theme-primary">{localRam / 1024}</span>
                <span className="text-theme-muted text-sm"> GB</span>
              </div>
            </div>
          </SettingsItem>
        </SettingsSection>

        {/* Java Settings */}
        <SettingsSection title="Java" icon={HardDrive}>
          <SettingsItem
            label="Java Path"
            description="Path to Java executable (leave empty for auto-detect)"
          >
            <button
              onClick={handleSelectJava}
              className="flex items-center gap-2 px-4 py-2 bg-surface-secondary hover:bg-miracle-500/20 rounded-lg transition-colors text-theme-primary"
            >
              <Folder className="w-4 h-4" />
              <span className="text-sm truncate max-w-xs">
                {javaPath || 'Auto-detect'}
              </span>
            </button>
          </SettingsItem>
        </SettingsSection>

        {/* Developer */}
        <SettingsSection title="Developer" icon={Terminal}>
          <SettingsItem
            label="Show Game Logs"
            description="Open game output log window when launching"
          >
            <button
              onClick={() => setShowGameLogs(!showGameLogs)}
              className={clsx(
                'relative w-12 h-6 rounded-full transition-colors',
                showGameLogs ? 'bg-miracle-500' : 'bg-surface-secondary'
              )}
            >
              <div
                className={clsx(
                  'absolute top-1 w-4 h-4 rounded-full transition-transform',
                  showGameLogs ? 'bg-white translate-x-7' : 'bg-theme-muted translate-x-1'
                )}
              />
            </button>
          </SettingsItem>
        </SettingsSection>

        {/* About */}
        <SettingsSection title="About" icon={Monitor}>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-theme-muted">Version</span>
              <span>1.0.0</span>
            </div>
            <div className="flex justify-between">
              <span className="text-theme-muted">Minecraft</span>
              <span>1.21.4</span>
            </div>
            <div className="flex justify-between">
              <span className="text-theme-muted">Fabric Loader</span>
              <span>0.16.9</span>
            </div>
          </div>
        </SettingsSection>
      </div>
    </div>
  );
}

function SettingsSection({
  title,
  icon: Icon,
  children,
}: {
  title: string;
  icon: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <div className="glass rounded-xl overflow-hidden">
      <div className="px-6 py-4 border-b border-theme-muted/20 flex items-center gap-3">
        <Icon className="w-5 h-5 text-miracle-500" />
        <h2 className="font-semibold text-theme-primary">{title}</h2>
      </div>
      <div className="p-6 space-y-6">{children}</div>
    </div>
  );
}

function SettingsItem({
  label,
  description,
  children,
}: {
  label: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <div className="font-medium text-theme-primary">{label}</div>
        <div className="text-sm text-theme-muted">{description}</div>
      </div>
      {children}
    </div>
  );
}
