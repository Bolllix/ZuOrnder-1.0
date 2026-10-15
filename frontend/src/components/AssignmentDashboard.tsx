// ============================================================================
// AssignmentDashboard – Belegung berechnen und anzeigen (Reiter "Belegungs-Optimierung")
// ============================================================================
// - Knopf "Belegung berechnen" schickt die Anfrage an den Server
//   (POST /api/projects/{id}/assignment/calculate).
// - Danach werden alle Gebäude, Räume und Betten mit ihrer Belegung gezeigt.
// - Klick auf ein belegtes Bett öffnet die Begründung (welche Regeln Punkte gaben).
// - Personen ohne Bett werden mit dem Grund aufgelistet.
import React, { useState } from 'react';
import type { Project, Person, AssignmentResult, AssignmentPair } from '../types';
import { api } from '../services/api';
import { Sparkles, Bed as BedIcon, Info, RefreshCw, Download, UserX } from 'lucide-react';

interface AssignmentDashboardProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

/** Punkte mit Vorzeichen anzeigen: 50 -> "+50", -25 -> "-25", 0 -> "0" */
const formatPoints = (points: number) => (points > 0 ? `+${points}` : `${points}`);

export const AssignmentDashboard: React.FC<AssignmentDashboardProps> = ({ project, onProjectUpdate }) => {
  const [isCalculating, setIsCalculating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  // Das angeklickte Bett (für das Begründungs-Fenster); null = Fenster zu
  const [selectedPair, setSelectedPair] = useState<AssignmentPair | null>(null);

  /** Berechnung auf dem Server starten und das Ergebnis ins Projekt übernehmen. */
  const handleRunOptimization = async () => {
    setIsCalculating(true);
    try {
      const result = await api.calculateAssignment(project.id);
      const updatedProject = { ...project, assignmentResult: result };
      onProjectUpdate(updatedProject);
    } catch {
      alert('Fehler beim Berechnen der Belegung.');
    } finally {
      setIsCalculating(false);
    }
  };

  const handleExportExcel = async () => {
    setIsExporting(true);
    try {
      await api.exportExcel(project.id, project.name);
    } catch {
      alert('Fehler beim Exportieren der Excel-Datei.');
    } finally {
      setIsExporting(false);
    }
  };

  const result: AssignmentResult | undefined = project.assignmentResult;

  // Personen ohne Bett (mit Namen statt nur IDs), für die Liste unten.
  // Personen, die inzwischen gelöscht wurden, werden übersprungen.
  const unassignedPersons: Array<{ person: Person; reason?: string }> = [];
  for (const id of result?.unassignedPersonIds ?? []) {
    const person = project.persons.find((p) => p.id === id);
    if (person) {
      unassignedPersons.push({ person, reason: result?.unassignedReasons?.[id] });
    }
  }

  return (
    <div className="space-y-6">
      {/* Kopfbereich mit den Aktions-Knöpfen */}
      <div className="glass-panel p-6 rounded-2xl border border-indigo-500/30 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <Sparkles className="w-7 h-7 text-indigo-400" /> Zimmer- & Bettenbelegungsplan
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Optimale Zuordnung: Ungarischer Algorithmus als Start, danach Verbesserung unter Berücksichtigung von Paaren, Gruppen und Geschlechtertrennung.
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

      {/* Kennzahlen */}
      {result && (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div className="glass-panel p-4 rounded-xl border border-emerald-500/30">
            <div className="text-xs text-slate-400 font-medium">Gesamt-Score</div>
            <div className={`text-2xl font-bold mt-1 font-mono ${result.totalScore >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {formatPoints(result.totalScore)} Pkt
            </div>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-slate-700">
            <div className="text-xs text-slate-400 font-medium">Zugeordnete Personen</div>
            <div className={`text-2xl font-bold mt-1 font-mono ${unassignedPersons.length > 0 ? 'text-amber-300' : 'text-slate-100'}`}>
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

      {/* Personen ohne Bett, jeweils mit Grund */}
      {result && unassignedPersons.length > 0 && (
        <div className="glass-panel p-5 rounded-2xl border border-amber-500/40 space-y-3">
          <h3 className="text-sm font-bold text-amber-300 flex items-center gap-2">
            <UserX className="w-4 h-4" /> {unassignedPersons.length} Person(en) ohne Bett
          </h3>
          <p className="text-xs text-slate-400">
            Harte Regeln werden nie gebrochen – lieber bleibt eine Person ohne Bett. Lösung: weitere Betten/Räume anlegen oder eine Regel ausschalten.
          </p>
          <ul className="space-y-1.5">
            {unassignedPersons.map(({ person, reason }) => (
              <li key={person.id} className="text-xs p-2.5 rounded-lg bg-slate-900/70 border border-slate-800">
                <span className="font-semibold text-slate-100">
                  {person.firstname} {person.lastname}
                </span>
                <span className="text-slate-400"> – {reason || 'Kein passendes freies Bett gefunden.'}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Zimmerübersicht */}
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
                      {room.boysRoom && <span className="text-xs text-blue-300 font-semibold">Jungenzimmer</span>}
                    </div>

                    {/* Betten im Raum: belegt (mit Name und Punkten) oder frei */}
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
                                <span
                                  className={`text-xs font-mono font-bold px-2 py-0.5 rounded-full border ${
                                    pair.score >= 0
                                      ? 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20'
                                      : 'text-rose-400 bg-rose-500/10 border-rose-500/20'
                                  }`}
                                >
                                  {formatPoints(pair.score)} Pkt
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

      {/* Fenster mit der Begründung für ein angeklicktes Bett */}
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
                Score: {formatPoints(selectedPair.score)} Pkt
              </span>
            </div>

            <div className="space-y-2 max-h-[50vh] overflow-y-auto pr-1">
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
                      {formatPoints(exp.points)}
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
