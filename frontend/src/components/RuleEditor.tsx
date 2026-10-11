import React, { useState } from 'react';
import type { Project, DynamicRule, RuleType, TargetScope, Condition } from '../types';
import { api } from '../services/api';
import {
  SlidersHorizontal,
  Plus,
  Trash2,
  Check,
  Info,
  HelpCircle,
  Sparkles,
  ArrowRight,
  ShieldAlert,
  Users,
  Heart,
  UserCheck,
  Bed,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  X
} from 'lucide-react';

interface RuleEditorProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

export const RuleEditor: React.FC<RuleEditorProps> = ({ project, onProjectUpdate }) => {
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [infoRuleId, setInfoRuleId] = useState<string | null>(null);

  // Form State für benutzerdefinierte Regeln (WER -> WO -> BEWERTUNG)
  const [ruleName, setRuleName] = useState('');
  const [ruleDescription, setRuleDescription] = useState('');

  // 1. WER? (Personen-Filter)
  const [whoType, setWhoType] = useState<'AGE' | 'GROUP' | 'GENDER' | 'SPECIAL' | 'ALL'>('AGE');
  const [whoAgeOp, setWhoAgeOp] = useState<'GREATER_THAN' | 'LESS_THAN' | 'EQUALS'>('GREATER_THAN');
  const [whoAgeVal, setWhoAgeVal] = useState<number>(65);
  const [whoGroupVal, setWhoGroupVal] = useState<string>('');
  const [whoGenderVal, setWhoGenderVal] = useState<'maennlich' | 'weiblich' | 'divers'>('weiblich');
  const [whoSpecialVal, setWhoSpecialVal] = useState<string>('');

  // 2. WO? (Bett / Zimmer / Etage)
  const [whereType, setWhereType] = useState<'TOP_BUNK' | 'BOTTOM_BUNK' | 'DOUBLE_BED' | 'GROUND_FLOOR' | 'SPECIFIC_FLOOR' | 'SAME_ROOM'>('TOP_BUNK');
  const [whereFloorVal, setWhereFloorVal] = useState<number>(0);

  // 3. WIE BEWERTEN? (Gewichtung)
  const [ratingType, setRatingType] = useState<'FORBID' | 'AVOID_STRONG' | 'AVOID' | 'VERY_HIGH' | 'HIGH' | 'LOW' | 'CUSTOM'>('FORBID');
  const [customWeight, setCustomWeight] = useState<number>(25);

  // Standard-Karten Helper
  const findStandardRule = (type: 'GENDER' | 'COUPLES' | 'GROUPS' | 'SENIORS') => {
    if (!project.rules) return undefined;
    if (type === 'GENDER') {
      return project.rules.find(r => r.name.toLowerCase().includes('geschlecht') || r.description.toLowerCase().includes('geschlecht'));
    }
    if (type === 'COUPLES') {
      return project.rules.find(r => r.targetScope === 'PAIR_CO_LOCATION' || r.name.toLowerCase().includes('paar'));
    }
    if (type === 'GROUPS') {
      return project.rules.find(r => r.targetScope === 'GROUP_CO_LOCATION' || r.name.toLowerCase().includes('gruppe'));
    }
    if (type === 'SENIORS') {
      return project.rules.find(r => r.name.toLowerCase().includes('senior') || r.name.toLowerCase().includes('hochbett'));
    }
    return undefined;
  };

  const handleToggleStandardRule = async (type: 'GENDER' | 'COUPLES' | 'GROUPS' | 'SENIORS') => {
    const existing = findStandardRule(type);
    if (existing) {
      // Toggle existierend
      try {
        const updatedRule = { ...existing, active: !existing.active };
        const updatedProject = await api.updateRule(project.id, existing.id, updatedRule);
        onProjectUpdate(updatedProject);
      } catch (e) {
        alert('Fehler beim Aktualisieren der Regel.');
      }
    } else {
      // Neu anlegen
      let newRulePayload: DynamicRule;
      if (type === 'GENDER') {
        newRulePayload = {
          id: '',
          name: 'Geschlechtertrennung im Zimmer',
          description: 'Männer und Frauen dürfen nicht im selben Zimmer schlafen (getrennte Zimmer). Wenn inaktiv, sind gemischte Zimmer für alle erlaubt.',
          active: true,
          ruleType: 'HARD',
          targetScope: 'ROOM_PERSON',
          action: 'FORBID',
          weight: -999999,
          conditions: []
        };
      } else if (type === 'COUPLES') {
        newRulePayload = {
          id: '',
          name: 'Paare im selben Zimmer',
          description: 'Angemeldete Paare mit gleicher Paar-ID werden bevorzugt im selben Zimmer / Doppelbett untergebracht (funktioniert mit und ohne Geschlechtertrennung).',
          active: true,
          ruleType: 'SOFT',
          targetScope: 'PAIR_CO_LOCATION',
          action: 'ADD_POINTS',
          weight: 50,
          conditions: []
        };
      } else if (type === 'GROUPS') {
        newRulePayload = {
          id: '',
          name: 'Gruppenzusammenhalt im Zimmer',
          description: 'Personen der gleichen Gruppe/Klasse bevorzugt im selben Zimmer unterbringen (+20 Pkt).',
          active: true,
          ruleType: 'SOFT',
          targetScope: 'GROUP_CO_LOCATION',
          action: 'ADD_POINTS',
          weight: 20,
          conditions: []
        };
      } else {
        newRulePayload = {
          id: '',
          name: 'Senioren nicht im oberen Hochbett',
          description: 'Personen ab 65 Jahren dürfen aus Sicherheitsgründen nicht im oberen Hochbett schlafen.',
          active: true,
          ruleType: 'HARD',
          targetScope: 'BED_PERSON',
          action: 'FORBID',
          weight: -999999,
          conditions: [
            { field: 'person.age', operator: 'GREATER_THAN', value: 65 },
            { field: 'bed.isTopBunk', operator: 'EQUALS', value: true }
          ]
        };
      }

      try {
        const updatedProject = await api.addRule(project.id, newRulePayload);
        onProjectUpdate(updatedProject);
      } catch (e) {
        alert('Fehler beim Erstellen der Standard-Regel.');
      }
    }
  };

  // Öffnet das Modal für benutzerdefinierte Regeln
  const handleOpenAddModal = () => {
    setEditingRuleId(null);
    setRuleName('');
    setRuleDescription('');
    setWhoType('AGE');
    setWhoAgeOp('GREATER_THAN');
    setWhoAgeVal(65);
    setWhoGroupVal('');
    setWhoGenderVal('weiblich');
    setWhoSpecialVal('');
    setWhereType('TOP_BUNK');
    setWhereFloorVal(0);
    setRatingType('FORBID');
    setCustomWeight(25);
    setShowAddModal(true);
  };

  // Erzeugt aus dem WER -> WO -> BEWERTUNG Flow die Bedingungen und Payload
  const handleSaveCustomRule = async () => {
    let finalName = ruleName.trim();
    if (!finalName) {
      // Automatischer lesbarer Name
      const whoText = whoType === 'AGE' ? `Personen Alter ${whoAgeOp === 'GREATER_THAN' ? '>' : '<'} ${whoAgeVal}` :
                      whoType === 'GROUP' ? `Gruppe '${whoGroupVal || 'Alle'}'` :
                      whoType === 'GENDER' ? `Geschlecht ${whoGenderVal}` :
                      whoType === 'SPECIAL' ? `Mit Anmerkung '${whoSpecialVal}'` : 'Alle Personen';
      const whereText = whereType === 'TOP_BUNK' ? 'oberes Hochbett' :
                        whereType === 'BOTTOM_BUNK' ? 'unteres Bett' :
                        whereType === 'DOUBLE_BED' ? 'Doppelbett' :
                        whereType === 'GROUND_FLOOR' ? 'Erdgeschoss' :
                        whereType === 'SPECIFIC_FLOOR' ? `Etage ${whereFloorVal}` : 'gleiches Zimmer';
      finalName = `${whoText} ➔ ${whereText}`;
    }

    // 1. Bedingungen generieren
    const conditions: Condition[] = [];

    // WER
    if (whoType === 'AGE') {
      conditions.push({ field: 'person.age', operator: whoAgeOp, value: whoAgeVal });
    } else if (whoType === 'GROUP' && whoGroupVal.trim()) {
      conditions.push({ field: 'person.groupId', operator: 'EQUALS', value: whoGroupVal.trim() });
    } else if (whoType === 'GENDER') {
      conditions.push({ field: 'person.gender', operator: 'EQUALS', value: whoGenderVal });
    } else if (whoType === 'SPECIAL' && whoSpecialVal.trim()) {
      conditions.push({ field: 'person.specialneeds', operator: 'CONTAINS', value: whoSpecialVal.trim() });
    }

    // WO
    let targetScope: TargetScope = 'BED_PERSON';
    if (whereType === 'TOP_BUNK') {
      conditions.push({ field: 'bed.isTopBunk', operator: 'EQUALS', value: true });
      targetScope = 'BED_PERSON';
    } else if (whereType === 'BOTTOM_BUNK') {
      conditions.push({ field: 'bed.isBottomBunk', operator: 'EQUALS', value: true });
      targetScope = 'BED_PERSON';
    } else if (whereType === 'DOUBLE_BED') {
      conditions.push({ field: 'bed.bedType', operator: 'EQUALS', value: 'DOUBLE' });
      targetScope = 'BED_PERSON';
    } else if (whereType === 'GROUND_FLOOR') {
      conditions.push({ field: 'room.floor', operator: 'EQUALS', value: 0 });
      targetScope = 'ROOM_PERSON';
    } else if (whereType === 'SPECIFIC_FLOOR') {
      conditions.push({ field: 'room.floor', operator: 'EQUALS', value: whereFloorVal });
      targetScope = 'ROOM_PERSON';
    } else if (whereType === 'SAME_ROOM') {
      targetScope = whoType === 'GROUP' ? 'GROUP_CO_LOCATION' : 'PAIR_CO_LOCATION';
    }

    // WIE BEWERTEN
    const ruleType: RuleType = ratingType === 'FORBID' ? 'HARD' : 'SOFT';
    let weight = 0;
    if (ratingType === 'FORBID') weight = -999999;
    else if (ratingType === 'AVOID_STRONG') weight = -50;
    else if (ratingType === 'AVOID') weight = -25;
    else if (ratingType === 'VERY_HIGH') weight = 50;
    else if (ratingType === 'HIGH') weight = 25;
    else if (ratingType === 'LOW') weight = 10;
    else if (ratingType === 'CUSTOM') weight = customWeight;

    const payload: DynamicRule = {
      id: editingRuleId || '',
      name: finalName,
      description: ruleDescription.trim() || generateLiveSummary(),
      active: true,
      ruleType,
      targetScope,
      action: ruleType === 'HARD' ? 'FORBID' : 'ADD_POINTS',
      weight,
      conditions,
    };

    try {
      let updated: Project;
      if (editingRuleId) {
        updated = await api.updateRule(project.id, editingRuleId, payload);
      } else {
        updated = await api.addRule(project.id, payload);
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

  // Generiert die Live-Satz-Vorschau in natürlichem Deutsch
  const generateLiveSummary = () => {
    let who = '';
    if (whoType === 'AGE') {
      who = `Personen mit Alter ${whoAgeOp === 'GREATER_THAN' ? 'über' : whoAgeOp === 'LESS_THAN' ? 'unter' : 'von genau'} ${whoAgeVal} Jahren`;
    } else if (whoType === 'GROUP') {
      who = `Personen aus Gruppe '${whoGroupVal || '...'}'`;
    } else if (whoType === 'GENDER') {
      who = `Personen mit Geschlecht ${whoGenderVal}`;
    } else if (whoType === 'SPECIAL') {
      who = `Personen mit Anmerkung '${whoSpecialVal || '...'}'`;
    } else {
      who = `Alle Personen`;
    }

    let where = '';
    if (whereType === 'TOP_BUNK') where = 'ins obere Hochbett';
    else if (whereType === 'BOTTOM_BUNK') where = 'ins untere Bett';
    else if (whereType === 'DOUBLE_BED') where = 'in ein Doppelbett';
    else if (whereType === 'GROUND_FLOOR') where = 'ins Erdgeschoss (Etage 0)';
    else if (whereType === 'SPECIFIC_FLOOR') where = `in Etage ${whereFloorVal}`;
    else if (whereType === 'SAME_ROOM') where = 'zusammen ins selbe Zimmer';

    let rating = '';
    if (ratingType === 'FORBID') rating = 'STRIKT VERBOTEN (Ausgeschlossen)';
    else if (ratingType === 'AVOID_STRONG') rating = 'DRINGEND VERMIEDEN (Sehr hohe Strafe)';
    else if (ratingType === 'AVOID') rating = 'EHER VERMIEDEN (Leichte Strafe)';
    else if (ratingType === 'VERY_HIGH') rating = 'UNBEDINGT ANGESTREBT (Höchste Priorität)';
    else if (ratingType === 'HIGH') rating = 'BEVORZUGT (Normale Priorität)';
    else if (ratingType === 'LOW') rating = 'MIT LEICHTEM BONUS BELOHNT';
    else if (ratingType === 'CUSTOM') rating = `MIT ${customWeight > 0 ? '+' : ''}${customWeight} PUNKTEN GEWICHTET`;

    return `WENN ${who}, DANN soll die Belegung ${where} ${rating} werden.`;
  };

  // Trennung: Standard-Regeln vs. Benutzerdefinierte Regeln
  const genderRule = findStandardRule('GENDER');
  const coupleRule = findStandardRule('COUPLES');
  const groupRule = findStandardRule('GROUPS');
  const seniorRule = findStandardRule('SENIORS');

  const standardIds = [genderRule?.id, coupleRule?.id, groupRule?.id, seniorRule?.id].filter(Boolean);
  const customRules = project.rules.filter(r => !standardIds.includes(r.id));

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <SlidersHorizontal className="w-7 h-7 text-indigo-400" /> Regel-Verwaltung & Szenario-Baukasten
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Steuere die automatische Bettenverteilung: Schalte Standard-Regeln mit 1 Klick ein oder formuliere individuelle WENN ➔ DANN Regeln.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center space-x-1.5 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-xl shadow-lg shadow-indigo-500/20 transition"
        >
          <Plus className="w-4 h-4" /> <span>Eigene Regel erstellen (WENN ➔ DANN)</span>
        </button>
      </div>

      {/* BEREICH 1: DIE 4 PRAXIS-STANDARDS (1-Klick Themen-Schalter) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-300 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" /> Praxis-Standards (Empfohlen für jede Freizeit)
            </h3>
            <p className="text-xs text-slate-400">
              Diese Regeln decken 95 % aller Anforderungen ab. Du kannst sie einfach per Klick an- oder ausschalten.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Karte 1: Geschlechtertrennung */}
          <div className={`p-4 rounded-2xl border transition ${genderRule?.active ? 'bg-slate-900/90 border-indigo-500/50 shadow-lg shadow-indigo-500/5' : 'glass-panel border-slate-800 opacity-70'}`}>
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-pink-500/10 text-pink-400 flex items-center justify-center font-bold">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-semibold text-sm text-slate-100">Geschlechtertrennung im Zimmer</h4>
                  <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider bg-rose-500/20 text-rose-300 border border-rose-500/30">
                    Strikte Regel (Getrennte Zimmer)
                  </span>
                </div>
              </div>

              <button
                onClick={() => handleToggleStandardRule('GENDER')}
                className={`w-12 h-6 rounded-full transition p-1 flex items-center ${genderRule?.active ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start border border-slate-700'}`}
              >
                <div className="w-4 h-4 rounded-full bg-white shadow-md" />
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2.5">
              Männer und Frauen dürfen nicht im selben Zimmer schlafen (getrennte Zimmer). Wenn inaktiv, sind gemischte Zimmer für alle erlaubt.
            </p>
          </div>

          {/* Karte 2: Paare zusammen */}
          <div className={`p-4 rounded-2xl border transition ${coupleRule?.active ? 'bg-slate-900/90 border-indigo-500/50 shadow-lg shadow-indigo-500/5' : 'glass-panel border-slate-800 opacity-70'}`}>
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center font-bold">
                  <Heart className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-semibold text-sm text-slate-100">Paare im selben Zimmer</h4>
                  <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    Hohe Priorität (Wunsch)
                  </span>
                </div>
              </div>

              <button
                onClick={() => handleToggleStandardRule('COUPLES')}
                className={`w-12 h-6 rounded-full transition p-1 flex items-center ${coupleRule?.active ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start border border-slate-700'}`}
              >
                <div className="w-4 h-4 rounded-full bg-white shadow-md" />
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2.5">
              Angemeldete Paare mit gleicher Paar-ID werden bevorzugt im selben Zimmer / Doppelbett untergebracht (funktioniert mit und ohne Geschlechtertrennung).
            </p>
          </div>

          {/* Karte 3: Gruppenzusammenhalt */}
          <div className={`p-4 rounded-2xl border transition ${groupRule?.active ? 'bg-slate-900/90 border-indigo-500/50 shadow-lg shadow-indigo-500/5' : 'glass-panel border-slate-800 opacity-70'}`}>
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center font-bold">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-semibold text-sm text-slate-100">Gruppenzusammenhalt im Zimmer</h4>
                  <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                    Bevorzugen (Wunsch)
                  </span>
                </div>
              </div>

              <button
                onClick={() => handleToggleStandardRule('GROUPS')}
                className={`w-12 h-6 rounded-full transition p-1 flex items-center ${groupRule?.active ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start border border-slate-700'}`}
              >
                <div className="w-4 h-4 rounded-full bg-white shadow-md" />
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2.5">
              Personen derselben Gruppe oder Schulklasse werden bevorzugt zusammen in dieselben Zimmer gelegt.
            </p>
          </div>

          {/* Karte 4: Senioren nicht oben */}
          <div className={`p-4 rounded-2xl border transition ${seniorRule?.active ? 'bg-slate-900/90 border-indigo-500/50 shadow-lg shadow-indigo-500/5' : 'glass-panel border-slate-800 opacity-70'}`}>
            <div className="flex items-start justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center font-bold">
                  <Bed className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-semibold text-sm text-slate-100">Senioren nicht ins obere Hochbett</h4>
                  <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider bg-rose-500/20 text-rose-300 border border-rose-500/30">
                    Sicherheits-Verbot (&gt; 65 Jahre)
                  </span>
                </div>
              </div>

              <button
                onClick={() => handleToggleStandardRule('SENIORS')}
                className={`w-12 h-6 rounded-full transition p-1 flex items-center ${seniorRule?.active ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start border border-slate-700'}`}
              >
                <div className="w-4 h-4 rounded-full bg-white shadow-md" />
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2.5">
              Aus Sicherheitsgründen werden Personen über 65 Jahren niemals im oberen Hochbett untergebracht.
            </p>
          </div>
        </div>
      </div>

      {/* BEREICH 2: BENUTZERDEFINIERTE REGELN (WER -> WO -> BEWERTUNG) */}
      <div className="space-y-4 pt-4 border-t border-slate-800">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
              <UserCheck className="w-4 h-4 text-indigo-400" /> Benutzerdefinierte Zusatzregeln ({customRules.length})
            </h3>
            <p className="text-xs text-slate-400">
              Spezielle Szenarien: Definiere genau, WER wo liegen darf oder soll (z. B. nach Gruppe, Alter, Etage oder Anmerkungen).
            </p>
          </div>
        </div>

        {customRules.length === 0 ? (
          <div className="p-8 glass-panel rounded-2xl text-center space-y-3 border border-dashed border-slate-800">
            <div className="text-slate-400 text-sm font-medium">Keine benutzerdefinierten Zusatzregeln aktiv</div>
            <p className="text-slate-500 text-xs max-w-md mx-auto">
              Die Standard-Regeln oben reichen für die meisten Freizeiten völlig aus. Wenn du für bestimmte Gruppen oder Teilnehmer spezielle Wünsche hast, klicke auf <b>"Eigene Regel erstellen"</b>.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {customRules.map((r) => {
              const isHard = r.ruleType === 'HARD';
              const isInfoOpen = infoRuleId === r.id;
              return (
                <div key={r.id} className="space-y-2">
                  <div
                    className={`glass-panel p-4 rounded-2xl border transition flex items-center justify-between ${
                      r.active ? (isHard ? 'border-rose-500/40 bg-slate-900/60' : 'border-indigo-500/40 bg-slate-900/60') : 'border-slate-800 opacity-60'
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
                              isHard
                                ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                                : r.weight < 0
                                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                                : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                            }`}
                          >
                            {isHard ? 'Strikte Regel (Verbot)' : r.weight < 0 ? 'Vermeiden' : 'Bevorzugen (Wunsch)'}
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
                        <span>Details</span>
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
                        <HelpCircle className="w-4 h-4" /> Logik & Auswirkung:
                      </div>
                      <div>
                        <span className="text-slate-400">Regel-Typ: </span>
                        <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Strikter Ausschluss (Verbot)' : r.weight < 0 ? 'Unerwünschtes Szenario (Strafe)' : 'Gewünschtes Szenario (Ziel)'}</span>
                      </div>
                      {r.conditions && r.conditions.length > 0 && (
                        <div>
                          <span className="text-slate-400">Prüfbedingungen: </span>
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
        )}
      </div>

      {/* MODAL: NEUE BENUTZERDEFINIERTE REGEL NACH "WER -> WO -> BEWERTUNG" */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-indigo-500/40 max-w-2xl w-full space-y-6 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-indigo-400" />
                <h3 className="text-lg font-bold text-slate-100">Neue Regel nach WENN ➔ DANN formulieren</h3>
              </div>
              <button onClick={() => setShowAddModal(false)} className="p-1 text-slate-400 hover:text-slate-200 rounded-lg transition">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* SCHRITT 1: WER? */}
            <div className="p-4 bg-slate-900/90 rounded-xl border border-slate-800 space-y-3">
              <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider block flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[11px] flex items-center justify-center font-bold">1</span>
                WENN... Wer ist betroffen? (Zielgruppe)
              </span>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] text-slate-400 mb-1">Eigenschaft wählen:</label>
                  <select
                    value={whoType}
                    onChange={(e: any) => setWhoType(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                  >
                    <option value="AGE">👤 Alter der Person</option>
                    <option value="GROUP">👥 Gruppe / Schulklasse</option>
                    <option value="GENDER">🚻 Geschlecht</option>
                    <option value="SPECIAL">⚠️ Sonderwunsch / Anmerkung</option>
                    <option value="ALL">🌐 Alle Personen</option>
                  </select>
                </div>

                {/* Dynamischer Input für WER */}
                {whoType === 'AGE' && (
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Vergleich:</label>
                      <select
                        value={whoAgeOp}
                        onChange={(e: any) => setWhoAgeOp(e.target.value)}
                        className="w-full px-2.5 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs"
                      >
                        <option value="GREATER_THAN">älter als (&gt;)</option>
                        <option value="LESS_THAN">jünger als (&lt;)</option>
                        <option value="EQUALS">genau gleich (=)</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Alter (Jahre):</label>
                      <input
                        type="number"
                        value={whoAgeVal}
                        onChange={(e) => setWhoAgeVal(parseInt(e.target.value) || 0)}
                        className="w-full px-2.5 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-mono"
                      />
                    </div>
                  </div>
                )}

                {whoType === 'GROUP' && (
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Gruppenname / Klasse:</label>
                    <input
                      type="text"
                      placeholder="z.B. Jugend 1 oder Klasse 8a"
                      value={whoGroupVal}
                      onChange={(e) => setWhoGroupVal(e.target.value)}
                      className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                    />
                  </div>
                )}

                {whoType === 'GENDER' && (
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Geschlecht:</label>
                    <select
                      value={whoGenderVal}
                      onChange={(e: any) => setWhoGenderVal(e.target.value)}
                      className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                    >
                      <option value="maennlich">Männlich</option>
                      <option value="weiblich">Weiblich</option>
                      <option value="divers">Divers</option>
                    </select>
                  </div>
                )}

                {whoType === 'SPECIAL' && (
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Enthält Suchbegriff:</label>
                    <input
                      type="text"
                      placeholder="z.B. Rollstuhl, Asthma, Gehbehinderung"
                      value={whoSpecialVal}
                      onChange={(e) => setWhoSpecialVal(e.target.value)}
                      className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                    />
                  </div>
                )}
              </div>
            </div>

            {/* SCHRITT 2: WO? */}
            <div className="p-4 bg-slate-900/90 rounded-xl border border-slate-800 space-y-3">
              <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider block flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[11px] flex items-center justify-center font-bold">2</span>
                WO soll die Unterbringung sein? (Bett, Zimmer oder Etage)
              </span>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] text-slate-400 mb-1">Ziel-Ort wählen:</label>
                  <select
                    value={whereType}
                    onChange={(e: any) => setWhereType(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                  >
                    <option value="TOP_BUNK">🛏️ Oberes Hochbett</option>
                    <option value="BOTTOM_BUNK">🛏️ Unteres Bett / Standardbett</option>
                    <option value="DOUBLE_BED">🛏️ Doppelbett</option>
                    <option value="GROUND_FLOOR">🚪 Erdgeschoss (Etage 0)</option>
                    <option value="SPECIFIC_FLOOR">🚪 Bestimmte Etage</option>
                    <option value="SAME_ROOM">👥 Zusammen ins selbe Zimmer</option>
                  </select>
                </div>

                {whereType === 'SPECIFIC_FLOOR' && (
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1">Etagen-Nummer:</label>
                    <input
                      type="number"
                      placeholder="z.B. 1 oder 2"
                      value={whereFloorVal}
                      onChange={(e) => setWhereFloorVal(parseInt(e.target.value) || 0)}
                      className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-mono"
                    />
                  </div>
                )}
              </div>
            </div>

            {/* SCHRITT 3: ZIEL / BEWERTUNG */}
            <div className="p-4 bg-slate-900/90 rounded-xl border border-slate-800 space-y-4">
              <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider block flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[11px] flex items-center justify-center font-bold">3</span>
                DANN... Was ist das Ziel für dieses Szenario?
              </span>

              {/* GRUPPE 1: VERMEIDEN / VERBIETEN */}
              <div className="space-y-2">
                <span className="text-[11px] font-bold text-rose-300 uppercase tracking-wider block flex items-center gap-1">
                  🛑 Szenario soll VERHINDERT / VERMIEDEN werden:
                </span>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setRatingType('FORBID')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'FORBID'
                        ? 'bg-rose-500/20 border-rose-500 text-rose-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <XCircle className="w-4 h-4 text-rose-400" />
                      <span>Strikt verbieten</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Niemals zulassen (Verbot)</div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRatingType('AVOID_STRONG')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'AVOID_STRONG'
                        ? 'bg-rose-500/20 border-rose-500 text-rose-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <AlertTriangle className="w-4 h-4 text-amber-400" />
                      <span>Dringend vermeiden</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Höchste Priorität zum Vermeiden</div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRatingType('AVOID')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'AVOID'
                        ? 'bg-amber-500/20 border-amber-500 text-amber-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <AlertTriangle className="w-4 h-4 text-amber-400" />
                      <span>Eher vermeiden</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Leichte Vermeidung</div>
                  </button>
                </div>
              </div>

              {/* GRUPPE 2: ERFÜLLEN / WÜNSCHE */}
              <div className="space-y-2 pt-2 border-t border-slate-800">
                <span className="text-[11px] font-bold text-emerald-300 uppercase tracking-wider block flex items-center gap-1">
                  🎯 Szenario soll ERFÜLLT / BEVORZUGT werden:
                </span>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setRatingType('VERY_HIGH')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'VERY_HIGH'
                        ? 'bg-emerald-500/20 border-emerald-500 text-emerald-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                      <span>Unbedingt anstreben</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Höchste Priorität (Wunsch)</div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRatingType('HIGH')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'HIGH'
                        ? 'bg-indigo-500/20 border-indigo-500 text-indigo-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <CheckCircle2 className="w-4 h-4 text-indigo-400" />
                      <span>Bevorzugen</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Mittlere Priorität (Wunsch)</div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRatingType('LOW')}
                    className={`p-3 rounded-xl border text-left transition ${
                      ratingType === 'LOW'
                        ? 'bg-indigo-500/20 border-indigo-500 text-indigo-200 font-bold'
                        : 'bg-slate-800/80 border-slate-700 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div className="flex items-center gap-1.5 text-xs">
                      <CheckCircle2 className="w-4 h-4 text-indigo-400" />
                      <span>Leichter Bonus</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">Geringe Priorität</div>
                  </button>
                </div>
              </div>
            </div>

            {/* LIVE-SATZ-VORSCHAU */}
            <div className="p-4 bg-indigo-950/40 border border-indigo-500/40 rounded-xl space-y-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-300 flex items-center gap-1.5">
                <ArrowRight className="w-3.5 h-3.5 text-indigo-400" /> Vorschau der Regel (In natürlicher Sprache):
              </span>
              <div className="text-xs text-slate-100 font-medium">
                {generateLiveSummary()}
              </div>
            </div>

            <div className="flex justify-end space-x-2 pt-2 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 bg-slate-800 text-slate-400 hover:text-slate-200 text-xs rounded-lg transition"
              >
                Abbrechen
              </button>
              <button
                type="button"
                onClick={handleSaveCustomRule}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg shadow-md transition"
              >
                Regel aktivieren
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
