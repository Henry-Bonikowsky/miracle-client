import { motion } from 'framer-motion';
import clsx from 'clsx';

interface ContentCardSkeletonProps {
  className?: string;
}

export default function ContentCardSkeleton({ className }: ContentCardSkeletonProps) {
  return (
    <div
      className={clsx(
        'glass rounded-xl border border-theme-muted/20/50 overflow-hidden',
        className
      )}
    >
      {/* Image skeleton */}
      <div className="relative h-32 bg-surface-secondary">
        <motion.div
          className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
          animate={{ x: ['-100%', '100%'] }}
          transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
        />
      </div>

      {/* Content skeleton */}
      <div className="p-4 space-y-3">
        {/* Title skeleton */}
        <div className="h-5 bg-surface-secondary rounded-md w-3/4 relative overflow-hidden">
          <motion.div
            className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
            animate={{ x: ['-100%', '100%'] }}
            transition={{ duration: 1.5, repeat: Infinity, ease: 'linear', delay: 0.1 }}
          />
        </div>

        {/* Description skeleton */}
        <div className="space-y-2">
          <div className="h-3 bg-surface-secondary rounded w-full relative overflow-hidden">
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
              animate={{ x: ['-100%', '100%'] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'linear', delay: 0.2 }}
            />
          </div>
          <div className="h-3 bg-surface-secondary rounded w-5/6 relative overflow-hidden">
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
              animate={{ x: ['-100%', '100%'] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'linear', delay: 0.3 }}
            />
          </div>
        </div>

        {/* Meta skeleton */}
        <div className="flex items-center gap-4 pt-2">
          <div className="h-4 bg-surface-secondary rounded w-16 relative overflow-hidden">
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
              animate={{ x: ['-100%', '100%'] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'linear', delay: 0.4 }}
            />
          </div>
          <div className="h-4 bg-surface-secondary rounded w-20 relative overflow-hidden">
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-surface-secondary/50 to-transparent"
              animate={{ x: ['-100%', '100%'] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'linear', delay: 0.5 }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export function ContentCardSkeletonGrid({ count = 6 }: { count?: number }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <ContentCardSkeleton key={i} />
      ))}
    </div>
  );
}
