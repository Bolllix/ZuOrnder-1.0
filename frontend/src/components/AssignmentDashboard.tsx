import React, { useState } from 'react';
import type { Project, AssignmentResult, AssignmentPair } from '../types';
import { api } from '../services/api';
import { Sparkles, Bed as BedIcon, Info, RefreshCw, Download } from 'lucide-react';

interface AssignmentDashboardProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

export const AssignmentDashboard: React.FC<AssignmentDashboardProps> = ({ project, onProjectUpdate }) => {
  const [isCalculating, setIsCalculating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [selectedPair, setSelectedPair] = useState<AssignmentPair | null>(null);

  const handleRunOptimization = async () => {
    setIsCalculating(true);
    try {
      const result = await api.calculateAssignment(project.id);
      const updatedProject = { ...project, assignmentResult: result };
      onProjectUpdate(updatedProject);
    } catch (e) {
      alert('Fehler beim Berechnen der Belegung.');
    } finally {
      setIsCalculating(false);
    }
  };

  const handleExportExcel = async () => {
    setIsExporting(true);
    try {
      await api.exportExcel(project.id, project.name);
    } catch (e) {
      alert('Fehler beim Exportieren der Excel-Datei.');
    } finally {
      setIsExporting(false);
    }
  };

  const result: AssignmentResult | undefined = project.assignmentResult;

  return (
    <div className="space-y-6">
      {/* Action Header */}
      <div className="glass-panel p-6 rounded-2xl border border-indigo-500/30 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <Sparkles className="w-7 h-7 text-indigo-400" /> Zimmer- & Bettenbelegungsplan
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Optimale Zuordnung berechnen mittels Ungarischem Algorithmus (Hungarian Solver).
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={handleExportExcel}
            disabled={isExporting}
            className="flex items-center space-x-2 px-5 py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm rounded-xl shadow-lg shadow-emerald-500/20 transition transform active:scale-95 disabled:opacity-50"
          >
            <Download className={`w-5 h-5 ${isExporting ? 'animate-bounce' : ''}`} />
            <span>{isExporting ? 'Erstelle Excel...' : 'Excel-Export (.xlsx)'}</span>
          </button>

          <button
            onClick={handleRunOptimization}
            disabled={isCalculating}
            className="flex items-center space-x-2 px-6 py-3 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold text-sm rounded-xl shadow-lg shadow-indigo-500/30 transition transform active:scale-95 disabled:opacity-50"
          >
            <RefreshCw className={`w-5 h-5 ${isCalculating ? 'animate-spin' : ''}`} />
            <span>{isCalculating ? 'Berechne optimalen Score...' : 'Belegung berechnen'}</span>
          </button>
        </div>
      </div>

      {/* Results Summary Stats Banner */}
      {result && (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div className="glass-panel p-4 rounded-xl border border-emerald-500/30">
            <div className="text-xs text-slate-400 font-medium">Gesamt-Score</div>
            <div className="text-2xl font-bold text-emerald-400 mt-1 font-mono">+{result.totalScore} Pkt</div>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-slate-700">
            <div className="text-xs text-slate-400 font-medium">Zugeordnete Personen</div>
            <div className="text-2xl font-bold text-slate-100 mt-1 font-mono">
              {result.assignments.length} / {project.persons.length}
            </div>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-slate-700">
            <div className="text-xs text-slate-400 font-medium">Verletzte Harte Regeln</div>
            <div className={`text-2xl font-bold mt-1 font-mono ${result.hardRuleViolationsCount > 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
              {result.hardRuleViolationsCount}
            </div>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-slate-700">
            <div className="text-xs text-slate-400 font-medium">Berechnungsdauer</div>
            <div className="text-2xl font-bold text-indigo-400 mt-1 font-mono">{result.executionTimeMs} ms</div>
          </div>
        </div>
      )}

      {/* Room Occupancy Grid */}
      {!result || result.assignments.length === 0 ? (
        <div className="glass-panel p-12 rounded-2xl text-center space-y-3 border border-slate-800">
          <Sparkles className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-lg font-semibold text-slate-300">Noch keine Belegung berechnet</h3>
          <p className="text-slate-400 text-sm max-w-md mx-auto">
            Klicken Sie oben auf "Belegung berechnen", um die optimale Verteilung Ihrer Teilnehmer auf die Betten zu ermitteln.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {project.buildings.map((building) => (
            <div key={building.id} className="space-y-4">
              <h3 className="text-lg font-bold text-slate-200 border-b border-slate-800 pb-2">{building.name}</h3>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {building.rooms.map((room) => (
                  <div key={room.id} className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-4">
                    <div className="flex items-center justify-between border-b border-slate-700/60 pb-2">
                      <div>
                        <span className="font-bold text-slate-100 text-md">{room.name}</span>
                        <span className="ml-2 text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400">Etage {room.floor}</span>
                      </div>
                      {room.girlsRoom && <span className="text-xs text-pink-300 font-semibold">Mädchenzimmer</span>}
                    </div>

                    {/* Beds in Room */}
                    <div className="space-y-2">
                      {room.beds.map((bed) => {
                        const pair = result.assignments.find((a) => a.bedId === bed.id);
                        return (
                          <div
                            key={bed.id}
                            onClick={() => pair && setSelectedPair(pair)}
                            className={`p-3 rounded-xl border transition cursor-pointer flex items-center justify-between ${
                              pair
                                ? 'bg-slate-800/90 border-indigo-500/40 hover:border-indigo-400 shadow-sm'
                                : 'bg-slate-900/40 border-slate-800/80 text-slate-500'
                            }`}
                          >
                            <div className="flex items-center space-x-3">
                              <BedIcon className={`w-5 h-5 ${pair ? 'text-indigo-400' : 'text-slate-600'}`} />
                              <div>
                                <div className="text-xs font-semibold text-slate-200">{bed.name}</div>
                                {pair ? (
                                  <div className="text-sm font-bold text-indigo-300 mt-0.5">{pair.personName}</div>
                                ) : (
                                  <div className="text-xs italic text-slate-500">Freies Bett</div>
                                )}
                              </div>
                            </div>

                            {pair && (
                              <div className="flex items-center space-x-2">
                                <span className="text-xs font-mono font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                                  +{pair.score} Pkt
                                </span>
                                <Info className="w-4 h-4 text-slate-400 hover:text-slate-200" />
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Transparent Score Explanation Modal */}
      {selectedPair && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-indigo-500/40 max-w-lg w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
              <div>
                <h3 className="text-lg font-bold text-slate-100">{selectedPair.personName}</h3>
                <p className="text-xs text-indigo-400 font-medium">
                  {selectedPair.buildingName} • {selectedPair.roomName} • {selectedPair.bedName}
                </p>
              </div>
              <span className="text-sm font-bold font-mono text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/30">
                Score: +{selectedPair.score} Pkt
              </span>
            </div>

            <div className="space-y-2">
              <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Erfüllte Regeln & Begründung</h4>
              {selectedPair.explanations.length === 0 ? (
                <p className="text-xs text-slate-400 italic">Standardzuordnung (keine spezifischen Punkteboni/Strafen).</p>
              ) : (
                selectedPair.explanations.map((exp, idx) => (
                  <div key={idx} className="p-3 bg-slate-900/80 rounded-xl border border-slate-800 flex items-start justify-between">
                    <div className="space-y-0.5">
                      <div className="text-xs font-bold text-slate-200">{exp.ruleName}</div>
                      <div className="text-xs text-slate-400">{exp.reason}</div>
                    </div>
                    <span className={`text-xs font-mono font-bold ${exp.points >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                      {exp.points >= 0 ? `+${exp.points}` : exp.points}
                    </span>
                  </div>
                ))
              )}
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setSelectedPair(null)}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded-lg"
              >
                Schließen
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
