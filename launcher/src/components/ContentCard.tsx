import { Download, Loader2, Check, Package } from 'lucide-react';
import { motion } from 'framer-motion';
import clsx from 'clsx';
import { ContentItem } from '@/lib/stores/contentStore';
import { useState } from 'react';

interface ContentCardProps {
  item: ContentItem;
  onInstall: (item: ContentItem) => Promise<void>;
  isInstalled?: boolean;
}

function formatDownloads(downloads: number): string {
  if (downloads >= 1_000_000) {
    return `${(downloads / 1_000_000).toFixed(1)}M`;
  }
  if (downloads >= 1_000) {
    return `${(downloads / 1_000).toFixed(1)}K`;
  }
  return downloads.toString();
}

export default function ContentCard({ item, onInstall, isInstalled }: ContentCardProps) {
  const [isInstalling, setIsInstalling] = useState(false);
  const [installed, setInstalled] = useState(isInstalled ?? false);
  const [error, setError] = useState<string | null>(null);

  const handleInstall = async () => {
    if (installed || isInstalling) return;

    setIsInstalling(true);
    setError(null);

    try {
      await onInstall(item);
      setInstalled(true);
    } catch (err) {
      setError(String(err));
    } finally {
      setIsInstalling(false);
    }
  };

  return (
    <motion.div
      whileHover={{ y: -3, scale: 1.01 }}
      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
      className="glass rounded-xl border border-theme-muted/20/50 overflow-hidden hover:border-miracle-500/30 hover:shadow-lg hover:shadow-miracle-500/5 transition-all group"
    >
      <div className="p-4">
        <div className="flex gap-4">
          {/* Icon */}
          <div className="w-16 h-16 rounded-lg bg-surface-secondary overflow-hidden flex-shrink-0">
            {item.iconUrl ? (
              <img
                src={item.iconUrl}
                alt={item.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <Package className="w-8 h-8 text-theme-muted" />
              </div>
            )}
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <h3 className="font-semibold text-theme-primary truncate">{item.name}</h3>
                <p className="text-sm text-theme-muted">by {item.author}</p>
              </div>
              {/* Source Badge */}
              <span
                className={clsx(
                  'text-xs px-2 py-0.5 rounded flex-shrink-0',
                  item.source === 'modrinth'
                    ? 'bg-green-500/20 text-green-400'
                    : 'bg-orange-500/20 text-orange-400'
                )}
              >
                {item.source === 'modrinth' ? 'Modrinth' : 'CurseForge'}
              </span>
            </div>

            {/* Description */}
            <p className="text-sm text-theme-secondary mt-2 line-clamp-2">{item.description}</p>

            {/* Categories */}
            {item.categories.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-2">
                {item.categories.slice(0, 3).map((cat) => (
                  <span
                    key={cat}
                    className="text-xs px-2 py-0.5 rounded bg-surface-secondary/50 text-theme-secondary"
                  >
                    {cat}
                  </span>
                ))}
                {item.categories.length > 3 && (
                  <span className="text-xs px-2 py-0.5 text-theme-muted">
                    +{item.categories.length - 3}
                  </span>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between mt-4 pt-4 border-t border-theme-muted/20/50">
          <div className="flex items-center gap-4 text-sm text-theme-muted">
            <div className="flex items-center gap-1">
              <Download className="w-4 h-4" />
              <span>{formatDownloads(item.downloads)}</span>
            </div>
          </div>

          {error ? (
            <p className="text-xs text-red-400" style={{ userSelect: 'text', WebkitUserSelect: 'text' }}>{error}</p>
          ) : (
            <button
              onClick={handleInstall}
              disabled={installed || isInstalling}
              className={clsx(
                'px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2',
                installed
                  ? 'bg-green-500/20 text-green-400 cursor-default'
                  : isInstalling
                  ? 'bg-surface-secondary text-theme-muted cursor-wait'
                  : 'bg-miracle-500 hover:bg-miracle-600 text-theme-primary'
              )}
            >
              {installed ? (
                <>
                  <Check className="w-4 h-4" />
                  Installed
                </>
              ) : isInstalling ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Installing...
                </>
              ) : (
                <>
                  <Download className="w-4 h-4" />
                  Install
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </motion.div>
  );
}
