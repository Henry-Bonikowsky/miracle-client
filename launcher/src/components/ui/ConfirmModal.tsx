import { motion, AnimatePresence } from 'framer-motion';
import { X, AlertTriangle, AlertCircle, Info, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import { useState, useEffect, useCallback, createContext, useContext, ReactNode } from 'react';

type ModalVariant = 'danger' | 'warning' | 'info';

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ModalVariant;
  showInput?: boolean;
  inputPlaceholder?: string;
  inputDefaultValue?: string;
  onInputConfirm?: (value: string) => void;
}

const variantConfig = {
  danger: {
    icon: Trash2,
    iconBg: 'bg-red-500/20',
    iconColor: 'text-red-400',
    confirmBg: 'bg-red-500 hover:bg-red-600',
  },
  warning: {
    icon: AlertTriangle,
    iconBg: 'bg-yellow-500/20',
    iconColor: 'text-yellow-400',
    confirmBg: 'bg-yellow-500 hover:bg-yellow-600',
  },
  info: {
    icon: Info,
    iconBg: 'bg-miracle-500/20',
    iconColor: 'text-miracle-400',
    confirmBg: 'bg-miracle-500 hover:bg-miracle-600',
  },
};

export default function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  showInput = false,
  inputPlaceholder = '',
  inputDefaultValue = '',
  onInputConfirm,
}: ConfirmModalProps) {
  const [inputValue, setInputValue] = useState(inputDefaultValue);
  const config = variantConfig[variant];
  const Icon = config.icon;

  useEffect(() => {
    if (isOpen) {
      setInputValue(inputDefaultValue);
    }
  }, [isOpen, inputDefaultValue]);

  const handleConfirm = () => {
    if (showInput && onInputConfirm) {
      onInputConfirm(inputValue);
    } else {
      onConfirm();
    }
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (!showInput || inputValue.trim())) {
      handleConfirm();
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />

          {/* Modal */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
            className="relative glass rounded-xl border border-theme-muted/20/50 shadow-2xl w-[400px] max-w-[90vw]"
            onKeyDown={handleKeyDown}
          >
            {/* Close button */}
            <button
              onClick={onClose}
              className="absolute top-4 right-4 p-1 hover:bg-surface-secondary/50 rounded-lg transition-colors text-theme-muted hover:text-theme-primary"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="p-6">
              {/* Icon */}
              <div className={clsx('w-12 h-12 rounded-full flex items-center justify-center mb-4', config.iconBg)}>
                <Icon className={clsx('w-6 h-6', config.iconColor)} />
              </div>

              {/* Title */}
              <h3 className="text-lg font-semibold text-theme-primary mb-2">{title}</h3>

              {/* Message */}
              <p className="text-theme-secondary text-sm mb-6">{message}</p>

              {/* Input (optional) */}
              {showInput && (
                <input
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder={inputPlaceholder}
                  className="w-full px-4 py-2.5 bg-surface-secondary border border-theme-muted/20 rounded-lg focus:border-miracle-500 focus:outline-none transition-colors mb-6"
                  autoFocus
                />
              )}

              {/* Actions */}
              <div className="flex gap-3">
                <button
                  onClick={onClose}
                  className="flex-1 px-4 py-2.5 rounded-lg bg-surface-secondary hover:bg-miracle-500/20 text-theme-primary font-medium transition-colors"
                >
                  {cancelLabel}
                </button>
                <button
                  onClick={handleConfirm}
                  disabled={showInput && !inputValue.trim()}
                  className={clsx(
                    'flex-1 px-4 py-2.5 rounded-lg text-theme-primary font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed',
                    config.confirmBg
                  )}
                >
                  {confirmLabel}
                </button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

// Alert Modal (single button)
interface AlertModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  message: string;
  buttonLabel?: string;
  variant?: ModalVariant;
}

export function AlertModal({
  isOpen,
  onClose,
  title,
  message,
  buttonLabel = 'OK',
  variant = 'info',
}: AlertModalProps) {
  const config = variantConfig[variant];
  const Icon = variant === 'danger' ? AlertCircle : config.icon;

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            className="relative glass rounded-xl border border-theme-muted/20/50 shadow-2xl w-[400px] max-w-[90vw] p-6"
          >
            <div className={clsx('w-12 h-12 rounded-full flex items-center justify-center mb-4', config.iconBg)}>
              <Icon className={clsx('w-6 h-6', config.iconColor)} />
            </div>

            <h3 className="text-lg font-semibold text-theme-primary mb-2">{title}</h3>
            <p className="text-theme-secondary text-sm mb-6 whitespace-pre-wrap">{message}</p>

            <button
              onClick={onClose}
              className={clsx(
                'w-full px-4 py-2.5 rounded-lg text-theme-primary font-medium transition-colors',
                config.confirmBg
              )}
            >
              {buttonLabel}
            </button>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

// Context for global confirm/alert modals
interface ModalContextValue {
  confirm: (options: {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    variant?: ModalVariant;
  }) => Promise<boolean>;
  alert: (options: {
    title: string;
    message: string;
    buttonLabel?: string;
    variant?: ModalVariant;
  }) => Promise<void>;
  prompt: (options: {
    title: string;
    message: string;
    placeholder?: string;
    defaultValue?: string;
    confirmLabel?: string;
    cancelLabel?: string;
  }) => Promise<string | null>;
}

const ModalContext = createContext<ModalContextValue | null>(null);

export function useModal() {
  const context = useContext(ModalContext);
  if (!context) {
    throw new Error('useModal must be used within a ModalProvider');
  }
  return context;
}

interface ModalState {
  type: 'confirm' | 'alert' | 'prompt';
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  buttonLabel?: string;
  variant?: ModalVariant;
  placeholder?: string;
  defaultValue?: string;
  resolve: (value: boolean | string | null) => void;
}

export function ModalProvider({ children }: { children: ReactNode }) {
  const [modal, setModal] = useState<ModalState | null>(null);

  const confirm = useCallback((options: {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    variant?: ModalVariant;
  }): Promise<boolean> => {
    return new Promise((resolve) => {
      setModal({ type: 'confirm', ...options, resolve: resolve as (v: boolean | string | null) => void });
    });
  }, []);

  const alert = useCallback((options: {
    title: string;
    message: string;
    buttonLabel?: string;
    variant?: ModalVariant;
  }): Promise<void> => {
    return new Promise((resolve) => {
      setModal({ type: 'alert', ...options, resolve: () => resolve() });
    });
  }, []);

  const prompt = useCallback((options: {
    title: string;
    message: string;
    placeholder?: string;
    defaultValue?: string;
    confirmLabel?: string;
    cancelLabel?: string;
  }): Promise<string | null> => {
    return new Promise((resolve) => {
      setModal({ type: 'prompt', variant: 'info', ...options, resolve: resolve as (v: boolean | string | null) => void });
    });
  }, []);

  const handleClose = useCallback(() => {
    if (modal) {
      if (modal.type === 'confirm') {
        modal.resolve(false);
      } else if (modal.type === 'prompt') {
        modal.resolve(null);
      } else {
        modal.resolve(null);
      }
      setModal(null);
    }
  }, [modal]);

  const handleConfirm = useCallback(() => {
    if (modal) {
      modal.resolve(true);
      setModal(null);
    }
  }, [modal]);

  const handleInputConfirm = useCallback((value: string) => {
    if (modal) {
      modal.resolve(value);
      setModal(null);
    }
  }, [modal]);

  return (
    <ModalContext.Provider value={{ confirm, alert, prompt }}>
      {children}

      {modal?.type === 'confirm' && (
        <ConfirmModal
          isOpen={true}
          onClose={handleClose}
          onConfirm={handleConfirm}
          title={modal.title}
          message={modal.message}
          confirmLabel={modal.confirmLabel}
          cancelLabel={modal.cancelLabel}
          variant={modal.variant}
        />
      )}

      {modal?.type === 'alert' && (
        <AlertModal
          isOpen={true}
          onClose={handleClose}
          title={modal.title}
          message={modal.message}
          buttonLabel={modal.buttonLabel}
          variant={modal.variant}
        />
      )}

      {modal?.type === 'prompt' && (
        <ConfirmModal
          isOpen={true}
          onClose={handleClose}
          onConfirm={() => {}}
          onInputConfirm={handleInputConfirm}
          title={modal.title}
          message={modal.message}
          confirmLabel={modal.confirmLabel || 'OK'}
          cancelLabel={modal.cancelLabel}
          variant="info"
          showInput={true}
          inputPlaceholder={modal.placeholder}
          inputDefaultValue={modal.defaultValue}
        />
      )}
    </ModalContext.Provider>
  );
}
