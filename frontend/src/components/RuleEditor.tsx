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
  const [targetScope, setTargetScope] = useState<TargetScope>('BED_PERSON');
  const [weight, setWeight] = useState<number>(50);
  const [conditions, setConditions] = useState<Condition[]>([]);

  const handleOpenAddModal = () => {
    setEditingRuleId(null);
    setName('');
    setDescription('');
    setRuleType('SOFT');
    setTargetScope('BED_PERSON');
    setWeight(50);
    setConditions([
      { field: 'person.age', operator: 'GREATER_THAN', value: 65 }
    ]);
    setShowAddModal(true);
  };

  const handleApplyPreset = (preset: 'COUPLES' | 'GROUPS' | 'SENIORS_BUNK') => {
    if (preset === 'COUPLES') {
      setName('Paare bevorzugt im selben Zimmer');
      setDescription('Personen mit der gleichen Paar-ID sollen bevorzugt im selben Zimmer untergebracht werden.');
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
            <SlidersHorizontal className="w-7 h-7 text-indigo-400" /> Regel-Verwaltung & WENN → DANN Formulierer
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Formuliere freie WENN-Bedingungen und lege fest, wie der Algorithmus Zimmer- und Bettenzuordnungen bewertet.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center space-x-1.5 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-xl shadow-lg shadow-indigo-500/20 transition"
        >
          <Plus className="w-4 h-4" /> <span>Neue Regel formulieren</span>
        </button>
      </div>

      {/* Add / Edit Rule Modal */}
      {showAddModal && (
        <div className="glass-panel p-6 rounded-2xl border border-indigo-500/40 space-y-6 shadow-2xl">
          <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
            <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-indigo-400" />
              {editingRuleId ? 'Regel bearbeiten' : 'Neue Regel formulieren'}
            </h3>
          </div>

          {/* Presets Bar */}
          {!editingRuleId && (
            <div className="p-4 bg-slate-900/90 rounded-xl border border-slate-800 space-y-2">
              <span className="text-xs font-semibold text-indigo-300 uppercase tracking-wider block">
                Schnell-Vorlagen (1-Klick Laden):
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

          {/* General Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-200 mb-1">1. Name der Regel *</label>
              <input
                type="text"
                placeholder="z.B. Senioren nicht ins obere Hochbett"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-200 mb-1">2. Kurze Beschreibung</label>
              <input
                type="text"
                placeholder="Erklärung oder Notiz"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:border-indigo-500"
              />
            </div>
          </div>

          {/* PROMINENT ALWAYS-VISIBLE WENN... SECTION */}
          <div className="p-4 bg-indigo-950/30 border border-indigo-500/30 rounded-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-sm font-bold text-indigo-300 uppercase tracking-wider flex items-center gap-1.5">
                  🔍 WENN... (Welche Bedingungen müssen zutreffen?)
                </h4>
                <p className="text-xs text-slate-400 mt-0.5">
                  Füge hier Bedingungen hinzu (z. B. <i>Alter &gt; 65</i>, <i>Bett = Oberes Hochbett</i>, <i>Geschlecht = weiblich</i>).
                </p>
              </div>

              <button
                type="button"
                onClick={handleAddCondition}
                className="flex items-center space-x-1 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg transition shadow"
              >
                <Plus className="w-4 h-4" /> <span>WENN-Bedingung hinzufügen</span>
              </button>
            </div>

            {conditions.length === 0 ? (
              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 text-xs text-slate-400 italic">
                Keine spezifischen WENN-Bedingungen eingetragen (gilt allgemein). Klicke oben auf "+ WENN-Bedingung hinzufügen".
              </div>
            ) : (
              <div className="space-y-2">
                {conditions.map((cond, idx) => (
                  <div key={idx} className="grid grid-cols-1 sm:grid-cols-4 gap-2 items-center p-3 bg-slate-900 rounded-xl border border-slate-800 shadow-sm">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-400 mb-0.5">Eigenschaft (Feld)</label>
                      <select
                        value={cond.field}
                        onChange={(e) => handleUpdateCondition(idx, 'field', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-medium"
                      >
                        <option value="person.age">👤 Person: Alter</option>
                        <option value="person.gender">👤 Person: Geschlecht</option>
                        <option value="person.partnerId">👫 Person: Paar-ID</option>
                        <option value="person.groupId">👥 Person: Gruppe / Klasse</option>
                        <option value="person.specialNeeds">⚠️ Person: Sonderwunsch / Anmerkung</option>
                        <option value="bed.isTopBunk">🛏️ Bett: Ist oberes Hochbett</option>
                        <option value="bed.bedType">🛏️ Bett: Bett-Art (Einzelbett, Hochbett...)</option>
                        <option value="room.floor">🚪 Raum: Etage</option>
                        <option value="room.girlsRoom">🚪 Raum: Mädchenzimmer</option>
                        <option value="room.boysRoom">🚪 Raum: Jungenzimmer</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[10px] font-bold text-slate-400 mb-0.5">Vergleich</label>
                      <select
                        value={cond.operator}
                        onChange={(e: any) => handleUpdateCondition(idx, 'operator', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                      >
                        <option value="GREATER_THAN">ist größer als (&gt;)</option>
                        <option value="LESS_THAN">ist kleiner als (&lt;)</option>
                        <option value="EQUALS">ist genau gleich (=)</option>
                        <option value="NOT_EQUALS">ist ungleich (!=)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[10px] font-bold text-slate-400 mb-0.5">Wert</label>
                      <input
                        type="text"
                        placeholder="z.B. 65, maennlich, true"
                        value={cond.value}
                        onChange={(e) => handleUpdateCondition(idx, 'value', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                      />
                    </div>

                    <div className="text-right pt-2 sm:pt-0">
                      <button
                        type="button"
                        onClick={() => handleRemoveCondition(idx)}
                        className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded transition"
                        title="Bedingung löschen"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* DANN... ACTION SECTION */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
            <div>
              <label className="block text-xs font-bold text-slate-200 mb-1">⚡ DANN... (Was soll der Algorithmus tun?)</label>
              <select
                value={ruleType}
                onChange={(e: any) => setRuleType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs font-bold"
              >
                <option value="SOFT">DANN → Mit Pluspunkten belohnen (Weiche Regel)</option>
                <option value="HARD">DANN → Strikt verbieten (Harte Regel / Restriktion)</option>
              </select>
            </div>

            {ruleType === 'SOFT' ? (
              <div>
                <label className="block text-xs font-bold text-slate-200 mb-1">Bonus-Punkte (+Score)</label>
                <input
                  type="number"
                  value={weight}
                  onChange={(e) => setWeight(parseInt(e.target.value) || 0)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm font-mono"
                />
              </div>
            ) : (
              <div>
                <label className="block text-xs font-bold text-slate-200 mb-1">Scope / Zuordnungstyp</label>
                <select
                  value={targetScope}
                  onChange={(e: any) => setTargetScope(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs"
                >
                  <option value="BED_PERSON">Person ↔ Bett (z. B. Alter vs. Hochbett)</option>
                  <option value="ROOM_PERSON">Person ↔ Raum (z. B. Geschlecht vs. Raum)</option>
                  <option value="PAIR_CO_LOCATION">Paare im selben Zimmer</option>
                  <option value="GROUP_CO_LOCATION">Gruppen im selben Zimmer</option>
                </select>
              </div>
            )}
          </div>

          {/* Live Rule Logic Preview Box */}
          <div className="p-4 bg-indigo-950/50 border border-indigo-500/30 rounded-xl space-y-1">
            <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-300 flex items-center gap-1">
              <ArrowRight className="w-3.5 h-3.5" /> Zusammenfassung der Regel:
            </span>
            <div className="text-xs text-slate-200 font-mono">
              {ruleType === 'HARD' ? (
                <span className="text-rose-300">
                  WENN die angegebenen Bedingungen erfüllt sind, DANN schließt der Algorithmus diese Unterbringung <strong>STRIKT AUS (Verbot)</strong>.
                </span>
              ) : (
                <span className="text-emerald-300">
                  WENN die angegebenen Bedingungen erfüllt sind, DANN belohnt der Algorithmus die Zuordnung mit <strong>+{weight} Pluspunkten</strong>.
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
