import { useState, useEffect } from 'react';
import { Newspaper, Calendar, Settings } from 'lucide-react';
import { useNewsStore, NewsItem } from '@/lib/stores/newsStore';
import { useAuthStore } from '@/lib/stores/authStore';
import { getAccountRank } from '@/lib/constants/ranks';
import NewsManagementModal from '@/components/NewsManagementModal';

interface NewsArticleModalProps {
  article: NewsItem;
  onClose: () => void;
}

function NewsArticleModal({ article, onClose }: NewsArticleModalProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="glass rounded-2xl border border-white/10 shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 border-b border-white/10">
          <h2 className="text-2xl font-bold">{article.title}</h2>
          <div className="flex items-center gap-2 mt-2 text-theme-muted">
            <Calendar className="w-4 h-4" />
            <span className="text-sm">{article.date}</span>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <p className="text-theme-primary leading-relaxed whitespace-pre-wrap">
            {article.content}
          </p>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-white/10">
          <button
            onClick={onClose}
            className="w-full py-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors font-medium"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

export default function NewsPage() {
  const { newsItems, markAllAsRead } = useNewsStore();
  const { profile } = useAuthStore();
  const isOwner = profile?.id ? getAccountRank(profile.id) === 'owner' : false;

  const [selectedArticle, setSelectedArticle] = useState<NewsItem | null>(null);
  const [showManagement, setShowManagement] = useState(false);

  // Mark as read when viewing the page
  useEffect(() => {
    markAllAsRead();
  }, [markAllAsRead]);

  return (
    <div className="h-full flex flex-col p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-miracle-500/20">
            <Newspaper className="w-6 h-6 text-miracle-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">News & Updates</h1>
            <p className="text-theme-muted text-sm">Stay up to date with Miracle Client</p>
          </div>
        </div>

        {isOwner && (
          <button
            onClick={() => setShowManagement(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors text-theme-secondary hover:text-white"
          >
            <Settings className="w-4 h-4" />
            Manage
          </button>
        )}
      </div>

      {/* News Grid */}
      <div className="flex-1 overflow-y-auto">
        {newsItems.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-theme-muted">
            <Newspaper className="w-16 h-16 mb-4 opacity-30" />
            <p className="text-xl font-medium">No news yet</p>
            <p className="text-sm mt-1">Check back later for updates!</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {newsItems.map((item, index) => (
              <button
                key={item.id}
                onClick={() => setSelectedArticle(item)}
                className={`w-full text-left p-5 rounded-xl border transition-all hover:scale-[1.01] active:scale-[0.99] ${
                  index === 0
                    ? 'bg-miracle-500/10 border-miracle-500/30 hover:bg-miracle-500/15'
                    : 'glass border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold text-lg truncate">{item.title}</h3>
                      {index === 0 && (
                        <span className="flex-shrink-0 text-xs px-2 py-0.5 rounded-full bg-miracle-500/20 text-miracle-400 font-medium">
                          Latest
                        </span>
                      )}
                    </div>
                    <p className="text-theme-muted text-sm line-clamp-2">{item.content}</p>
                  </div>
                  <div className="flex-shrink-0 text-right">
                    <span className="text-xs text-theme-muted">{item.date}</span>
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Article Modal */}
      {selectedArticle && (
        <NewsArticleModal
          article={selectedArticle}
          onClose={() => setSelectedArticle(null)}
        />
      )}

      {/* Management Modal (for owners) */}
      {showManagement && (
        <NewsManagementModal onClose={() => setShowManagement(false)} />
      )}
    </div>
  );
}
