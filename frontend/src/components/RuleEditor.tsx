import React, { useState } from 'react';
import type { Project, DynamicRule, RuleType, TargetScope, Condition } from '../types';
import { api } from '../services/api';
import { SlidersHorizontal, Plus, Trash2, Check, Edit3, Info, HelpCircle, Sparkles, ArrowRight } from 'lucide-react';

interface RuleEditorProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

export const RuleEditor: React.FC<RuleEditorProps> = ({ project, onProjectUpdate }) => {
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [infoRuleId, setInfoRuleId] = useState<string | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [ruleType, setRuleType] = useState<RuleType>('SOFT');
  const [targetScope, setTargetScope] = useState<TargetScope>('PAIR_CO_LOCATION');
  const [weight, setWeight] = useState<number>(50);
  const [conditions, setConditions] = useState<Condition[]>([]);

  const handleOpenAddModal = () => {
    setEditingRuleId(null);
    setName('');
    setDescription('');
    setRuleType('SOFT');
    setTargetScope('PAIR_CO_LOCATION');
    setWeight(50);
    setConditions([]);
    setShowAddModal(true);
  };

  const handleApplyPreset = (preset: 'COUPLES' | 'GROUPS' | 'SENIORS_BUNK') => {
    if (preset === 'COUPLES') {
      setName('Paare bevorzugt im selben Zimmer');
      setDescription('Personen mit der gleichen Paar-ID sollen im selben Zimmer untergebracht werden.');
      setRuleType('SOFT');
      setTargetScope('PAIR_CO_LOCATION');
      setWeight(50);
      setConditions([]);
    } else if (preset === 'GROUPS') {
      setName('Gruppenzusammenhalt im Zimmer');
      setDescription('Personen der gleichen Gruppe/Klasse bevorzugt im selben Zimmer unterbringen.');
      setRuleType('SOFT');
      setTargetScope('GROUP_CO_LOCATION');
      setWeight(20);
      setConditions([]);
    } else if (preset === 'SENIORS_BUNK') {
      setName('Senioren (>65 Jahre) nicht ins obere Hochbett');
      setDescription('Aus Sicherheitsgründen keine Personen über 65 Jahre im oberen Hochbett unterbringen.');
      setRuleType('HARD');
      setTargetScope('BED_PERSON');
      setWeight(-999999);
      setConditions([
        { field: 'person.age', operator: 'GREATER_THAN', value: 65 },
        { field: 'bed.isTopBunk', operator: 'EQUALS', value: true },
      ]);
    }
  };

  const handleOpenEditModal = (rule: DynamicRule) => {
    setEditingRuleId(rule.id);
    setName(rule.name);
    setDescription(rule.description || '');
    setRuleType(rule.ruleType);
    setTargetScope(rule.targetScope);
    setWeight(rule.weight);
    setConditions(rule.conditions || []);
    setShowAddModal(true);
  };

  const handleAddCondition = () => {
    setConditions([...conditions, { field: 'person.age', operator: 'GREATER_THAN', value: 65 }]);
  };

  const handleRemoveCondition = (index: number) => {
    setConditions(conditions.filter((_, idx) => idx !== index));
  };

  const handleUpdateCondition = (index: number, key: keyof Condition, val: any) => {
    const next = [...conditions];
    next[index] = { ...next[index], [key]: val };
    setConditions(next);
  };

  const handleSaveRule = async () => {
    if (!name.trim()) return;
    try {
      const rulePayload: DynamicRule = {
        id: editingRuleId || '',
        name: name.trim(),
        description: description.trim(),
        active: true,
        ruleType,
        targetScope,
        action: ruleType === 'HARD' ? 'FORBID' : 'ADD_POINTS',
        weight: ruleType === 'HARD' ? -999999 : weight,
        conditions,
      };

      let updated: Project;
      if (editingRuleId) {
        updated = await api.updateRule(project.id, editingRuleId, rulePayload);
      } else {
        updated = await api.addRule(project.id, rulePayload);
      }

      onProjectUpdate(updated);
      setShowAddModal(false);
    } catch (e) {
      alert('Fehler beim Speichern der Regel.');
    }
  };

  const handleDeleteRule = async (ruleId: string) => {
    if (!confirm('Möchten Sie diese Regel wirklich löschen?')) return;
    try {
      const updated = await api.deleteRule(project.id, ruleId);
      onProjectUpdate(updated);
    } catch (e) {
      alert('Fehler beim Löschen der Regel.');
    }
  };

  const handleToggleRuleActive = async (rule: DynamicRule) => {
    try {
      const updatedRule = { ...rule, active: !rule.active };
      const updated = await api.updateRule(project.id, rule.id, updatedRule);
      onProjectUpdate(updated);
    } catch (e) {
      alert('Fehler beim Aktualisieren der Regel.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <SlidersHorizontal className="w-7 h-7 text-indigo-400" /> Regel-Verwaltung & Optimierung
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Formuliere WENN → DANN Regeln für automatische Zimmer- und Bettenzuordnungen.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center space-x-1.5 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-xl shadow-lg shadow-indigo-500/20 transition"
        >
          <Plus className="w-4 h-4" /> <span>Neue Regel anlegen</span>
        </button>
      </div>

      {/* Add / Edit Rule Modal */}
      {showAddModal && (
        <div className="glass-panel p-6 rounded-2xl border border-indigo-500/40 space-y-6 shadow-2xl">
          <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
            <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-indigo-400" />
              {editingRuleId ? 'Regel bearbeiten' : 'Neue Regel im WENN → DANN Format formulieren'}
            </h3>
          </div>

          {/* Presets Bar */}
          {!editingRuleId && (
            <div className="p-4 bg-slate-900/90 rounded-xl border border-slate-800 space-y-2">
              <span className="text-xs font-semibold text-indigo-300 uppercase tracking-wider block">
                Schnell-Vorlagen (1-Klick Vorlage laden):
              </span>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => handleApplyPreset('COUPLES')}
                  className="px-3 py-1.5 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-200 border border-indigo-500/30 rounded-lg text-xs font-medium transition"
                >
                  👫 Paare zusammen (+50 Pkt)
                </button>
                <button
                  type="button"
                  onClick={() => handleApplyPreset('GROUPS')}
                  className="px-3 py-1.5 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-200 border border-indigo-500/30 rounded-lg text-xs font-medium transition"
                >
                  👥 Gruppe/Klasse zusammen (+20 Pkt)
                </button>
                <button
                  type="button"
                  onClick={() => handleApplyPreset('SENIORS_BUNK')}
                  className="px-3 py-1.5 bg-rose-500/20 hover:bg-rose-500/40 text-rose-200 border border-rose-500/30 rounded-lg text-xs font-medium transition"
                >
                  🚫 Senioren nicht oben (Verbot)
                </button>
              </div>
            </div>
          )}

          {/* Form Fields */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">1. Regel-Bezeichnung *</label>
              <input
                type="text"
                placeholder="z.B. Paare im selben Zimmer bevorzugen"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">2. Kurze Beschreibung</label>
              <input
                type="text"
                placeholder="Erklärung für das Team"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">3. Worauf bezieht sich die Regel?</label>
              <select
                value={targetScope}
                onChange={(e: any) => setTargetScope(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
              >
                <option value="PAIR_CO_LOCATION">👫 Paare (Personen mit gleicher Paar-ID im selben Zimmer)</option>
                <option value="GROUP_CO_LOCATION">👥 Gruppen (Personen derselben Gruppe im selben Zimmer)</option>
                <option value="BED_PERSON">👤 Einzelperson & Bett (z. B. Alter vs. Hochbett)</option>
                <option value="ROOM_PERSON">🚪 Einzelperson & Raum (z. B. Geschlecht vs. Raumtyp)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">4. Was soll passieren? (DANN...)</label>
              <select
                value={ruleType}
                onChange={(e: any) => setRuleType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs font-bold"
              >
                <option value="SOFT">DANN → Belohnung (Weiche Regel / Pluspunkte)</option>
                <option value="HARD">DANN → Striktes Verbot (Harte Regel / Ausgeschlossen)</option>
              </select>
            </div>

            {ruleType === 'SOFT' && (
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Wunsch-Stärke (Bonus-Punkte)</label>
                <input
                  type="number"
                  value={weight}
                  onChange={(e) => setWeight(parseInt(e.target.value) || 0)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm font-mono"
                />
              </div>
            )}
          </div>

          {/* Conditional Logic Builder (WENN...) */}
          {(targetScope === 'BED_PERSON' || targetScope === 'ROOM_PERSON') && (
            <div className="space-y-3 pt-3 border-t border-slate-800">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
                  WENN... (Zusatzbedingungen definieren)
                </span>
                <button
                  type="button"
                  onClick={handleAddCondition}
                  className="text-xs px-3 py-1 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 rounded-lg border border-indigo-500/30"
                >
                  + Bedingung hinzufügen
                </button>
              </div>

              {conditions.length === 0 ? (
                <p className="text-xs text-slate-500 italic p-3 bg-slate-900/50 rounded-xl border border-slate-800">
                  Keine spezifischen Filter-Bedingungen. Gilt für alle Zuordnungen in diesem Bereich.
                </p>
              ) : (
                conditions.map((cond, idx) => (
                  <div key={idx} className="grid grid-cols-1 sm:grid-cols-4 gap-2 items-center p-3 bg-slate-900 rounded-xl border border-slate-800">
                    <div>
                      <label className="block text-[10px] text-slate-500 mb-0.5">Eigenschaft</label>
                      <select
                        value={cond.field}
                        onChange={(e) => handleUpdateCondition(idx, 'field', e.target.value)}
                        className="w-full px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs"
                      >
                        <option value="person.age">Person: Alter</option>
                        <option value="person.gender">Person: Geschlecht</option>
                        <option value="bed.isTopBunk">Bett: Ist obere Etage</option>
                        <option value="room.floor">Raum: Etage</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[10px] text-slate-500 mb-0.5">Vergleich</label>
                      <select
                        value={cond.operator}
                        onChange={(e: any) => handleUpdateCondition(idx, 'operator', e.target.value)}
                        className="w-full px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                      >
                        <option value="GREATER_THAN">größer als (&gt;)</option>
                        <option value="EQUALS">ist gleich (=)</option>
                        <option value="NOT_EQUALS">ist ungleich (!=)</option>
                        <option value="LESS_THAN">kleiner als (&lt;)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[10px] text-slate-500 mb-0.5">Wert</label>
                      <input
                        type="text"
                        placeholder="Wert (z.B. 65)"
                        value={cond.value}
                        onChange={(e) => handleUpdateCondition(idx, 'value', e.target.value)}
                        className="w-full px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                      />
                    </div>

                    <div className="text-right pt-3 sm:pt-0">
                      <button type="button" onClick={() => handleRemoveCondition(idx)} className="text-slate-500 hover:text-rose-400 p-1">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Live Sentence Preview Box */}
          <div className="p-4 bg-indigo-950/40 border border-indigo-500/30 rounded-xl space-y-1">
            <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-300 flex items-center gap-1">
              <ArrowRight className="w-3.5 h-3.5" /> Vorschau der Regel-Logik:
            </span>
            <div className="text-xs text-slate-200 font-mono">
              {ruleType === 'HARD' ? (
                <span className="text-rose-300">
                  WENN die Bedingungen zutreffen, DANN ist die Unterbringung <strong>STRIKT VERBOTEN</strong>.
                </span>
              ) : (
                <span className="text-emerald-300">
                  WENN die Zuordnung zutrifft, DANN belohnt der Algorithmus den Plan mit <strong>+{weight} Pluspunkten</strong>.
                </span>
              )}
            </div>
          </div>

          <div className="flex justify-end space-x-2 pt-3 border-t border-slate-800">
            <button type="button" onClick={() => setShowAddModal(false)} className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-xs rounded-lg">
              Abbrechen
            </button>
            <button type="button" onClick={handleSaveRule} className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg">
              {editingRuleId ? 'Änderungen speichern' : 'Regel aktivieren'}
            </button>
          </div>
        </div>
      )}

      {/* Rules List Display */}
      <div className="space-y-3">
        {project.rules.map((r) => {
          const isHard = r.ruleType === 'HARD';
          const isInfoOpen = infoRuleId === r.id;
          return (
            <div key={r.id} className="space-y-2">
              <div
                className={`glass-panel p-4 rounded-2xl border transition flex items-center justify-between ${
                  r.active ? (isHard ? 'border-rose-500/30' : 'border-indigo-500/30') : 'border-slate-800 opacity-60'
                }`}
              >
                <div className="flex items-center space-x-4">
                  <button
                    onClick={() => handleToggleRuleActive(r)}
                    className={`w-6 h-6 rounded-lg flex items-center justify-center transition ${
                      r.active ? 'bg-indigo-600 text-white' : 'bg-slate-800 text-slate-600 border border-slate-700'
                    }`}
                  >
                    {r.active && <Check className="w-4 h-4" />}
                  </button>

                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-semibold text-slate-100 text-sm">{r.name}</span>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                          isHard ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                        }`}
                      >
                        {isHard ? 'Harte Regel (Verbot)' : `Weiche Regel (+${r.weight} Pkt)`}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">{r.description || 'Keine Beschreibung'}</p>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setInfoRuleId(isInfoOpen ? null : r.id)}
                    className={`flex items-center space-x-1 px-2.5 py-1 rounded-lg text-xs font-medium transition ${
                      isInfoOpen
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-800 text-indigo-400 hover:bg-slate-700 border border-slate-700'
                    }`}
                  >
                    <Info className="w-3.5 h-3.5" />
                    <span>Mehr Infos</span>
                  </button>

                  <button
                    onClick={() => handleOpenEditModal(r)}
                    title="Regel bearbeiten"
                    className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition"
                  >
                    <Edit3 className="w-4 h-4" />
                  </button>

                  <button
                    onClick={() => handleDeleteRule(r.id)}
                    title="Regel löschen"
                    className="p-2 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Info Drawer */}
              {isInfoOpen && (
                <div className="p-4 glass-card rounded-xl border border-indigo-500/30 text-xs text-slate-300 space-y-2 bg-slate-900/90 ml-6">
                  <div className="font-bold text-indigo-300 flex items-center gap-1.5">
                    <HelpCircle className="w-4 h-4" /> WENN → DANN Funktionsweise:
                  </div>
                  <div>
                    <span className="text-slate-400">Regel-Typ: </span>
                    <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Harte Verbot-Restriktion' : 'Weiche Präferenz (+Punkte)'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Geltungsbereich (Scope): </span>
                    <span className="font-mono text-slate-200">{r.targetScope}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Auswirkung: </span>
                    <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Ausgeschlossen (-999999 Pkt)' : `+${r.weight} Pluspunkte bei Erfüllung`}</span>
                  </div>
                  {r.conditions && r.conditions.length > 0 && (
                    <div>
                      <span className="text-slate-400">Aktive WENN-Bedingungen: </span>
                      <ul className="list-disc list-inside mt-1 space-y-0.5 font-mono text-slate-200">
                        {r.conditions.map((c, i) => (
                          <li key={i}>WENN {c.field} {c.operator} "{String(c.value)}"</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
