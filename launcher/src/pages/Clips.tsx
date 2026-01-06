import { useEffect } from 'react';
import { Film, FolderOpen, RefreshCw, Trash2 } from 'lucide-react';
import { useClipsStore, formatDuration, formatDate, formatFileSize } from '@/lib/stores/clipsStore';
import ClipModal from '@/components/ClipModal';
import { convertFileSrc } from '@tauri-apps/api/core';

export default function ClipsPage() {
  const {
    clips,
    isLoading,
    error,
    selectedClip,
    isModalOpen,
    fetchClips,
    refreshClips,
    deleteClip,
    openModal,
    closeModal,
    openClipsFolder,
  } = useClipsStore();

  useEffect(() => {
    fetchClips();
  }, [fetchClips]);

  const handleRefresh = async () => {
    await refreshClips();
  };

  const handleOpenFolder = async () => {
    try {
      await openClipsFolder();
    } catch (error) {
      console.error('Failed to open clips folder:', error);
    }
  };

  const handleDelete = async (clipId: string) => {
    try {
      await deleteClip(clipId);
    } catch (error) {
      console.error('Failed to delete clip:', error);
    }
  };

  return (
    <div className="h-full flex flex-col p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-miracle-500/20">
            <Film className="w-6 h-6 text-miracle-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Clips</h1>
            <p className="text-theme-muted text-sm">
              {clips.length === 0 ? 'No clips yet' : `${clips.length} clip${clips.length === 1 ? '' : 's'}`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleRefresh}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors text-theme-secondary hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={handleOpenFolder}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors text-theme-secondary hover:text-white"
          >
            <FolderOpen className="w-4 h-4" />
            Open Folder
          </button>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="mb-4 p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400">
          <p className="font-medium">Failed to load clips</p>
          <p className="text-sm mt-1 opacity-80">{error}</p>
        </div>
      )}

      {/* Clips Grid */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <RefreshCw className="w-12 h-12 animate-spin text-miracle-400 mx-auto mb-4" />
              <p className="text-theme-muted">Loading clips...</p>
            </div>
          </div>
        ) : clips.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-theme-muted">
            <Film className="w-16 h-16 mb-4 opacity-30" />
            <p className="text-xl font-medium">No clips yet</p>
            <p className="text-sm mt-1">Record clips in-game to see them here</p>
            <button
              onClick={handleOpenFolder}
              className="mt-4 flex items-center gap-2 px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
            >
              <FolderOpen className="w-4 h-4" />
              Open Clips Folder
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {clips.map((clip) => (
              <div
                key={clip.id}
                className="group relative glass rounded-xl border border-white/10 hover:border-white/20 overflow-hidden transition-all hover:scale-[1.02] active:scale-[0.98]"
              >
                {/* Thumbnail/Video Preview */}
                <div
                  className="relative aspect-video bg-black/40 overflow-hidden cursor-pointer"
                  onClick={() => openModal(clip)}
                >
                  {clip.thumbnailPath ? (
                    <img
                      src={convertFileSrc(clip.thumbnailPath)}
                      alt={clip.filename}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <Film className="w-12 h-12 text-white/20" />
                    </div>
                  )}

                  {/* Play Overlay */}
                  <div className="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors flex items-center justify-center">
                    <div className="w-14 h-14 rounded-full bg-white/0 group-hover:bg-white/90 transition-all flex items-center justify-center transform scale-75 group-hover:scale-100">
                      <div className="w-0 h-0 border-l-[12px] border-l-black/0 group-hover:border-l-black border-y-[8px] border-y-transparent ml-1" />
                    </div>
                  </div>

                  {/* Duration Badge */}
                  <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-black/70 text-white text-xs font-medium">
                    {formatDuration(clip.durationMs)}
                  </div>
                </div>

                {/* Info */}
                <div className="p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium truncate text-sm">{clip.filename}</h3>
                      <div className="flex items-center gap-3 mt-1 text-xs text-theme-muted">
                        <span>{formatDate(clip.createdAt)}</span>
                        <span>•</span>
                        <span>{formatFileSize(clip.sizeBytes)}</span>
                        <span>•</span>
                        <span>
                          {clip.width}x{clip.height}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(clip.id);
                      }}
                      className="flex-shrink-0 p-1.5 rounded-lg bg-white/0 hover:bg-red-500/20 text-theme-muted hover:text-red-400 transition-colors"
                      title="Delete clip"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Clip Modal */}
      {isModalOpen && selectedClip && (
        <ClipModal
          clip={selectedClip}
          onClose={closeModal}
          onDelete={handleDelete}
          onOpenFolder={handleOpenFolder}
        />
      )}
    </div>
  );
}
