import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/lib/stores/authStore';
import { Loader2, Copy, ExternalLink, Check } from 'lucide-react';
import { invoke } from '@tauri-apps/api/core';
import { writeText } from '@tauri-apps/plugin-clipboard-manager';
import { useModal } from '@/components/ui';

export default function LoginPage() {
  const navigate = useNavigate();
  const modal = useModal();
  const { startLogin, completeLogin, deviceCode, isLoading, error } = useAuthStore();
  const [copied, setCopied] = useState(false);
  const [autoCopied, setAutoCopied] = useState(false);

  const handleLogin = async () => {
    try {
      await startLogin();
    } catch (e) {
      console.error("Failed to start login:", e);
    }
  };

  const openBrowser = async (url: string) => {
    try {
      await invoke("plugin:shell|open", { path: url });
    } catch (e) {
      console.error("Failed to open browser:", e);
      await writeText(url);
      await modal.alert({
        title: "Browser Not Opened",
        message: "Could not open browser automatically. The URL has been copied to your clipboard.",
        variant: "warning",
      });
    }
  };

  useEffect(() => {
    if (deviceCode) {
      const openInBrowser = async () => {
        // Auto-copy the code
        try {
          console.log("Auto-copying code:", deviceCode.user_code);
          await writeText(deviceCode.user_code);
          console.log("Auto-copy successful");
          setAutoCopied(true);
        } catch (e) {
          console.error("Auto-copy failed:", e);
          try {
            await navigator.clipboard.writeText(deviceCode.user_code);
            setAutoCopied(true);
          } catch (e2) {
            console.error("Fallback also failed:", e2);
          }
        }
        await openBrowser(deviceCode.verification_uri);
        completeLogin()
          .then(() => navigate("/"))
          .catch((e) => console.error("Login failed:", e));
      };
      openInBrowser();
    }
  }, [deviceCode, completeLogin, navigate]);

  const copyCode = async () => {
    if (deviceCode) {
      try {
        console.log("Copying code:", deviceCode.user_code);
        await writeText(deviceCode.user_code);
        console.log("Copy successful");
        setCopied(true);
        setAutoCopied(true);
      } catch (e) {
        console.error("Copy failed:", e);
        // Fallback: try navigator.clipboard
        try {
          await navigator.clipboard.writeText(deviceCode.user_code);
          setCopied(true);
          setAutoCopied(true);
        } catch (e2) {
          console.error("Fallback copy also failed:", e2);
        }
      }
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const copyUrl = async () => {
    if (deviceCode) {
      await writeText(deviceCode.verification_uri);
    }
  };

  return (
    <div className="h-full flex items-center justify-center p-8">
      <div className="glass rounded-2xl p-8 max-w-md w-full border border-theme-muted/20/50">
        <div className="flex flex-col items-center mb-8">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-miracle-400 to-miracle-600 flex items-center justify-center mb-4 shadow-lg shadow-miracle-600/30">
            <span className="text-3xl font-bold text-white">M</span>
          </div>
          <h1 className="text-2xl font-bold gradient-text">Miracle Client</h1>
          <p className="text-theme-muted text-sm mt-2">The next generation Minecraft experience</p>
        </div>

        {error && (
          <div className="mb-6 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
            <div className="flex items-start justify-between gap-2 mb-2">
              <span className="text-red-400 text-xs font-semibold">Error:</span>
              <button onClick={() => writeText(error)} className="px-2 py-1 bg-red-600 hover:bg-red-500 rounded text-xs flex items-center gap-1">
                <Copy className="w-3 h-3" />Copy
              </button>
            </div>
            <pre className="text-red-400 text-xs whitespace-pre-wrap break-all select-text font-mono" style={{ userSelect: "text" }}>{error}</pre>
          </div>
        )}

        {deviceCode ? (
          <div className="space-y-4">
            <div className="text-center">
              <p className="text-theme-muted text-sm mb-2">Enter this code at Microsoft:</p>
              {autoCopied && (
                <div className="flex items-center justify-center gap-1 text-green-400 text-xs mb-2">
                  <Check className="w-3 h-3" />
                  <span>Code copied to clipboard!</span>
                </div>
              )}
              <div className="flex items-center justify-center gap-2">
                <code className="text-2xl font-mono font-bold tracking-wider text-miracle-400 bg-surface-secondary px-4 py-2 rounded-lg select-all cursor-pointer" style={{ userSelect: "all" }}>
                  {deviceCode.user_code}
                </code>
                <button onClick={copyCode} className={"p-2 rounded-lg transition-colors " + (copied ? "bg-green-600/20" : "hover:bg-surface-secondary")} title={copied ? "Copied!" : "Copy code"}>
                  {copied ? <Check className="w-5 h-5 text-green-400" /> : <Copy className="w-5 h-5 text-theme-muted" />}
                </button>
              </div>
            </div>
            <button onClick={() => openBrowser(deviceCode.verification_uri)} className="w-full py-3 px-4 bg-surface-secondary hover:bg-surface-secondary rounded-lg font-medium transition-colors flex items-center justify-center gap-2">
              <ExternalLink className="w-4 h-4" />Open Microsoft Login
            </button>
            {deviceCode.message && <p className="text-theme-muted text-xs text-center">{deviceCode.message}</p>}
            <div className="p-3 bg-surface-secondary/50 rounded-lg border border-theme-muted/20/30">
              <div className="flex items-center justify-between gap-2 mb-2">
                <span className="text-xs text-theme-muted">Verification URL:</span>
                <button onClick={copyUrl} className="px-2 py-1 bg-miracle-600 hover:bg-miracle-500 rounded text-xs flex items-center gap-1 transition-colors" title="Copy URL">
                  <Copy className="w-3 h-3" />Copy
                </button>
              </div>
              <div className="w-full p-2 bg-surface-primary rounded border border-theme-muted/20 text-xs text-miracle-400 font-mono break-all select-text cursor-text" style={{ userSelect: "text", WebkitUserSelect: "text", MozUserSelect: "text" }}>
                {deviceCode.verification_uri}
              </div>
            </div>
            <div className="flex items-center justify-center gap-2 text-theme-muted text-sm">
              <Loader2 className="w-4 h-4 animate-spin" />Waiting for authentication...
            </div>
          </div>
        ) : (
          <>
            <button onClick={handleLogin} disabled={isLoading} className="w-full py-3 px-4 bg-gradient-to-r from-miracle-500 to-miracle-600 hover:from-miracle-400 hover:to-miracle-500 rounded-lg font-semibold transition-all duration-200 btn-glow disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
              {isLoading ? (<><Loader2 className="w-5 h-5 animate-spin" />Connecting...</>) : (<><svg className="w-5 h-5" viewBox="0 0 21 21" fill="currentColor"><path d="M0 0h10v10H0zM11 0h10v10H11zM0 11h10v10H0zM11 11h10v10H11z" /></svg>Sign in with Microsoft</>)}
            </button>
            <p className="text-theme-muted text-xs text-center mt-6">By signing in, you agree to our Terms of Service and Privacy Policy</p>
          </>
        )}
      </div>
    </div>
  );
}