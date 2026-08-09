import React, { useState } from 'react';
import type { Project, DynamicRule, RuleType, TargetScope, RuleAction, Condition } from '../types';
import { api } from '../services/api';
import { SlidersHorizontal, Plus, Trash2, Check, Edit3, Info, HelpCircle } from 'lucide-react';

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
  const [action] = useState<RuleAction>('ADD_POINTS');
  const [weight, setWeight] = useState<number>(10);
  const [conditions, setConditions] = useState<Condition[]>([]);

  const handleOpenAddModal = () => {
    setEditingRuleId(null);
    setName('');
    setDescription('');
    setRuleType('SOFT');
    setTargetScope('BED_PERSON');
    setWeight(10);
    setConditions([]);
    setShowAddModal(true);
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
        action: ruleType === 'HARD' ? 'FORBID' : action,
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
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <SlidersHorizontal className="w-7 h-7 text-indigo-400" /> Dynamic Rule Engine & Editor
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Erstellen, bearbeiten & verwalten Sie alle Zuordnungsregeln und Gewichtungen.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg shadow-md transition"
        >
          <Plus className="w-4 h-4" /> <span>Neue Regel formulieren</span>
        </button>
      </div>

      {/* Add / Edit Rule Modal */}
      {showAddModal && (
        <div className="glass-panel p-6 rounded-2xl border border-indigo-500/30 space-y-5 shadow-2xl">
          <h3 className="text-lg font-bold text-slate-100">
            {editingRuleId ? 'Bestehende Regel bearbeiten' : 'Neue Regel formulieren'}
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">Regel-Name *</label>
              <input
                type="text"
                placeholder="z.B. Senioren nicht nach oben"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1">Beschreibung</label>
              <input
                type="text"
                placeholder="Kurze Erklärung der Regel"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1">Regel-Typ</label>
              <select
                value={ruleType}
                onChange={(e: any) => setRuleType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm font-medium"
              >
                <option value="HARD">Harte Regel (Verbot / Restriktion)</option>
                <option value="SOFT">Weiche Regel (Wunsch/Punkte)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">Geltungsbereich (Scope)</label>
              <select
                value={targetScope}
                onChange={(e: any) => setTargetScope(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              >
                <option value="BED_PERSON">Person & Bett (z.B. Alter vs. Hochbett)</option>
                <option value="ROOM_PERSON">Person & Raum (z.B. Geschlecht vs. Mädchenzimmer)</option>
                <option value="PAIR_CO_LOCATION">Paare im selben Zimmer</option>
                <option value="GROUP_CO_LOCATION">Gruppen im selben Zimmer/Gebäude</option>
              </select>
            </div>

            {ruleType === 'SOFT' && (
              <div>
                <label className="block text-xs text-slate-400 mb-1">Gewichtung / Punkte (+/-)</label>
                <input
                  type="number"
                  value={weight}
                  onChange={(e) => setWeight(parseInt(e.target.value) || 0)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm font-mono"
                />
              </div>
            )}
          </div>

          {/* Conditions Section */}
          <div className="space-y-3 pt-2 border-t border-slate-800">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Bedingungen (WENN...)</span>
              <button
                onClick={handleAddCondition}
                className="text-xs px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-indigo-400 rounded-lg border border-slate-700"
              >
                + Bedingung hinzufügen
              </button>
            </div>

            {conditions.length === 0 ? (
              <p className="text-xs text-slate-500 italic">Keine Einzelbedingungen (gilt allgemein für den ausgewählten Scope).</p>
            ) : (
              conditions.map((cond, idx) => (
                <div key={idx} className="grid grid-cols-1 sm:grid-cols-4 gap-2 items-center p-2.5 bg-slate-900/80 rounded-xl border border-slate-800">
                  <select
                    value={cond.field}
                    onChange={(e) => handleUpdateCondition(idx, 'field', e.target.value)}
                    className="px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs"
                  >
                    <option value="person.age">Person: Alter</option>
                    <option value="person.gender">Person: Geschlecht</option>
                    <option value="bed.isTopBunk">Bett: Ist obere Etage</option>
                    <option value="room.girlsRoom">Raum: Ist Mädchenzimmer</option>
                    <option value="room.boysRoom">Raum: Ist Jungenzimmer</option>
                    <option value="room.floor">Raum: Etage</option>
                  </select>

                  <select
                    value={cond.operator}
                    onChange={(e: any) => handleUpdateCondition(idx, 'operator', e.target.value)}
                    className="px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                  >
                    <option value="EQUALS">=</option>
                    <option value="NOT_EQUALS">!=</option>
                    <option value="GREATER_THAN">&gt;</option>
                    <option value="LESS_THAN">&lt;</option>
                  </select>

                  <input
                    type="text"
                    placeholder="Wert"
                    value={cond.value}
                    onChange={(e) => handleUpdateCondition(idx, 'value', e.target.value)}
                    className="px-2 py-1.5 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs font-mono"
                  />

                  <div className="text-right">
                    <button onClick={() => handleRemoveCondition(idx)} className="text-slate-500 hover:text-rose-400 p-1">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="flex justify-end space-x-2 pt-3 border-t border-slate-800">
            <button onClick={() => setShowAddModal(false)} className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-xs rounded-lg">
              Abbrechen
            </button>
            <button onClick={handleSaveRule} className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded-lg">
              {editingRuleId ? 'Änderungen speichern' : 'Regel anlegen'}
            </button>
          </div>
        </div>
      )}

      {/* Rules List */}
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
                  {/* Info Button */}
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

                  {/* Edit Button */}
                  <button
                    onClick={() => handleOpenEditModal(r)}
                    title="Regel bearbeiten"
                    className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition"
                  >
                    <Edit3 className="w-4 h-4" />
                  </button>

                  {/* Delete Button */}
                  <button
                    onClick={() => handleDeleteRule(r.id)}
                    title="Regel löschen"
                    className="p-2 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Info Drawer Panel */}
              {isInfoOpen && (
                <div className="p-4 glass-card rounded-xl border border-indigo-500/30 text-xs text-slate-300 space-y-2 bg-slate-900/90 ml-6">
                  <div className="font-bold text-indigo-300 flex items-center gap-1.5">
                    <HelpCircle className="w-4 h-4" /> Funktionsweise & Auswertungsdetails:
                  </div>
                  <div>
                    <span className="text-slate-400">Regel-Typ: </span>
                    <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Harte Verbot-Restriktion' : 'Weiche Präferenz-Gewichtung'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Geltungsbereich (Scope): </span>
                    <span className="font-mono text-slate-200">{r.targetScope}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Auswirkung auf Score: </span>
                    <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Verbot (-999999 Pkt / Kombination ausgeschlossen)' : `+${r.weight} Punkte bei Erfüllung`}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Gegenwärtige Bedingungen: </span>
                    {r.conditions && r.conditions.length > 0 ? (
                      <ul className="list-disc list-inside mt-1 space-y-0.5 font-mono text-slate-200">
                        {r.conditions.map((c, i) => (
                          <li key={i}>WENN {c.field} {c.operator} "{String(c.value)}"</li>
                        ))}
                      </ul>
                    ) : (
                      <span className="italic text-slate-500">Keine spezifischen Zusatzbedingungen (gilt für den gesamten Scope)</span>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
