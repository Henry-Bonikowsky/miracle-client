import { useEffect, useState, useMemo } from 'react';
import { X, Download, ExternalLink, AlertCircle } from 'lucide-react';
import clsx from 'clsx';
import { ModCatalogEntry } from '@/lib/data/modCatalog';
import { useGameStore } from '@/lib/stores/gameStore';
import { useProfileStore } from '@/lib/stores/profileStore';
import { marked } from 'marked';
import { open } from '@tauri-apps/plugin-shell';

interface ModDetailsModalProps {
  mod: ModCatalogEntry | null;
  isInstalled?: boolean;
  onClose: () => void;
  onInstalled?: () => void;
}

interface ModrinthDetails {
  id: string;
  title: string;
  description: string;
  body: string;
  categories: string[];
  downloads: number;
  followers: number;
  versions: string[];
  license?: {
    id: string;
    name: string;
  };
  gallery?: Array<{
    url: string;
    featured: boolean;
    title?: string;
  }>;
  icon_url?: string;
}

interface CurseForgeDetails {
  id: number;
  name: string;
  summary: string;
  description?: string;
  categories: Array<{ name: string }>;
  downloadCount: number;
  logo?: { url: string };
  screenshots?: Array<{ url: string; title?: string }>;
}

export default function ModDetailsModal({ mod, isInstalled, onClose, onInstalled }: ModDetailsModalProps) {
  const { downloadMod, selectedVersion } = useGameStore();
  const { activeProfileId } = useProfileStore();
  const [details, setDetails] = useState<ModrinthDetails | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  useEffect(() => {
    if (!mod) return;

    // Reset state when mod changes
    setDetails(null);
    setError(null);
    setDownloadError(null);

    const fetchModDetails = async () => {
      setLoading(true);

      try {
        // Only fetch from Modrinth if the mod has a Modrinth URL
        if (mod.modrinthUrl) {
          const projectId = mod.slug || mod.modrinthUrl.split('/').pop() || mod.id;
          const response = await fetch(`https://api.modrinth.com/v2/project/${projectId}`);

          if (response.ok) {
            const data = await response.json();
            setDetails(data);
          } else {
            setError('Could not fetch mod details from Modrinth');
          }
        } else if (mod.curseforgeId) {
          // Fetch from CurseForge API
          const response = await fetch(`https://api.curseforge.com/v1/mods/${mod.curseforgeId}`, {
            headers: {
              'x-api-key': '$2a$10$JerFj3jTqK5z2SJlzO4i.e0/7O3wSdh27GyM4vHIRinf7VJvuJnfe'
            }
          });

          if (response.ok) {
            const cfData: { data: CurseForgeDetails } = await response.json();
            console.log('CurseForge API response:', cfData);

            // Convert CurseForge format to Modrinth format for compatibility
            setDetails({
              id: cfData.data.id.toString(),
              title: cfData.data.name,
              description: cfData.data.summary,
              body: cfData.data.description || cfData.data.summary,
              categories: cfData.data.categories?.map(c => c.name) || [],
              downloads: cfData.data.downloadCount || 0,
              followers: 0,
              versions: [],
              icon_url: cfData.data.logo?.url,
              gallery: cfData.data.screenshots?.map(s => ({
                url: s.url,
                featured: false,
                title: s.title
              })) || []
            });
          } else {
            const errorText = await response.text();
            console.error('CurseForge API error:', response.status, errorText);
            setError(`Could not fetch mod details from CurseForge: ${response.status}`);
          }
        } else {
          setError('No download source available');
        }
      } catch (err) {
        setError('Failed to load mod details');
        console.error('Error fetching mod details:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchModDetails();
  }, [mod]);

  const handleInstall = async () => {
    if (!mod || isInstalled) return;

    setDownloading(true);
    setDownloadError(null);

    try {
      const result = await downloadMod(mod.slug, selectedVersion, mod.curseforgeId, activeProfileId || undefined);
      console.log('Mod installed:', result);
      if (onInstalled) {
        onInstalled();
      }
      // Close modal after successful install
      setTimeout(() => {
        onClose();
      }, 1500);
    } catch (err: any) {
      console.error('Failed to install mod:', err);
      setDownloadError(err?.toString() || 'Failed to install mod');
    } finally {
      setDownloading(false);
    }
  };

  // Parse and sanitize markdown body
  const parsedBody = useMemo(() => {
    if (!details?.body) return null;

    // Remove images to avoid the hijacking issue
    const bodyWithoutImages = details.body.replace(/!\[.*?\]\(.*?\)/g, '');

    // Parse markdown to HTML
    const html = marked.parse(bodyWithoutImages, { async: false }) as string;

    // Limit to 2000 characters
    return html.slice(0, 2000);
  }, [details?.body]);

  // Aggressively prevent all navigation and handle links externally
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;

      // Check if clicked element or any parent is a link
      let element: HTMLElement | null = target;
      while (element) {
        if (element.tagName === 'A') {
          e.preventDefault();
          e.stopPropagation();
          const href = element.getAttribute('href');
          if (href && (href.startsWith('http') || href.startsWith('https'))) {
            open(href);
          }
          return;
        }
        element = element.parentElement;
      }
    };

    // Add global click listener
    document.addEventListener('click', handleClick, true);

    return () => {
      document.removeEventListener('click', handleClick, true);
    };
  }, []);

  if (!mod) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[70vw] h-[70vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 border-b border-theme-muted/20/50 flex items-start justify-between flex-shrink-0">
          <div className="flex items-start gap-4 flex-1">
            {details?.icon_url && (
              <img
                src={details.icon_url}
                alt={mod.name}
                className="w-16 h-16 rounded-lg object-cover"
              />
            )}
            <div className="flex-1 min-w-0">
              <h2 className="text-2xl font-bold gradient-text truncate">{details?.title || mod.name}</h2>
              <p className="text-theme-muted text-sm mt-1">by {mod.author}</p>
              <div className="flex items-center gap-3 mt-2">
                <span className="text-xs px-2 py-1 rounded bg-surface-secondary/50 text-theme-secondary capitalize">
                  {mod.category}
                </span>
                {mod.recommended && (
                  <span className="text-xs px-2 py-1 rounded bg-miracle-500/20 text-miracle-400">
                    Recommended
                  </span>
                )}
                {isInstalled && (
                  <span className="text-xs px-2 py-1 rounded bg-green-500/20 text-green-400">
                    Installed
                  </span>
                )}
              </div>
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
        <div className="flex-1 overflow-y-auto p-6 select-text">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className="w-8 h-8 border-2 border-miracle-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : error ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center text-theme-muted">
                <AlertCircle className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>{error}</p>
                <p className="text-sm mt-2">{mod.description}</p>
              </div>
            </div>
          ) : details ? (
            <div className="space-y-6">
              {/* Stats */}
              <div className="grid grid-cols-3 gap-4">
                <div className="glass rounded-lg p-4 border border-theme-muted/20/30">
                  <div className="text-2xl font-bold gradient-text">
                    {details.downloads.toLocaleString()}
                  </div>
                  <div className="text-sm text-theme-muted mt-1">Downloads</div>
                </div>
                <div className="glass rounded-lg p-4 border border-theme-muted/20/30">
                  <div className="text-2xl font-bold gradient-text">
                    {details.followers.toLocaleString()}
                  </div>
                  <div className="text-sm text-theme-muted mt-1">Followers</div>
                </div>
                <div className="glass rounded-lg p-4 border border-theme-muted/20/30">
                  <div className="text-2xl font-bold gradient-text">
                    {details.versions.length}
                  </div>
                  <div className="text-sm text-theme-muted mt-1">Versions</div>
                </div>
              </div>

              {/* Description */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Description</h3>
                <p className="text-theme-secondary leading-relaxed">{details.description}</p>
              </div>

              {/* Gallery */}
              {details.gallery && details.gallery.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold mb-3">Screenshots</h3>
                  <div className="grid grid-cols-2 gap-4">
                    {details.gallery.slice(0, 4).map((image, idx) => (
                      <div
                        key={idx}
                        onClick={() => open(image.url)}
                        className="cursor-pointer group"
                      >
                        <img
                          src={image.url}
                          alt={image.title || `Screenshot ${idx + 1}`}
                          className="rounded-lg w-full h-48 object-cover border border-theme-muted/20/30 group-hover:border-miracle-500/50 transition-colors"
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Categories */}
              {details.categories && details.categories.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold mb-3">Categories</h3>
                  <div className="flex flex-wrap gap-2">
                    {details.categories.map((cat) => (
                      <span
                        key={cat}
                        className="text-xs px-3 py-1.5 rounded-lg bg-surface-secondary/50 text-theme-secondary capitalize"
                      >
                        {cat.replace('-', ' ')}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Dependencies */}
              {mod.dependencies && mod.dependencies.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold mb-3">Dependencies</h3>
                  <div className="flex flex-wrap gap-2">
                    {mod.dependencies.map((dep) => (
                      <span
                        key={dep}
                        className="text-xs px-3 py-1.5 rounded-lg bg-miracle-500/20 text-miracle-300"
                      >
                        {dep}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* License */}
              {details.license && (
                <div>
                  <h3 className="text-lg font-semibold mb-3">License</h3>
                  <p className="text-theme-secondary">{details.license.name}</p>
                </div>
              )}

              {/* Full description / body */}
              {parsedBody && (
                <div>
                  <h3 className="text-lg font-semibold mb-3">About</h3>
                  <div
                    className="text-theme-secondary leading-relaxed prose prose-invert prose-sm max-w-none
                               prose-headings:text-white prose-a:text-miracle-400 prose-a:no-underline hover:prose-a:underline
                               prose-strong:text-white prose-code:text-miracle-300 prose-code:bg-surface-secondary prose-code:px-1 prose-code:rounded"
                    dangerouslySetInnerHTML={{ __html: parsedBody }}
                  />
                </div>
              )}
            </div>
          ) : null}
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-theme-muted/20/50 flex flex-col gap-3 flex-shrink-0">
          {downloadError && (
            <div className="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-500/20 border border-red-500/30 text-red-400 text-sm">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {downloadError}
            </div>
          )}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              {mod.modrinthUrl && (
                <button
                  onClick={() => open(mod.modrinthUrl!)}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-secondary/50 hover:bg-surface-secondary text-theme-secondary hover:text-theme-primary transition-all text-sm"
                >
                  <ExternalLink className="w-4 h-4" />
                  View on Modrinth
                </button>
              )}
              {mod.curseforgeUrl && (
                <button
                  onClick={() => open(mod.curseforgeUrl!)}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-secondary/50 hover:bg-surface-secondary text-theme-secondary hover:text-theme-primary transition-all text-sm"
                >
                  <ExternalLink className="w-4 h-4" />
                  View on CurseForge
                </button>
              )}
            </div>
            <button
              onClick={handleInstall}
              disabled={isInstalled || downloading}
              className={clsx(
                'flex items-center gap-2 px-6 py-2.5 rounded-lg font-medium transition-all',
                isInstalled || downloading
                  ? 'bg-miracle-500/50 text-white cursor-not-allowed opacity-50'
                  : !mod.modrinthUrl && mod.curseforgeUrl
                  ? 'bg-orange-500 hover:bg-orange-600 text-white'
                  : 'bg-gradient-to-r from-miracle-500 to-miracle-600 hover:from-miracle-400 hover:to-miracle-500 text-white'
              )}
            >
              {downloading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Installing...
                </>
              ) : isInstalled ? (
                <>
                  <Download className="w-4 h-4" />
                  Already Installed
                </>
              ) : !mod.modrinthUrl && mod.curseforgeUrl ? (
                <>
                  <Download className="w-4 h-4" />
                  Download from CurseForge
                </>
              ) : (
                <>
                  <Download className="w-4 h-4" />
                  Install
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
