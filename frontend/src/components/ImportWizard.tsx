import React, { useState } from 'react';
import type { Project, TableData, ImportValidationResult } from '../types';
import { api } from '../services/api';
import { Upload, FileSpreadsheet, CheckCircle2, ArrowRight, Table, AlertTriangle, ShieldCheck } from 'lucide-react';

interface ImportWizardProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
  onComplete: () => void;
}

export const ImportWizard: React.FC<ImportWizardProps> = ({ project, onProjectUpdate, onComplete }) => {
  const [step, setStep] = useState<number>(1);
  const [file, setFile] = useState<File | null>(null);
  const [headerRowIndex, setHeaderRowIndex] = useState<number>(0);
  const [tableData, setTableData] = useState<TableData | null>(null);
  const [columnMapping, setColumnMapping] = useState<Record<string, string>>({});
  const [validationResult, setValidationResult] = useState<ImportValidationResult | null>(null);

  const availableAttributes = [
    { value: 'IGNORE', label: '-- Spalte ignorieren --' },
    { value: 'firstname', label: 'Vorname *' },
    { value: 'lastname', label: 'Nachname *' },
    { value: 'name', label: 'Vollständiger Name (Vorname Nachname)' },
    { value: 'gender', label: 'Geschlecht (m/w/d)' },
    { value: 'age', label: 'Alter' },
    { value: 'partnerid', label: 'Paar-ID' },
    { value: 'groupid', label: 'Gruppe / Klassenname' },
    { value: 'desiredfloor', label: 'Wunsch-Etage' },
    { value: 'desiredroom', label: 'Wunsch-Zimmer' },
    { value: 'specialneeds', label: 'Besondere Anforderungen' },
  ];

  // Step 1: File Upload & fetch headers preview (10 rows for display)
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);
      try {
        const data = await api.parseTable(project.id, selectedFile, headerRowIndex, 10);
        setTableData(data);
        setStep(2);
      } catch (err) {
        alert('Fehler beim Auslesen der Datei.');
      }
    }
  };

  // Step 2 & 3: Load Preview & Auto Suggest
  const handleLoadPreview = async () => {
    if (!file) return;
    try {
      const data = await api.parseTable(project.id, file, headerRowIndex, 10);
      setTableData(data);

      const initialMap: Record<string, string> = {};
      data.headers.forEach((h: string) => {
        const lower = h.toLowerCase();
        if (lower.includes('nachname')) initialMap[h] = 'lastname';
        else if (lower.includes('vorname')) initialMap[h] = 'firstname';
        else if (lower.includes('name')) initialMap[h] = 'name';
        else if (lower.includes('alter') || lower.includes('age')) initialMap[h] = 'age';
        else if (lower.includes('geschlecht') || lower.includes('gender') || lower.includes('sex')) initialMap[h] = 'gender';
        else if (lower.includes('paar') || lower.includes('partner')) initialMap[h] = 'partnerid';
        else if (lower.includes('gruppe') || lower.includes('group') || lower.includes('klasse')) initialMap[h] = 'groupid';
        else initialMap[h] = 'IGNORE';
      });
      setColumnMapping(initialMap);
      setStep(4);
    } catch (err) {
      alert('Fehler beim Generieren der Vorschau.');
    }
  };

  // Step 4: Validate Mapping on FULL file (reads ALL rows)
  const handleValidateMapping = async () => {
    if (!file) return;
    try {
      const result = await api.mapColumns(project.id, file, columnMapping, headerRowIndex);
      setValidationResult(result);
      setStep(5);
    } catch (err) {
      alert('Fehler bei der Spaltenzuordnung & Validierung.');
    }
  };

  // Step 5: Execute Import into Project
  const handleExecuteImport = async () => {
    if (!validationResult || validationResult.validPersons.length === 0) return;
    try {
      const updated = await api.savePersons(project.id, validationResult.validPersons);
      onProjectUpdate(updated);
      setStep(6);
    } catch (err) {
      alert('Fehler beim Importieren in das Projekt.');
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Step Indicator */}
      <div className="flex items-center justify-between glass-panel p-4 rounded-2xl border border-slate-800">
        {[1, 2, 3, 4, 5, 6].map((s) => (
          <div key={s} className="flex items-center space-x-2">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition ${
                step === s
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
                  : step > s
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                  : 'bg-slate-800 text-slate-500'
              }`}
            >
              {step > s ? <CheckCircle2 className="w-5 h-5" /> : s}
            </div>
            <span className={`text-xs font-medium hidden sm:inline ${step === s ? 'text-slate-100' : 'text-slate-500'}`}>
              {s === 1 && 'Upload'}
              {s === 2 && 'Vorschau'}
              {s === 3 && 'Zeilen-Header'}
              {s === 4 && 'Spalten-Mapping'}
              {s === 5 && 'Validierung'}
              {s === 6 && 'Fertig'}
            </span>
          </div>
        ))}
      </div>

      {/* Step 1: Upload */}
      {step === 1 && (
        <div className="glass-panel p-10 rounded-2xl border border-slate-800 text-center space-y-4">
          <div className="w-16 h-16 bg-indigo-600/20 rounded-full flex items-center justify-center mx-auto text-indigo-400">
            <Upload className="w-8 h-8" />
          </div>
          <h3 className="text-lg font-bold text-slate-100">Excel oder CSV Datei hochladen</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            Unterstützt .xlsx, .xls und .csv Dateien mit unvollständigen Reihen, abweichenden Spaltennamen oder Freitext.
          </p>
          <label className="inline-block px-6 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm rounded-xl cursor-pointer shadow-lg shadow-indigo-500/20 transition">
            <span>Datei auswählen</span>
            <input type="file" accept=".xlsx,.xls,.csv" onChange={handleFileChange} className="hidden" />
          </label>
        </div>
      )}

      {/* Step 2 & 3: Header selection & Row config */}
      {(step === 2 || step === 3) && tableData && (
        <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
            <FileSpreadsheet className="w-5 h-5 text-indigo-400" /> Headerzeile festlegen & Vorschau
          </h3>
          <div className="flex items-center space-x-3 text-xs">
            <label className="text-slate-400">Headerzeile Index (0 = 1. Zeile):</label>
            <input
              type="number"
              value={headerRowIndex}
              onChange={(e) => setHeaderRowIndex(parseInt(e.target.value) || 0)}
              className="px-2 py-1 bg-slate-900 border border-slate-700 rounded text-slate-200 w-16"
            />
          </div>

          <div className="overflow-x-auto max-h-60 rounded-xl border border-slate-800 bg-slate-950">
            <table className="w-full text-xs text-left text-slate-300">
              <thead className="bg-slate-900 text-slate-400 uppercase font-semibold">
                <tr>
                  {tableData.headers.map((h, i) => (
                    <th key={i} className="px-3 py-2 border-b border-slate-800">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {tableData.rows.slice(0, 5).map((r, ri) => (
                  <tr key={ri} className="border-b border-slate-900/60">
                    {r.map((c, ci) => (
                      <td key={ci} className="px-3 py-2 text-slate-400">{c}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex justify-end">
            <button
              onClick={handleLoadPreview}
              className="flex items-center space-x-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl shadow-md"
            >
              <span>Weiter zum Spalten-Mapping</span> <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 4: Column Mapping */}
      {step === 4 && tableData && (
        <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
            <Table className="w-5 h-5 text-indigo-400" /> Manuelles Spalten-Mapping
          </h3>
          <p className="text-xs text-slate-400">
            Zuordnung der Excel-Spalten zu den Feldern der Teilnehmer.
          </p>

          <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
            {tableData.headers.map((header) => (
              <div key={header} className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-center p-3 bg-slate-900/80 rounded-xl border border-slate-800">
                <div className="text-xs font-semibold text-slate-200 truncate">
                  Excel-Spalte: <span className="text-indigo-400 font-mono">{header}</span>
                </div>
                <select
                  value={columnMapping[header] || 'IGNORE'}
                  onChange={(e) => setColumnMapping({ ...columnMapping, [header]: e.target.value })}
                  className="px-3 py-1.5 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs"
                >
                  {availableAttributes.map((attr) => (
                    <option key={attr.value} value={attr.value}>{attr.label}</option>
                  ))}
                </select>
              </div>
            ))}
          </div>

          <div className="flex justify-end pt-2">
            <button
              onClick={handleValidateMapping}
              className="flex items-center space-x-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl shadow-md"
            >
              <span>Zuordnung prüfen & Validieren</span> <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 5: Validation Preview */}
      {step === 5 && validationResult && (
        <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
          <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-emerald-400" /> Import-Validierungsergebnis
          </h3>

          <div className="grid grid-cols-2 gap-4 text-center">
            <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl">
              <div className="text-2xl font-bold text-emerald-400">{validationResult.validPersons.length}</div>
              <div className="text-xs text-slate-400">Gültige Teilnehmer bereit</div>
            </div>
            <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl">
              <div className="text-2xl font-bold text-amber-400">{validationResult.warnings.length}</div>
              <div className="text-xs text-slate-400">Warnungen / Korrekturen</div>
            </div>
          </div>

          {validationResult.warnings.length > 0 && (
            <div className="space-y-1 max-h-40 overflow-y-auto p-3 bg-slate-900 rounded-xl border border-slate-800 text-xs">
              <span className="font-semibold text-amber-400 flex items-center gap-1">
                <AlertTriangle className="w-4 h-4" /> Warnungen:
              </span>
              {validationResult.warnings.map((w, i) => (
                <div key={i} className="text-slate-400">• {w}</div>
              ))}
            </div>
          )}

          <div className="flex justify-end space-x-2 pt-2">
            <button onClick={() => setStep(4)} className="px-4 py-2 bg-slate-800 text-slate-400 text-xs rounded-lg">
              Zurück
            </button>
            <button
              onClick={handleExecuteImport}
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-xl shadow-md"
            >
              {validationResult.validPersons.length} Teilnehmer importieren
            </button>
          </div>
        </div>
      )}

      {/* Step 6: Success */}
      {step === 6 && (
        <div className="glass-panel p-10 rounded-2xl border border-emerald-500/30 text-center space-y-4">
          <CheckCircle2 className="w-16 h-16 text-emerald-400 mx-auto" />
          <h3 className="text-xl font-bold text-slate-100">Import erfolgreich abgeschlossen!</h3>
          <p className="text-xs text-slate-400">
            Die Teilnehmer wurden erfolgreich in das Projekt übernommen und stehen für die Zuordnung bereit.
          </p>
          <button
            onClick={onComplete}
            className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs rounded-xl shadow-md"
          >
            Zur Teilnehmerübersicht
          </button>
        </div>
      )}
    </div>
  );
};
