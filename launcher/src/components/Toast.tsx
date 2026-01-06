import { useEffect, useState } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import clsx from 'clsx';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

interface ToastProps {
  message: string;
  type: ToastType;
  onClose: () => void;
  duration?: number;
  onClick?: () => void;
}

export default function Toast({ message, type, onClose, duration = 8000, onClick }: ToastProps) {
  const [isFadingOut, setIsFadingOut] = useState(false);

  useEffect(() => {
    if (duration > 0) {
      // Start fade-out animation 300ms before removal
      const fadeTimer = setTimeout(() => {
        setIsFadingOut(true);
      }, duration - 300);

      // Remove toast after fade animation completes
      const removeTimer = setTimeout(() => {
        onClose();
      }, duration);

      return () => {
        clearTimeout(fadeTimer);
        clearTimeout(removeTimer);
      };
    }
  }, [duration, onClose]);

  const handleClose = () => {
    setIsFadingOut(true);
    setTimeout(() => {
      onClose();
    }, 300);
  };

  const icons = {
    success: CheckCircle,
    error: AlertCircle,
    info: Info,
    warning: AlertCircle,
  };

  const Icon = icons[type];

  const colors = {
    success: {
      bg: 'bg-green-500/70',
      border: 'border-green-500/80',
      text: 'text-green-100',
      icon: 'text-green-200',
    },
    error: {
      bg: 'bg-red-500/70',
      border: 'border-red-500/80',
      text: 'text-red-100',
      icon: 'text-red-200',
    },
    info: {
      bg: 'bg-miracle-500/70',
      border: 'border-miracle-500/80',
      text: 'text-miracle-100',
      icon: 'text-miracle-200',
    },
    warning: {
      bg: 'bg-yellow-500/70',
      border: 'border-yellow-500/80',
      text: 'text-yellow-100',
      icon: 'text-yellow-200',
    },
  };

  const colorScheme = colors[type];

  return (
    <div
      className={clsx(
        'glass rounded-lg border p-4 shadow-2xl min-w-[300px] max-w-md',
        'animate-in slide-in-from-right-full duration-300',
        colorScheme.bg,
        colorScheme.border,
        'cursor-pointer hover:scale-105 transition-all',
        'transition-opacity duration-300',
        isFadingOut && 'opacity-0',
        type === 'warning' && 'animate-pulse'
      )}
      onClick={() => {
        if (onClick) {
          onClick();
        }
        handleClose();
      }}
    >
      <div className="flex items-start gap-3">
        <Icon className={clsx('w-5 h-5 flex-shrink-0 mt-0.5', colorScheme.icon)} />
        <p className={clsx('flex-1 text-sm leading-relaxed select-text', colorScheme.text)}>
          {message}
        </p>
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleClose();
          }}
          className="flex-shrink-0 p-1 hover:bg-surface-secondary/50 rounded transition-colors"
        >
          <X className="w-4 h-4 text-theme-muted hover:text-white" />
        </button>
      </div>
    </div>
  );
}
