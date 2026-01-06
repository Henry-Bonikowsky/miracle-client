import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { invoke } from '@tauri-apps/api/core';
import { useAuthStore } from '@/lib/stores/authStore';
import { useGameStore, LaunchState } from '@/lib/stores/gameStore';
import { useProfileStore } from '@/lib/stores/profileStore';
import { Play, Square, ChevronDown } from 'lucide-react';
import clsx from 'clsx';
import ProfileSelector from '@/components/ProfileSelector';
import { useToast } from '@/components/ToastContainer';

const launchStateMessages: Record<LaunchState, string> = {
  idle: 'Play',
  checking: 'Checking files...',
  downloading_java: 'Downloading Java...',
  downloading_minecraft: 'Downloading Minecraft...',
  downloading_fabric: 'Installing Fabric...',
  downloading_mods: 'Installing mods...',
  updating_mod: 'Updating Miracle Client...',
  launching: 'Launching...',
  running: 'Playing',
};

interface MinecraftVersion {
  id: string;
  type: string;
  releaseTime: string;
}

export default function HomePage() {
  const navigate = useNavigate();
  const { profile } = useAuthStore();
  const {
    selectedVersion,
    setSelectedVersion,
    launchState,
    downloadProgress,
    launchGame,
    stopGame,
  } = useGameStore();
  const { showToast } = useToast();
  const { fetchActiveProfile } = useProfileStore();

  const [versions, setVersions] = useState<MinecraftVersion[]>([]);
  const [showVersions, setShowVersions] = useState(false);

  const isLaunching = launchState !== 'idle' && launchState !== 'running';
  const isRunning = launchState === 'running';

  // Fetch versions and initialize profile
  useEffect(() => {
    const initialize = async () => {
      try {
        const vers = await invoke<MinecraftVersion[]>('get_minecraft_versions');
        setVersions(vers);
        if (vers.length > 0 && !selectedVersion) {
          setSelectedVersion(vers[0].id);
        }
        await fetchActiveProfile(selectedVersion);
      } catch (err) {
        console.error('Failed to initialize:', err);
      }
    };
    initialize();
  }, []);

  // Refetch active profile when version changes
  useEffect(() => {
    fetchActiveProfile(selectedVersion);
  }, [selectedVersion, fetchActiveProfile]);

  const handleLaunch = async () => {
    if (!profile) {
      showToast('Please log in first', 'error');
      return;
    }

    if (isRunning) {
      await stopGame();
      return;
    }

    if (isLaunching) return;

    try {
      await launchGame();
    } catch (error) {
      showToast(`Failed to launch: ${error}`, 'error');
    }
  };

  const releaseVersions = versions.filter((v) => v.type === 'release');

  return (
    <div className="h-full flex flex-col items-center justify-center p-6">
      {/* Centered Launch Section */}
      <div className="w-full max-w-md space-y-4">
        {/* Version Selector */}
        <div className="relative">
          <button
            onClick={() => setShowVersions(!showVersions)}
            className="w-full glass rounded-xl px-5 py-4 flex items-center justify-between hover:border-miracle-500/50 transition-all backdrop-blur-xl"
          >
            <div className="text-left">
              <div className="text-xs text-theme-muted uppercase tracking-wider">Minecraft</div>
              <div className="font-semibold text-lg text-theme-primary">{selectedVersion || 'Select version'}</div>
            </div>
            <ChevronDown
              className={clsx('w-5 h-5 text-theme-muted transition-transform', showVersions && 'rotate-180')}
            />
          </button>
          {showVersions && (
            <div className="absolute top-full left-0 right-0 mt-2 z-50 glass rounded-xl shadow-2xl max-h-60 overflow-y-auto backdrop-blur-xl">
              {releaseVersions.map((ver) => (
                <button
                  key={ver.id}
                  onClick={() => {
                    setSelectedVersion(ver.id);
                    setShowVersions(false);
                  }}
                  className={clsx(
                    'w-full px-5 py-3 text-left text-theme-primary hover:bg-miracle-500/10 transition-colors first:rounded-t-xl last:rounded-b-xl',
                    selectedVersion === ver.id && 'text-miracle-500 bg-miracle-500/10'
                  )}
                >
                  {ver.id}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Profile Selector */}
        <div className="glass rounded-xl backdrop-blur-xl">
          <ProfileSelector
            selectedVersion={selectedVersion}
            onManageProfiles={() => navigate('/profiles')}
          />
        </div>

        {/* Launch Button */}
        <button
          onClick={handleLaunch}
          disabled={isLaunching}
          className={clsx(
            'w-full py-5 rounded-xl font-bold text-xl flex items-center justify-center gap-3 transition-all',
            isRunning
              ? 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/30'
              : isLaunching
              ? 'bg-surface-secondary/80 text-theme-muted cursor-wait backdrop-blur-xl border border-miracle-500/10'
              : 'bg-gradient-to-r from-miracle-500 to-miracle-600 hover:from-miracle-400 hover:to-miracle-500 text-white shadow-xl shadow-miracle-500/40 hover:shadow-miracle-500/50 hover:scale-[1.02] active:scale-[0.98]'
          )}
        >
          {isRunning ? (
            <>
              <Square className="w-6 h-6" />
              Stop Game
            </>
          ) : isLaunching ? (
            <>
              <div className="w-6 h-6 border-2 border-theme-muted border-t-transparent rounded-full animate-spin" />
              {launchStateMessages[launchState]}
            </>
          ) : (
            <>
              <Play className="w-6 h-6" />
              {launchStateMessages[launchState]}
            </>
          )}
        </button>

        {/* Download Progress */}
        {isLaunching && downloadProgress && (
          <div className="glass rounded-xl p-4 backdrop-blur-xl">
            <div className="flex items-center justify-between text-sm mb-2">
              <span className="text-theme-secondary truncate max-w-[70%]">{downloadProgress.file}</span>
              <span className="text-miracle-500 font-medium">
                {Math.round((downloadProgress.current / downloadProgress.total) * 100)}%
              </span>
            </div>
            <div className="h-1.5 bg-surface-secondary rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-miracle-500 to-miracle-400 transition-all rounded-full"
                style={{ width: `${(downloadProgress.current / downloadProgress.total) * 100}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Click outside to close version dropdown */}
      {showVersions && (
        <div className="fixed inset-0 z-40" onClick={() => setShowVersions(false)} />
      )}
    </div>
  );
}
