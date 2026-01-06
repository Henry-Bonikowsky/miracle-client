import { useState, useEffect, useRef } from 'react';
import { convertFileSrc } from '@tauri-apps/api/core';
import { Play, ChevronLeft, ChevronRight, Film, FolderOpen } from 'lucide-react';
import clsx from 'clsx';
import { ClipInfo, formatDuration, formatDate } from '@/lib/stores/clipsStore';

interface ClipsCarouselProps {
  clips: ClipInfo[];
  onClipClick: (clip: ClipInfo) => void;
  onOpenFolder: () => void;
}

export default function ClipsCarousel({ clips, onClipClick, onOpenFolder }: ClipsCarouselProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const intervalRef = useRef<number | null>(null);

  const AUTO_ADVANCE_MS = 6000;

  // Auto-advance carousel
  useEffect(() => {
    if (clips.length <= 1 || isPaused || isHovered) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    intervalRef.current = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % clips.length);
    }, AUTO_ADVANCE_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [clips.length, isPaused, isHovered]);

  // Reset video when clip changes
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.currentTime = 0;
      videoRef.current.play().catch(() => {});
    }
  }, [currentIndex]);

  const goToPrevious = () => {
    setCurrentIndex((prev) => (prev - 1 + clips.length) % clips.length);
  };

  const goToNext = () => {
    setCurrentIndex((prev) => (prev + 1) % clips.length);
  };

  const currentClip = clips[currentIndex];

  // Empty state
  if (clips.length === 0) {
    return (
      <div className="glass rounded-xl border border-theme-muted/20/50 p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-lg flex items-center gap-2">
            <Film className="w-5 h-5 text-miracle-400" />
            Featured Clips
          </h3>
          <button
            onClick={onOpenFolder}
            className="text-sm text-theme-muted hover:text-miracle-400 transition-colors flex items-center gap-1"
          >
            <FolderOpen className="w-4 h-4" />
            Open Folder
          </button>
        </div>
        <div className="flex flex-col items-center justify-center py-12 text-theme-muted">
          <Film className="w-16 h-16 mb-4 opacity-30" />
          <p className="text-lg mb-2">No clips yet</p>
          <p className="text-sm text-center max-w-md">
            Clips you record in-game will appear here automatically.
            Enable clip capture in the Miracle Client settings while playing.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="glass rounded-xl border border-theme-muted/20/50 overflow-hidden"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-theme-muted/20/50">
        <h3 className="font-bold flex items-center gap-2">
          <Film className="w-5 h-5 text-miracle-400" />
          Featured Clips
        </h3>
        <div className="flex items-center gap-3">
          <span className="text-sm text-theme-muted">
            {currentIndex + 1} / {clips.length}
          </span>
          <button
            onClick={onOpenFolder}
            className="text-sm text-theme-muted hover:text-miracle-400 transition-colors flex items-center gap-1"
          >
            <FolderOpen className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Video Container */}
      <div className="relative aspect-video bg-surface-primary">
        {/* Video */}
        <video
          ref={videoRef}
          src={convertFileSrc(currentClip.path)}
          className="w-full h-full object-contain"
          autoPlay
          muted
          loop
          playsInline
          onClick={() => onClipClick(currentClip)}
        />

        {/* Gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent pointer-events-none" />

        {/* Play button overlay */}
        <button
          onClick={() => onClipClick(currentClip)}
          className="absolute inset-0 flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity"
        >
          <div className="w-16 h-16 rounded-full bg-miracle-500/90 flex items-center justify-center shadow-lg shadow-miracle-500/30">
            <Play className="w-8 h-8 text-white ml-1" />
          </div>
        </button>

        {/* Navigation arrows */}
        {clips.length > 1 && (
          <>
            <button
              onClick={(e) => {
                e.stopPropagation();
                goToPrevious();
              }}
              className="absolute left-2 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/50 hover:bg-black/70 flex items-center justify-center transition-colors"
            >
              <ChevronLeft className="w-6 h-6" />
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                goToNext();
              }}
              className="absolute right-2 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/50 hover:bg-black/70 flex items-center justify-center transition-colors"
            >
              <ChevronRight className="w-6 h-6" />
            </button>
          </>
        )}

        {/* Clip info overlay */}
        <div className="absolute bottom-0 left-0 right-0 p-4 pointer-events-none">
          <div className="flex items-end justify-between">
            <div>
              <h4 className="font-medium text-white text-lg">{currentClip.filename}</h4>
              <p className="text-sm text-theme-secondary">{formatDate(currentClip.createdAt)}</p>
            </div>
            {currentClip.durationMs > 0 && (
              <div className="text-sm text-theme-secondary bg-black/50 px-2 py-1 rounded">
                {formatDuration(currentClip.durationMs)}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Navigation dots */}
      {clips.length > 1 && (
        <div className="flex justify-center gap-2 p-3 border-t border-theme-muted/20/50">
          {clips.map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentIndex(index)}
              className={clsx(
                'w-2 h-2 rounded-full transition-all',
                index === currentIndex
                  ? 'bg-miracle-500 w-6'
                  : 'bg-surface-secondary hover:bg-miracle-500/50'
              )}
            />
          ))}
        </div>
      )}
    </div>
  );
}
