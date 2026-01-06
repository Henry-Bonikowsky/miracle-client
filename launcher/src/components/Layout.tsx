import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Home, Settings, Users, LogOut, Crown, Compass, FolderOpen, Newspaper, Film } from 'lucide-react';
import { useAuthStore } from '@/lib/stores/authStore';
import { useNewsStore } from '@/lib/stores/newsStore';
import PlayerHead from '@/components/PlayerHead';
import CinematicBackground from '@/components/CinematicBackground';
import { getAccountRank } from '@/lib/constants/ranks';
import { useToast } from '@/components/ToastContainer';
import clsx from 'clsx';

interface LayoutProps {
  children: React.ReactNode;
}

const navItems = [
  { path: '/', icon: Home, label: 'Home' },
  { path: '/profiles', icon: FolderOpen, label: 'Profiles' },
  { path: '/browse', icon: Compass, label: 'Browse' },
  { path: '/clips', icon: Film, label: 'Clips' },
  { path: '/news', icon: Newspaper, label: 'News' },
  { path: '/friends', icon: Users, label: 'Friends' },
  { path: '/settings', icon: Settings, label: 'Settings' },
];

export default function Layout({ children }: LayoutProps) {
  const { profile, logout } = useAuthStore();
  const { hasUnreadNews, newsItems } = useNewsStore();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [hasShownStartupToast, setHasShownStartupToast] = useState(false);

  const isOwner = profile?.id ? getAccountRank(profile.id) === 'owner' : false;

  // Show news toast on startup if there's unread news
  useEffect(() => {
    if (hasUnreadNews && newsItems.length > 0 && !hasShownStartupToast) {
      setHasShownStartupToast(true);
      const latestNews = newsItems[0];
      showToast(latestNews.title, 'info');
    }
  }, [hasUnreadNews, newsItems, hasShownStartupToast, showToast]);

  return (
    <div className="h-full flex relative">
      {/* Cinematic Background */}
      <CinematicBackground />

      {/* Sidebar */}
      <aside className="w-16 bg-surface-primary/70 backdrop-blur-xl border-r border-white/5 flex flex-col items-center py-4 relative z-10">
        <nav className="flex-1 flex flex-col items-center gap-2">
          {navItems.map(({ path, icon: Icon, label }) => {
            const isActive = location.pathname === path;
            const isNews = path === '/news';

            return (
              <button
                key={path}
                onClick={() => navigate(path)}
                className={clsx(
                  'w-10 h-10 rounded-lg flex items-center justify-center transition-all duration-200 group relative cursor-pointer',
                  isActive
                    ? 'bg-miracle-600 text-white shadow-lg shadow-miracle-600/30'
                    : 'text-theme-muted hover:text-theme-primary hover:bg-miracle-500/10'
                )}
              >
                <Icon className="w-5 h-5" />
                {/* Unread indicator for News */}
                {isNews && hasUnreadNews && !isActive && (
                  <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-miracle-500 rounded-full border-2 border-surface-primary" />
                )}
                <div className="absolute left-full ml-2 px-2 py-1 bg-surface-secondary rounded text-xs font-medium opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">
                  {label}
                </div>
              </button>
            );
          })}
        </nav>

        {/* Owner Controls Button - Only visible to owner */}
        {isOwner && (
          <button
            onClick={() => navigate('/owner')}
            className={clsx(
              'w-10 h-10 rounded-lg flex items-center justify-center transition-all duration-200 group relative',
              location.pathname === '/owner'
                ? 'bg-yellow-600 text-white shadow-lg shadow-yellow-600/30'
                : 'text-yellow-400 hover:text-white hover:bg-yellow-600/20'
            )}
          >
            <Crown className="w-5 h-5" />
            <div className="absolute left-full ml-2 px-2 py-1 bg-surface-secondary rounded text-xs font-medium opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">
              Owner Controls
            </div>
          </button>
        )}

        {/* User section */}
        <div className="flex flex-col items-center gap-2 pt-4 border-t border-white/5">
          {profile?.id ? (
            <PlayerHead
              uuid={profile.id}
              name={profile.name}
              size={32}
              className="w-8 h-8 rounded"
            />
          ) : (
            <div className="w-8 h-8 rounded bg-gradient-to-br from-miracle-400 to-miracle-600 flex items-center justify-center text-xs font-bold">
              ?
            </div>
          )}
          <button
            onClick={logout}
            className="w-10 h-10 rounded-lg flex items-center justify-center text-theme-muted hover:text-red-400 hover:bg-red-500/10 transition-colors"
          >
            <LogOut className="w-5 h-5" />
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto relative z-10">{children}</main>
    </div>
  );
}
