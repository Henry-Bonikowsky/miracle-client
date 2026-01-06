import { motion } from 'framer-motion';
import { AlertCircle, RefreshCw } from 'lucide-react';
import clsx from 'clsx';

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
  className?: string;
}

export default function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
  retryLabel = 'Try Again',
  className,
}: ErrorStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      className={clsx(
        'flex flex-col items-center justify-center py-12 px-8 text-center',
        className
      )}
    >
      <motion.div
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ delay: 0.1, duration: 0.3 }}
        className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center mb-5"
      >
        <AlertCircle className="w-8 h-8 text-red-400" />
      </motion.div>

      <motion.h3
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2, duration: 0.3 }}
        className="text-lg font-semibold text-theme-primary mb-2"
      >
        {title}
      </motion.h3>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.25, duration: 0.3 }}
        className="text-theme-muted max-w-md mb-6"
      >
        {message}
      </motion.p>

      {onRetry && (
        <motion.button
          initial={{ opacity: 0, y: 5 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.3 }}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onRetry}
          className="px-5 py-2 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 text-theme-primary font-medium transition-colors flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          {retryLabel}
        </motion.button>
      )}
    </motion.div>
  );
}
