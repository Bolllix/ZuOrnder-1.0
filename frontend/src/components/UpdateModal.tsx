import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { SystemVersionInfo, UpdateStatusInfo } from '../types';
import { 
  Sparkles, 
  DownloadCloud, 
  CheckCircle2, 
  AlertTriangle, 
  RefreshCw, 
  X, 
  ShieldCheck, 
  Laptop, 
  Globe 
} from 'lucide-react';

interface UpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialInfo: SystemVersionInfo | null;
  onRefreshInfo: () => void;
}

export const UpdateModal: React.FC<UpdateModalProps> = ({
  isOpen,
  onClose,
  initialInfo,
  onRefreshInfo
}) => {
  const [info, setInfo] = useState<SystemVersionInfo | null>(initialInfo);
  const [checking, setChecking] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [updateStatus, setUpdateStatus] = useState<UpdateStatusInfo | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isRestarting, setIsRestarting] = useState(false);

  useEffect(() => {
    if (initialInfo) {
      setInfo(initialInfo);
    }
  }, [initialInfo]);

  useEffect(() => {
    let interval: any = null;
    if (updating && !isRestarting) {
      interval = setInterval(async () => {
        try {
          const status = await api.getUpdateStatus();
          setUpdateStatus(status);

          if (status.step === 'RESTARTING') {
            setIsRestarting(true);
            clearInterval(interval);
            pollServerRestart();
          } else if (status.step === 'ERROR') {
            setErrorMsg(status.errorMessage || 'Beim Aktualisieren ist ein Fehler aufgetreten.');
            setUpdating(false);
            clearInterval(interval);
          }
        } catch {
          // If connection drops momentarily, server might already be restarting
        }
      }, 700);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [updating, isRestarting]);

  const pollServerRestart = () => {
    let attempts = 0;
    const pollInterval = setInterval(async () => {
      attempts++;
      try {
        const res = await api.getVersionInfo();
        if (res && res.currentVersion) {
          clearInterval(pollInterval);
          setTimeout(() => {
            window.location.reload();
          }, 1200);
        }
      } catch {
        // Still rebooting, keep polling
        if (attempts > 30) {
          clearInterval(pollInterval);
          setErrorMsg('Der Server benötigt länger zum Neustarten. Bitte lade die Seite in wenigen Augenblicken manuell neu.');
        }
      }
    }, 1500);
  };

  const handleManualCheck = async () => {
    setChecking(true);
    setErrorMsg(null);
    try {
      const fresh = await api.getVersionInfo();
      setInfo(fresh);
      onRefreshInfo();
    } catch (e: any) {
      setErrorMsg('Konnte Update-Server nicht erreichen. Bitte Internetverbindung prüfen.');
    } finally {
      setChecking(false);
    }
  };

  const handleStartUpdate = async () => {
    setUpdating(true);
    setErrorMsg(null);
    try {
      await api.triggerUpdate();
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || 'Update konnte nicht gestartet werden.');
      setUpdating(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-semibold text-base text-slate-100">ZuORDNER Updates & Version</h3>
              <p className="text-xs text-slate-400">Softwareaktualisierung & Systemstatus</p>
            </div>
          </div>
          {!updating && (
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Mode & Version Info Banner */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-3.5">
              <span className="text-[11px] text-slate-400 uppercase tracking-wider block font-semibold">Installierte Version</span>
              <div className="flex items-center space-x-2 mt-1">
                <span className="text-lg font-bold font-mono text-slate-100">
                  v{info?.currentVersion || '1.1.0'}
                </span>
                {info?.isDesktop ? (
                  <span className="inline-flex items-center gap-1 text-[10px] bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded-full border border-indigo-500/30">
                    <Laptop className="w-3 h-3" /> Desktop
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-500/30">
                    <Globe className="w-3 h-3" /> Web
                  </span>
                )}
              </div>
            </div>

            <div className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-3.5">
              <span className="text-[11px] text-slate-400 uppercase tracking-wider block font-semibold">Neueste Version</span>
              <div className="flex items-center space-x-2 mt-1">
                <span className="text-lg font-bold font-mono text-slate-100">
                  v{info?.latestVersion || info?.currentVersion || '1.1.0'}
                </span>
                {info?.updateAvailable ? (
                  <span className="inline-flex items-center gap-1 text-[10px] bg-amber-500/20 text-amber-300 px-2 py-0.5 rounded-full border border-amber-500/30 font-medium">
                    Neu verfügbar
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-500/30">
                    <CheckCircle2 className="w-3 h-3" /> Aktuell
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Updating / Restarting State */}
          {updating ? (
            <div className="bg-indigo-950/30 border border-indigo-500/40 rounded-xl p-5 space-y-4 text-center">
              <div className="w-12 h-12 rounded-2xl bg-indigo-600/30 border border-indigo-500 flex items-center justify-center text-indigo-300 mx-auto animate-pulse">
                <DownloadCloud className="w-6 h-6 animate-bounce" />
              </div>

              <div>
                <h4 className="font-semibold text-slate-100 text-sm">
                  {isRestarting ? 'ZuORDNER startet neu...' : 'Update wird heruntergeladen...'}
                </h4>
                <p className="text-xs text-slate-400 mt-1">
                  {isRestarting 
                    ? 'Der Server wird aktualisiert. Die Seite lädt gleich automatisch neu.' 
                    : `Fortschritt: ${updateStatus?.progressPercent || 0}% abgeschlossen`}
                </p>
              </div>

              {/* Progress bar */}
              <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden border border-slate-700">
                <div 
                  className="bg-gradient-to-r from-indigo-500 to-emerald-400 h-2.5 rounded-full transition-all duration-300"
                  style={{ width: `${isRestarting ? 100 : (updateStatus?.progressPercent || 10)}%` }}
                />
              </div>

              <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
                <RefreshCw className="w-3.5 h-3.5 animate-spin text-indigo-400" />
                <span>Bitte die Anwendung nicht schließen</span>
              </div>
            </div>
          ) : (
            <>
              {/* Update Available Box */}
              {info?.updateAvailable ? (
                <div className="bg-gradient-to-br from-indigo-900/40 via-purple-900/20 to-slate-900 border border-indigo-500/40 rounded-xl p-4 space-y-3">
                  <div className="flex items-start gap-3">
                    <div className="p-2 bg-indigo-600/30 rounded-lg text-indigo-300 shrink-0">
                      <Sparkles className="w-5 h-5 text-indigo-400" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-slate-100 text-sm">
                        Neues Update v{info.latestVersion} bereit!
                      </h4>
                      {info.releaseNotes && (
                        <p className="text-xs text-indigo-200/90 mt-1 leading-relaxed">
                          {info.releaseNotes}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 text-[11px] text-slate-300 pt-2 border-t border-indigo-500/20">
                    <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                    <span>Deine gespeicherten Gebäude, Betten und Teilnehmer bleiben zu 100% erhalten.</span>
                  </div>
                </div>
              ) : (
                /* Everything up to date */
                <div className="bg-emerald-950/20 border border-emerald-500/30 rounded-xl p-4 flex items-center gap-3">
                  <div className="p-2 bg-emerald-500/20 rounded-lg text-emerald-400 shrink-0">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="font-semibold text-slate-100 text-sm">ZuORDNER ist auf dem neuesten Stand</h4>
                    <p className="text-xs text-slate-400 mt-0.5">
                      {info?.isDesktop 
                        ? 'Du nutzt bereits die aktuellste Version der Windows Desktop-App.' 
                        : 'Die Webversion wird bei jedem Server-Deployment automatisch aktualisiert.'}
                    </p>
                  </div>
                </div>
              )}

              {/* Error Box if any */}
              {errorMsg && (
                <div className="bg-red-950/30 border border-red-500/40 rounded-xl p-3.5 flex items-start gap-3 text-red-200 text-xs">
                  <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-semibold block">Hinweis:</span>
                    <span>{errorMsg}</span>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer Actions */}
        {!updating && (
          <div className="px-6 py-4 border-t border-slate-800 bg-slate-950/50 flex items-center justify-between">
            <button
              onClick={handleManualCheck}
              disabled={checking}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${checking ? 'animate-spin text-indigo-400' : ''}`} />
              <span>{checking ? 'Prüfe...' : 'Nach Updates suchen'}</span>
            </button>

            {info?.updateAvailable && info?.isDesktop && info?.isJarRunning && (
              <button
                onClick={handleStartUpdate}
                className="flex items-center gap-2 px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition transform hover:scale-[1.02] active:scale-[0.98]"
              >
                <DownloadCloud className="w-4 h-4" />
                <span>Jetzt automatisch aktualisieren</span>
              </button>
            )}

            {info?.updateAvailable && (!info?.isDesktop || !info?.isJarRunning) && (
              <a
                href={info?.downloadUrl || 'https://github.com/Bolllix/ZuOrnder-1.0'}
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
              >
                <Globe className="w-3.5 h-3.5 text-indigo-400" />
                <span>GitHub aufrufen</span>
              </a>
            )}

            {!info?.updateAvailable && (
              <button
                onClick={onClose}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition"
              >
                Schließen
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
