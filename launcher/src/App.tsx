import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/lib/stores/authStore';
import { useGameStore } from '@/lib/stores/gameStore';
import { useThemeStore } from '@/lib/stores/themeStore';
import Layout from '@/components/Layout';
import Titlebar from '@/components/Titlebar';
import LoginPage from '@/pages/Login';
import HomePage from '@/pages/Home';
import ContentBrowserPage from '@/pages/ContentBrowser';
import ProfilesPage from '@/pages/Profiles';
import FriendsPage from '@/pages/Friends';
import SettingsPage from '@/pages/Settings';
import OwnerControlsPage from '@/pages/OwnerControls';
import NewsPage from '@/pages/News';
import ClipsPage from '@/pages/Clips';
import { ToastProvider } from '@/components/ToastContainer';
import { ModalProvider } from '@/components/ui';
import { restoreWindowState, setupWindowStateListeners } from '@/lib/windowState';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (isLoading) {
    return (
      <div className="h-screen w-screen flex items-center justify-center bg-surface-primary">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-miracle-500 border-t-transparent rounded-full animate-spin" />
          <span className="text-theme-muted">Loading...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function App() {
  const { checkMiracleUpdate, lastUpdateCheck } = useGameStore();
  // Initialize theme store on app start (triggers rehydration and applies saved theme)
  useThemeStore.getState().getTheme();

  useEffect(() => {
    // Restore window state on app start
    restoreWindowState();
    // Setup listeners to save state on changes
    setupWindowStateListeners();

    // Check for Miracle Client mod updates on startup
    // Only check if we haven't checked in the last hour
    const ONE_HOUR = 60 * 60 * 1000;
    const shouldCheck = !lastUpdateCheck || Date.now() - lastUpdateCheck > ONE_HOUR;

    if (shouldCheck) {
      checkMiracleUpdate().then((update) => {
        if (update) {
          console.log(`Miracle Client update available: ${update.current_version} -> ${update.latest_version}`);
        }
      });
    }
  }, []);

  return (
    <ToastProvider>
      <ModalProvider>
        <div className="h-screen w-screen flex flex-col overflow-hidden">
          <Titlebar />
          <div className="flex-1 overflow-hidden">
            <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout>
                    <HomePage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/browse"
              element={
                <ProtectedRoute>
                  <Layout>
                    <ContentBrowserPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/profiles"
              element={
                <ProtectedRoute>
                  <Layout>
                    <ProfilesPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/friends"
              element={
                <ProtectedRoute>
                  <Layout>
                    <FriendsPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <Layout>
                    <SettingsPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/news"
              element={
                <ProtectedRoute>
                  <Layout>
                    <NewsPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/owner"
              element={
                <ProtectedRoute>
                  <Layout>
                    <OwnerControlsPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/clips"
              element={
                <ProtectedRoute>
                  <Layout>
                    <ClipsPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            </Routes>
          </div>
        </div>
      </ModalProvider>
    </ToastProvider>
  );
}

export default App;
