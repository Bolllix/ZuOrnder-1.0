import React, { useState } from 'react';
import type { Project, TableData, ImportValidationResult } from '../types';
import { api } from '../services/api';
import { FileSpreadsheet, Upload, CheckCircle2, AlertTriangle } from 'lucide-react';

interface ImportWizardProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
  onComplete: () => void;
}

export const ImportWizard: React.FC<ImportWizardProps> = ({ project, onProjectUpdate, onComplete }) => {
  const [step, setStep] = useState<number>(1);
  const [file, setFile] = useState<File | null>(null);
  const [sheets, setSheets] = useState<string[]>([]);
  const [selectedSheet, setSelectedSheet] = useState<string>('');
  const [headerRowIndex, setHeaderRowIndex] = useState<number>(0);
  const [previewData, setPreviewData] = useState<TableData | null>(null);

  // Column Mapping: Header Name -> Attribute Name
  const [columnMapping, setColumnMapping] = useState<Record<string, string>>({});
  const [validationResult, setValidationResult] = useState<ImportValidationResult | null>(null);

  const availableAttributes = [
    { value: 'IGNORE', label: '--- Ignorieren ---' },
    { value: 'lastname', label: 'Nachname' },
    { value: 'firstname', label: 'Vorname' },
    { value: 'name', label: 'Vollständiger Name' },
    { value: 'gender', label: 'Geschlecht' },
    { value: 'age', label: 'Alter' },
    { value: 'partnerid', label: 'Paar-ID' },
    { value: 'groupid', label: 'Gruppe / Klassenname' },
    { value: 'desiredfloor', label: 'Wunsch-Etage' },
    { value: 'desiredroom', label: 'Wunsch-Zimmer' },
    { value: 'specialneeds', label: 'Besondere Anforderungen' },
  ];

  // Step 1: File Upload & fetch sheets
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);
      try {
        const sheetList = await api.getSheets(selectedFile);
        setSheets(sheetList);
        setSelectedSheet(sheetList[0] || '');
        setStep(2);
      } catch (err) {
        alert('Fehler beim Auslesen der Datei.');
      }
    }
  };

  // Step 2 & 3: Load Preview
  const handleLoadPreview = async () => {
    if (!file) return;
    try {
      const preview = await api.getPreview(file, selectedSheet, headerRowIndex, 10);
      setPreviewData(preview);

      // Auto-suggest mappings based on header text
      const initialMap: Record<string, string> = {};
      preview.headers.forEach((h) => {
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

  // Step 4: Validate Mapping
  const handleValidateMapping = async () => {
    if (!file) return;
    try {
      const result = await api.mapColumns(file, columnMapping, selectedSheet, headerRowIndex);
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
      const updated = await api.importToProject(project.id, validationResult.validPersons);
      onProjectUpdate(updated);
      setStep(6);
    } catch (err) {
      alert('Fehler beim Importieren der Teilnehmer.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Wizard Step Indicator */}
      <div className="glass-panel p-4 rounded-2xl border border-slate-800 flex items-center justify-between">
        {[
          { num: 1, label: 'Upload' },
          { num: 2, label: 'Blatt' },
          { num: 3, label: 'Vorschau' },
          { num: 4, label: 'Spalten zuordnen' },
          { num: 5, label: 'Validierung' },
          { num: 6, label: 'Fertig' },
        ].map((s) => (
          <div key={s.num} className="flex items-center space-x-2">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs ${
                step === s.num
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30'
                  : step > s.num
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  : 'bg-slate-800 text-slate-500'
              }`}
            >
              {step > s.num ? '✓' : s.num}
            </div>
            <span className={`text-xs font-medium hidden sm:inline ${step === s.num ? 'text-indigo-400' : 'text-slate-500'}`}>
              {s.label}
            </span>
          </div>
        ))}
      </div>

      {/* Step 1: File Upload */}
      {step === 1 && (
        <div className="glass-panel p-10 rounded-2xl text-center space-y-4 border-2 border-dashed border-slate-700">
          <FileSpreadsheet className="w-16 h-16 text-indigo-400 mx-auto animate-pulse" />
          <div>
            <h3 className="text-lg font-bold text-slate-100">Excel oder CSV Tabelle hochladen</h3>
            <p className="text-slate-400 text-sm mt-1">Unterstützt .xlsx, .xls und .csv Dateien</p>
          </div>

          <label className="inline-flex items-center space-x-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm rounded-xl cursor-pointer shadow-lg shadow-indigo-500/20 transition">
            <Upload className="w-4 h-4" />
            <span>Datei auswählen</span>
            <input type="file" accept=".xlsx,.xls,.csv" onChange={handleFileChange} className="hidden" />
          </label>
        </div>
      )}

      {/* Step 2: Sheet & Header selection */}
      {step === 2 && (
        <div className="glass-panel p-6 rounded-2xl space-y-6">
          <h3 className="text-lg font-bold text-slate-100">Schritt 2: Tabellenblatt & Überschriftenzeile auswählen</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Tabellenblatt</label>
              <select
                value={selectedSheet}
                onChange={(e) => setSelectedSheet(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              >
                {sheets.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Zeile mit Spaltenüberschriften</label>
              <input
                type="number"
                value={headerRowIndex + 1}
                onChange={(e) => setHeaderRowIndex(Math.max(0, parseInt(e.target.value) - 1 || 0))}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              />
              <span className="text-[11px] text-slate-500">Normalerweise Zeile 1</span>
            </div>
          </div>

          <div className="flex justify-between pt-4 border-t border-slate-800">
            <button onClick={() => setStep(1)} className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-sm rounded-lg">
              Zurück
            </button>
            <button onClick={handleLoadPreview} className="px-6 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg">
              Weiter zur Vorschau →
            </button>
          </div>
        </div>
      )}

      {/* Step 4: Column Mapping */}
      {step === 4 && previewData && (
        <div className="glass-panel p-6 rounded-2xl space-y-6">
          <div>
            <h3 className="text-lg font-bold text-slate-100">Schritt 4: Spalten manuell zuordnen</h3>
            <p className="text-slate-400 text-sm">Ordnen Sie jede Spalte Ihrer Tabelle der passenden Eigenschaft zu.</p>
          </div>

          <div className="space-y-3">
            {previewData.headers.map((header) => (
              <div key={header} className="grid grid-cols-1 md:grid-cols-3 gap-3 p-3 bg-slate-900/60 rounded-xl border border-slate-800 items-center">
                <div className="font-semibold text-sm text-slate-200 truncate">
                  {header}
                </div>
                <div>
                  <select
                    value={columnMapping[header] || 'IGNORE'}
                    onChange={(e) => setColumnMapping({ ...columnMapping, [header]: e.target.value })}
                    className="w-full px-3 py-1.5 bg-slate-800 border border-indigo-500/40 rounded-lg text-slate-200 text-xs font-medium"
                  >
                    {availableAttributes.map((attr) => (
                      <option key={attr.value} value={attr.value}>{attr.label}</option>
                    ))}
                  </select>
                </div>
                <div className="text-xs text-slate-500 truncate">
                  Beispiel: {previewData.rows[0]?.[previewData.headers.indexOf(header)] || '-'}
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-between pt-4 border-t border-slate-800">
            <button onClick={() => setStep(2)} className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-sm rounded-lg">
              Zurück
            </button>
            <button onClick={handleValidateMapping} className="px-6 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg">
              Validieren & Überprüfen →
            </button>
          </div>
        </div>
      )}

      {/* Step 5: Validation Result */}
      {step === 5 && validationResult && (
        <div className="glass-panel p-6 rounded-2xl space-y-6">
          <h3 className="text-lg font-bold text-slate-100">Schritt 5: Import-Validierung</h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl">
              <div className="text-2xl font-bold text-emerald-400">{validationResult.validPersons.length}</div>
              <div className="text-xs text-slate-400">Gültige Teilnehmer</div>
            </div>
            <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl">
              <div className="text-2xl font-bold text-amber-400">{validationResult.warnings.length}</div>
              <div className="text-xs text-slate-400">Hinweise / Warnungen</div>
            </div>
            <div className="p-4 bg-slate-800 border border-slate-700 rounded-xl">
              <div className="text-2xl font-bold text-slate-200">{validationResult.totalRowsProcessed}</div>
              <div className="text-xs text-slate-400">Zeilen verarbeitet</div>
            </div>
          </div>

          {validationResult.warnings.length > 0 && (
            <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl space-y-1 max-h-40 overflow-y-auto">
              <div className="text-xs font-bold text-amber-300 flex items-center gap-1">
                <AlertTriangle className="w-4 h-4" /> Hinweise zum Import:
              </div>
              {validationResult.warnings.map((w, idx) => (
                <div key={idx} className="text-xs text-amber-200/80">• {w}</div>
              ))}
            </div>
          )}

          <div className="flex justify-between pt-4 border-t border-slate-800">
            <button onClick={() => setStep(4)} className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-sm rounded-lg">
              Zurück
            </button>
            <button
              onClick={handleExecuteImport}
              className="px-6 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-lg shadow-lg shadow-emerald-500/20"
            >
              {validationResult.validPersons.length} Teilnehmer importieren ✓
            </button>
          </div>
        </div>
      )}

      {/* Step 6: Confirmation */}
      {step === 6 && (
        <div className="glass-panel p-10 rounded-2xl text-center space-y-4">
          <CheckCircle2 className="w-16 h-16 text-emerald-400 mx-auto" />
          <h3 className="text-xl font-bold text-slate-100">Import erfolgreich abgeschlossen!</h3>
          <p className="text-slate-400 text-sm">Die Teilnehmer wurden in Ihr Projekt übernommen.</p>
          <button
            onClick={onComplete}
            className="px-6 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl shadow-md"
          >
            Zurück zur Übersicht
          </button>
        </div>
      )}
    </div>
  );
};
