import { useEffect, useState, useRef } from 'react';
import { convertFileSrc } from '@tauri-apps/api/core';
import { useClipsStore, ClipInfo } from '@/lib/stores/clipsStore';

const CYCLE_INTERVAL_MS = 30000; // Change clip every 30 seconds

export default function CinematicBackground() {
  const { clips, fetchClips } = useClipsStore();
  const [currentClip, setCurrentClip] = useState<ClipInfo | null>(null);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  // Fetch clips on mount
  useEffect(() => {
    fetchClips();
  }, [fetchClips]);

  // Set initial clip and cycle through clips
  useEffect(() => {
    if (clips.length === 0) {
      setCurrentClip(null);
      return;
    }

    // Pick a random clip to start
    const randomIndex = Math.floor(Math.random() * clips.length);
    setCurrentClip(clips[randomIndex]);

    // Only cycle if we have multiple clips
    if (clips.length <= 1) return;

    const interval = setInterval(() => {
      setIsTransitioning(true);
      setTimeout(() => {
        const newIndex = Math.floor(Math.random() * clips.length);
        setCurrentClip(clips[newIndex]);
        setIsTransitioning(false);
      }, 500);
    }, CYCLE_INTERVAL_MS);

    return () => clearInterval(interval);
  }, [clips]);

  // Handle video loaded
  useEffect(() => {
    if (videoRef.current && currentClip) {
      videoRef.current.play().catch(() => {
        // Autoplay might be blocked, that's fine
      });
    }
  }, [currentClip]);

  return (
    <div className="fixed inset-0 overflow-hidden -z-10">
      {/* Video background when clips exist */}
      {currentClip ? (
        <>
          <video
            ref={videoRef}
            key={currentClip.id}
            src={convertFileSrc(currentClip.path)}
            className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-500 ${
              isTransitioning ? 'opacity-0' : 'opacity-100'
            }`}
            autoPlay
            muted
            loop
            playsInline
          />
          {/* Overlay for readability - uses theme colors */}
          <div
            className="absolute inset-0"
            style={{
              background: `linear-gradient(to top, var(--bg-from) 0%, color-mix(in srgb, var(--bg-from) 80%, transparent) 50%, color-mix(in srgb, var(--bg-from) 60%, transparent) 100%)`
            }}
          />
          <div
            className="absolute inset-0"
            style={{ background: 'color-mix(in srgb, var(--bg-from) 40%, transparent)' }}
          />
        </>
      ) : (
        /* Fallback: Theme gradient background when no clips */
        <>
          <div
            className="absolute inset-0"
            style={{ background: 'linear-gradient(135deg, var(--bg-from) 0%, var(--bg-to) 100%)' }}
          />
          {/* Subtle animated gradient orbs using accent color */}
          <div className="absolute inset-0 overflow-hidden">
            <div
              className="absolute -top-1/2 -left-1/2 w-full h-full rounded-full opacity-20"
              style={{
                background: 'radial-gradient(circle, color-mix(in srgb, var(--accent-500) 30%, transparent) 0%, transparent 70%)',
                animation: 'float 20s ease-in-out infinite',
              }}
            />
            <div
              className="absolute -bottom-1/2 -right-1/2 w-full h-full rounded-full opacity-15"
              style={{
                background: 'radial-gradient(circle, color-mix(in srgb, var(--accent-700) 30%, transparent) 0%, transparent 70%)',
                animation: 'float 25s ease-in-out infinite reverse',
              }}
            />
          </div>
          {/* Subtle grid pattern */}
          <div
            className="absolute inset-0 opacity-[0.02]"
            style={{
              backgroundImage: `
                linear-gradient(color-mix(in srgb, var(--text-primary) 10%, transparent) 1px, transparent 1px),
                linear-gradient(90deg, color-mix(in srgb, var(--text-primary) 10%, transparent) 1px, transparent 1px)
              `,
              backgroundSize: '50px 50px',
            }}
          />
        </>
      )}

      {/* Bottom fade for content area */}
      <div
        className="absolute bottom-0 left-0 right-0 h-1/3"
        style={{ background: 'linear-gradient(to top, var(--bg-from), transparent)' }}
      />

      <style>{`
        @keyframes float {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(30px, -30px) scale(1.05); }
          66% { transform: translate(-20px, 20px) scale(0.95); }
        }
      `}</style>
    </div>
  );
}
