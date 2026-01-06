import { getCurrentWindow } from '@tauri-apps/api/window';
import { PhysicalPosition, PhysicalSize } from '@tauri-apps/api/window';
import { listen } from '@tauri-apps/api/event';
import { useGameStore } from './stores/gameStore';
import { useThemeStore } from './stores/themeStore';
import { useAuthStore } from './stores/authStore';

interface WindowState {
  x: number;
  y: number;
  width: number;
  height: number;
  maximized: boolean;
}

const STORAGE_KEY = 'window-state';

export async function saveWindowState() {
  try {
    const window = getCurrentWindow();
    const position = await window.outerPosition();
    const size = await window.outerSize();
    const maximized = await window.isMaximized();

    const state: WindowState = {
      x: position.x,
      y: position.y,
      width: size.width,
      height: size.height,
      maximized,
    };

    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    console.log('Window state saved:', state);
  } catch (error) {
    console.error('Failed to save window state:', error);
  }
}

export async function restoreWindowState() {
  try {
    const savedState = localStorage.getItem(STORAGE_KEY);
    if (!savedState) {
      console.log('No saved window state found');
      return;
    }

    const state: WindowState = JSON.parse(savedState);
    console.log('Restoring window state:', state);

    // Validate dimensions - don't restore if they're too small (corrupted)
    if (state.width < 900 || state.height < 600) {
      console.log('Saved window state has invalid dimensions, clearing...');
      localStorage.removeItem(STORAGE_KEY);
      return;
    }

    const window = getCurrentWindow();

    // Restore position and size
    await window.setPosition(new PhysicalPosition(state.x, state.y));
    await window.setSize(new PhysicalSize(state.width, state.height));

    // Restore maximized state
    if (state.maximized) {
      await window.maximize();
    }
  } catch (error) {
    console.error('Failed to restore window state:', error);
    // Clear corrupted state
    localStorage.removeItem(STORAGE_KEY);
  }
}

export function setupWindowStateListeners() {
  const window = getCurrentWindow();

  // Save state when window is resized or moved
  let saveTimeout: ReturnType<typeof setTimeout>;
  const debouncedSave = () => {
    clearTimeout(saveTimeout);
    saveTimeout = setTimeout(saveWindowState, 500);
  };

  // Listen for window events
  window.listen('tauri://resize', debouncedSave);
  window.listen('tauri://move', debouncedSave);

  // Listen for window focus/show events to reset launch state
  window.listen('tauri://focus', () => {
    const { launchState, setLaunchState } = useGameStore.getState();
    // If the launcher is being shown and the game was "running", reset to idle
    // (This happens when the game closes and the launcher reappears)
    if (launchState === 'running') {
      setLaunchState('idle');
      // Sync theme from mod config in case user changed it in-game
      useThemeStore.getState().syncFromMod();
    }
  });

  // Listen for account switch requests from the mod
  listen<{ account_id: string }>('account_switch_requested', async (event) => {
    console.log('Account switch requested:', event.payload.account_id);
    const { switchAccount } = useAuthStore.getState();
    const { launchGame } = useGameStore.getState();

    try {
      // Switch to the requested account
      await switchAccount(event.payload.account_id);
      // Small delay to ensure state is updated
      await new Promise(resolve => setTimeout(resolve, 500));
      // Relaunch the game with the new account
      await launchGame();
    } catch (error) {
      console.error('Failed to switch account and relaunch:', error);
    }
  });

  // Listen for new accounts added from in-game
  listen<{
    id: string;
    name: string;
    accessToken: string;
    refreshToken: string;
    expiresAt: number;
  }>('account_added', (event) => {
    console.log('Account added from game:', event.payload.name);
    const { accounts } = useAuthStore.getState();

    // Check if account already exists
    const exists = accounts.some(a => a.id === event.payload.id);
    if (!exists) {
      // Add to accounts list
      const newAccount = {
        id: event.payload.id,
        name: event.payload.name,
        accessToken: event.payload.accessToken,
        refreshToken: event.payload.refreshToken,
        expiresAt: event.payload.expiresAt,
      };
      useAuthStore.setState({
        accounts: [...accounts, newAccount],
      });
      console.log('Added new account to store:', newAccount.name);
    }
  });

  // Window state is auto-saved on resize/move, no need to save on close
}
