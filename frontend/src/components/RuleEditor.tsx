// ============================================================================
// RuleEditor – Regeln verwalten (Reiter "Regel-Editor")
// ============================================================================
// Die Seite hat zwei Bereiche:
//  1. Vier "Praxis-Standards" mit je einem An/Aus-Schalter
//     (Geschlechtertrennung, Paare, Gruppen, Senioren).
//  2. Eigene Zusatzregeln, die man im Fenster "WER -> WO -> BEWERTUNG" baut.
//
// WICHTIG (Fehlerbehebung Version 2.0):
// Jeder Schalter findet "seine" Regel über einen festen Schlüssel (standardKey),
// NICHT mehr über Wörter im Namen. Früher hieß die Geschlechter-Regel z.B.
// "Harte Geschlechtertrennung (mit Paar-Ausnahme)" – der Paar-Schalter hat
// nach dem Wort "paar" gesucht und deshalb die FALSCHE Regel umgeschaltet.
// Außerdem vergibt jetzt der Server die Regel-IDs (früher bekamen alle neuen
// Regeln die leere ID "" und ließen sich danach nicht mehr umschalten).
import React, { useState } from 'react';
import type { Project, DynamicRule, NewDynamicRule, RuleType, TargetScope, Condition, StandardRuleKey } from '../types';
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
  X,
  Link2,
} from 'lucide-react';

interface RuleEditorProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

// Auswahlmöglichkeiten im Fenster "Eigene Regel erstellen"
type WhoType = 'AGE' | 'GROUP' | 'GENDER' | 'SPECIAL' | 'ALL';
type WhereType = 'TOP_BUNK' | 'BOTTOM_BUNK' | 'DOUBLE_BED' | 'GROUND_FLOOR' | 'SPECIFIC_FLOOR' | 'SAME_ROOM';
type RatingType = 'FORBID' | 'AVOID_STRONG' | 'AVOID' | 'VERY_HIGH' | 'HIGH' | 'LOW';

/** Aussehen und Text der 4 Standard-Karten. Die Regel selbst legt der Server an. */
const STANDARD_CARDS: Array<{
  key: StandardRuleKey;
  title: string;
  badge: string;
  badgeClass: string;
  icon: React.ElementType;
  iconClass: string;
  text: string;
}> = [
  {
    key: 'GENDER_SEPARATION',
    title: 'Geschlechtertrennung im Zimmer',
    badge: 'Strikte Regel (Getrennte Zimmer)',
    badgeClass: 'bg-rose-500/20 text-rose-300 border border-rose-500/30',
    icon: ShieldAlert,
    iconClass: 'bg-pink-500/10 text-pink-400',
    text: 'Männer und Frauen schlafen nicht im selben Zimmer. Als Mädchen- bzw. Jungenzimmer markierte Räume sind dann nur für Mädchen bzw. Jungen. Wenn inaktiv, sind gemischte Zimmer für alle erlaubt.',
  },
  {
    key: 'COUPLES_TOGETHER',
    title: 'Paare im selben Zimmer',
    badge: 'Hohe Priorität (Wunsch)',
    badgeClass: 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30',
    icon: Heart,
    iconClass: 'bg-rose-500/10 text-rose-400',
    text: 'Angemeldete Paare mit gleicher Paar-ID werden bevorzugt im selben Zimmer untergebracht (+50 Punkte pro Person), wenn möglich im Doppelbett. Funktioniert mit und ohne Geschlechtertrennung.',
  },
  {
    key: 'GROUPS_TOGETHER',
    title: 'Gruppenzusammenhalt im Zimmer',
    badge: 'Bevorzugen (Wunsch)',
    badgeClass: 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30',
    icon: Users,
    iconClass: 'bg-indigo-500/10 text-indigo-400',
    text: 'Personen derselben Gruppe oder Schulklasse werden bevorzugt zusammen in dieselben Zimmer gelegt.',
  },
  {
    key: 'SENIORS_NO_TOP_BUNK',
    title: 'Senioren nicht ins obere Hochbett',
    badge: 'Sicherheits-Verbot (> 65 Jahre)',
    badgeClass: 'bg-rose-500/20 text-rose-300 border border-rose-500/30',
    icon: Bed,
    iconClass: 'bg-amber-500/10 text-amber-400',
    text: 'Aus Sicherheitsgründen werden Personen über 65 Jahren niemals im oberen Hochbett untergebracht.',
  },
];

/** Lesbare Namen für die Regel-Typen (für die "Details"-Ansicht) */
const SCOPE_LABELS: Record<TargetScope, string> = {
  BED_PERSON: 'Person ↔ Bett',
  ROOM_PERSON: 'Person ↔ Raum/Etage',
  GENDER_SEPARATION: 'Geschlechtertrennung',
  PAIR_CO_LOCATION: 'Paare zusammen',
  GROUP_CO_LOCATION: 'Gruppe zusammen',
  CONDITION_CO_LOCATION: 'Personen mit gleicher Eigenschaft zusammen',
};

export const RuleEditor: React.FC<RuleEditorProps> = ({ project, onProjectUpdate }) => {
  // "useState" speichert Werte, die sich ändern können. Ändert sich ein Wert,
  // zeichnet React die Komponente automatisch neu.
  const [showAddModal, setShowAddModal] = useState(false);
  const [infoRuleId, setInfoRuleId] = useState<string | null>(null);

  // Formular für eigene Regeln (WER -> WO -> BEWERTUNG)
  const [ruleName, setRuleName] = useState('');
  const [ruleDescription, setRuleDescription] = useState('');

  // 1. WER? (Personen-Filter)
  const [whoType, setWhoType] = useState<WhoType>('AGE');
  const [whoAgeOp, setWhoAgeOp] = useState<'GREATER_THAN' | 'LESS_THAN' | 'EQUALS'>('GREATER_THAN');
  const [whoAgeVal, setWhoAgeVal] = useState<number>(65);
  const [whoGroupVal, setWhoGroupVal] = useState<string>('');
  const [whoGenderVal, setWhoGenderVal] = useState<'maennlich' | 'weiblich' | 'divers'>('weiblich');
  const [whoSpecialVal, setWhoSpecialVal] = useState<string>('');

  // 2. WO? (Bett / Zimmer / Etage)
  const [whereType, setWhereType] = useState<WhereType>('TOP_BUNK');
  const [whereFloorVal, setWhereFloorVal] = useState<number>(0);

  // 3. WIE BEWERTEN? (Gewichtung)
  const [ratingType, setRatingType] = useState<RatingType>('FORBID');

  // --------------------------------------------------------------------------
  // Standard-Regeln
  // --------------------------------------------------------------------------

  /** Sucht die Regel zu einem Standard-Schalter – eindeutig über ihren Schlüssel. */
  const findStandardRule = (key: StandardRuleKey): DynamicRule | undefined =>
    project.rules?.find((r) => r.standardKey === key);

  /**
   * Schalter umlegen: Der Server ändert GENAU diese eine Regel
   * (und legt sie an, falls es sie noch nicht gibt). Die anderen bleiben unberührt.
   */
  const handleToggleStandardRule = async (key: StandardRuleKey) => {
    const existing = findStandardRule(key);
    const newActive = !(existing?.active ?? false);
    try {
      const updatedProject = await api.setStandardRuleActive(project.id, key, newActive);
      onProjectUpdate(updatedProject);
    } catch {
      alert('Fehler beim Umschalten der Regel.');
    }
  };

  // --------------------------------------------------------------------------
  // Eigene Regeln
  // --------------------------------------------------------------------------

  /** Öffnet das Fenster für eine neue eigene Regel (mit Startwerten). */
  const handleOpenAddModal = () => {
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
    setShowAddModal(true);
  };

  // "Zusammen ins selbe Zimmer" braucht eine Zielgruppe – "alle Personen zusammen" ergibt keinen Sinn
  const sameRoomWithoutTarget = whereType === 'SAME_ROOM' && whoType === 'ALL';

  /** Baut aus den Eingaben (WER -> WO -> BEWERTUNG) eine Regel und speichert sie. */
  const handleSaveCustomRule = async () => {
    if (sameRoomWithoutTarget) return;

    let finalName = ruleName.trim();
    if (!finalName) {
      // Automatischer, lesbarer Name, falls keiner eingegeben wurde
      const whoText = whoType === 'AGE' ? `Personen Alter ${whoAgeOp === 'GREATER_THAN' ? '>' : whoAgeOp === 'LESS_THAN' ? '<' : '='} ${whoAgeVal}` :
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

    // 1. WENN-Bedingungen aus "WER" erzeugen
    const conditions: Condition[] = [];
    if (whoType === 'AGE') {
      conditions.push({ field: 'person.age', operator: whoAgeOp, value: whoAgeVal });
    } else if (whoType === 'GROUP' && whoGroupVal.trim()) {
      conditions.push({ field: 'person.groupId', operator: 'EQUALS', value: whoGroupVal.trim() });
    } else if (whoType === 'GENDER') {
      conditions.push({ field: 'person.gender', operator: 'EQUALS', value: whoGenderVal });
    } else if (whoType === 'SPECIAL' && whoSpecialVal.trim()) {
      conditions.push({ field: 'person.specialneeds', operator: 'CONTAINS', value: whoSpecialVal.trim() });
    }

    // 2. "WO" bestimmt weitere Bedingungen und den Regel-Typ (targetScope)
    let targetScope: TargetScope = 'BED_PERSON';
    if (whereType === 'TOP_BUNK') {
      conditions.push({ field: 'bed.isTopBunk', operator: 'EQUALS', value: true });
    } else if (whereType === 'BOTTOM_BUNK') {
      conditions.push({ field: 'bed.isBottomBunk', operator: 'EQUALS', value: true });
    } else if (whereType === 'DOUBLE_BED') {
      conditions.push({ field: 'bed.bedType', operator: 'EQUALS', value: 'DOUBLE' });
    } else if (whereType === 'GROUND_FLOOR') {
      conditions.push({ field: 'room.floor', operator: 'EQUALS', value: 0 });
      targetScope = 'ROOM_PERSON';
    } else if (whereType === 'SPECIFIC_FLOOR') {
      conditions.push({ field: 'room.floor', operator: 'EQUALS', value: whereFloorVal });
      targetScope = 'ROOM_PERSON';
    } else if (whereType === 'SAME_ROOM') {
      // Gruppe -> "Mitglieder derselben Gruppe zusammen" (ggf. nur die genannte Gruppe).
      // Sonst -> "Alle Personen mit dieser Eigenschaft zusammen" (z.B. alle über 65).
      // (In Version 1.x wurde hier fälschlich eine Paar-Regel daraus.)
      targetScope = whoType === 'GROUP' ? 'GROUP_CO_LOCATION' : 'CONDITION_CO_LOCATION';
    }

    // 3. "BEWERTUNG" -> harte Regel (Verbot) oder Wunsch mit Punkten
    const ruleType: RuleType = ratingType === 'FORBID' ? 'HARD' : 'SOFT';
    const weightByRating: Record<RatingType, number> = {
      FORBID: -999999,
      AVOID_STRONG: -50,
      AVOID: -25,
      VERY_HIGH: 50,
      HIGH: 25,
      LOW: 10,
    };

    const payload: NewDynamicRule = {
      name: finalName,
      description: ruleDescription.trim() || generateLiveSummary(),
      active: true,
      ruleType,
      targetScope,
      action: ruleType === 'HARD' ? 'FORBID' : 'ADD_POINTS',
      weight: weightByRating[ratingType],
      conditions,
    };

    try {
      const updated = await api.addRule(project.id, payload);
      onProjectUpdate(updated);
      setShowAddModal(false);
    } catch {
      alert('Fehler beim Speichern der Regel.');
    }
  };

  const handleDeleteRule = async (ruleId: string) => {
    if (!confirm('Möchten Sie diese Regel wirklich löschen?')) return;
    try {
      const updated = await api.deleteRule(project.id, ruleId);
      onProjectUpdate(updated);
    } catch {
      alert('Fehler beim Löschen der Regel.');
    }
  };

  /** Eigene Regel an-/ausschalten (die ID ist eindeutig, es ändert sich nur diese Regel). */
  const handleToggleRuleActive = async (rule: DynamicRule) => {
    try {
      const updatedRule = { ...rule, active: !rule.active };
      const updated = await api.updateRule(project.id, rule.id, updatedRule);
      onProjectUpdate(updated);
    } catch {
      alert('Fehler beim Aktualisieren der Regel.');
    }
  };

  /** Erzeugt die Vorschau der Regel als normalen deutschen Satz. */
  const generateLiveSummary = () => {
    let who = '';
    if (whoType === 'AGE') {
      who = `Personen mit Alter ${whoAgeOp === 'GREATER_THAN' ? 'über' : whoAgeOp === 'LESS_THAN' ? 'unter' : 'von genau'} ${whoAgeVal} Jahren`;
    } else if (whoType === 'GROUP') {
      who = whoGroupVal.trim() ? `Personen aus Gruppe '${whoGroupVal}'` : 'Personen derselben Gruppe';
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

    const ratingText: Record<RatingType, string> = {
      FORBID: 'STRIKT VERBOTEN (Ausgeschlossen)',
      AVOID_STRONG: 'DRINGEND VERMIEDEN (Sehr hohe Strafe)',
      AVOID: 'EHER VERMIEDEN (Leichte Strafe)',
      VERY_HIGH: 'UNBEDINGT ANGESTREBT (Höchste Priorität)',
      HIGH: 'BEVORZUGT (Normale Priorität)',
      LOW: 'MIT LEICHTEM BONUS BELOHNT',
    };

    return `WENN ${who}, DANN soll die Belegung ${where} ${ratingText[ratingType]} werden.`;
  };

  // Standard-Regeln vs. eigene Regeln trennen
  const genderRule = findStandardRule('GENDER_SEPARATION');
  const coupleRule = findStandardRule('COUPLES_TOGETHER');
  const customRules = project.rules.filter((r) => !r.standardKey);

  // Kleiner Hinweis, wie Geschlechtertrennung und Paar-Regel gerade zusammenspielen
  let interplayHint: string | null = null;
  if (genderRule?.active && coupleRule?.active) {
    interplayHint = 'Geschlechtertrennung UND Paar-Regel sind aktiv: Getrennte Zimmer für alle – nur angemeldete Paare (gleiche Paar-ID) dürfen zusammen schlafen.';
  } else if (genderRule?.active) {
    interplayHint = 'Nur die Geschlechtertrennung ist aktiv: Auch Paare werden nach Geschlecht getrennt.';
  } else if (coupleRule?.active) {
    interplayHint = 'Nur die Paar-Regel ist aktiv: Paare kommen bevorzugt zusammen, gemischte Zimmer sind erlaubt.';
  }

  return (
    <div className="space-y-8">
      {/* Kopfbereich */}
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

      {/* BEREICH 1: DIE 4 PRAXIS-STANDARDS (1-Klick-Schalter) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold uppercase tracking-wider text-indigo-300 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" /> Praxis-Standards (Empfohlen für jede Freizeit)
            </h3>
            <p className="text-xs text-slate-400">
              Diese Regeln decken die meisten Anforderungen ab. Jeder Schalter wirkt nur auf seine eigene Regel.
            </p>
          </div>
        </div>

        {/* Für jeden Eintrag in STANDARD_CARDS eine Karte zeichnen ("map" = für jedes Element) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {STANDARD_CARDS.map((card) => {
            const rule = findStandardRule(card.key);
            const isActive = rule?.active ?? false;
            const Icon = card.icon;
            return (
              <div
                key={card.key}
                className={`p-4 rounded-2xl border transition ${isActive ? 'bg-slate-900/90 border-indigo-500/50 shadow-lg shadow-indigo-500/5' : 'glass-panel border-slate-800 opacity-70'}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold ${card.iconClass}`}>
                      <Icon className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-slate-100">{card.title}</h4>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${card.badgeClass}`}>
                        {card.badge}
                      </span>
                    </div>
                  </div>

                  {/* Der An/Aus-Schalter */}
                  <button
                    onClick={() => handleToggleStandardRule(card.key)}
                    title={isActive ? 'Regel ausschalten' : 'Regel einschalten'}
                    className={`w-12 h-6 rounded-full transition p-1 flex items-center ${isActive ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start border border-slate-700'}`}
                  >
                    <div className="w-4 h-4 rounded-full bg-white shadow-md" />
                  </button>
                </div>
                <p className="text-xs text-slate-400 mt-2.5">{card.text}</p>
              </div>
            );
          })}
        </div>

        {interplayHint && (
          <div className="flex items-start gap-2 p-3 rounded-xl border border-indigo-500/30 bg-indigo-950/30 text-xs text-indigo-200">
            <Link2 className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
            <span>{interplayHint}</span>
          </div>
        )}
      </div>

      {/* BEREICH 2: EIGENE ZUSATZREGELN (WER -> WO -> BEWERTUNG) */}
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
                      {/* Häkchen = Regel an/aus */}
                      <button
                        onClick={() => handleToggleRuleActive(r)}
                        title={r.active ? 'Regel ausschalten' : 'Regel einschalten'}
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

                  {/* Aufklappbare Details */}
                  {isInfoOpen && (
                    <div className="p-4 glass-card rounded-xl border border-indigo-500/30 text-xs text-slate-300 space-y-2 bg-slate-900/90 ml-6">
                      <div className="font-bold text-indigo-300 flex items-center gap-1.5">
                        <HelpCircle className="w-4 h-4" /> Logik & Auswirkung:
                      </div>
                      <div>
                        <span className="text-slate-400">Regel-Typ: </span>
                        <span className="font-mono text-slate-200">{r.ruleType === 'HARD' ? 'Strikter Ausschluss (Verbot)' : r.weight < 0 ? `Unerwünschtes Szenario (${r.weight} Pkt)` : `Gewünschtes Szenario (+${r.weight} Pkt)`}</span>
                      </div>
                      <div>
                        <span className="text-slate-400">Bezug: </span>
                        <span className="font-mono text-slate-200">{SCOPE_LABELS[r.targetScope] ?? r.targetScope}</span>
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

      {/* FENSTER: NEUE EIGENE REGEL NACH "WER -> WO -> BEWERTUNG" */}
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

            {/* Optional: eigener Name */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] text-slate-400 mb-1">Name der Regel (optional):</label>
                <input
                  type="text"
                  placeholder="wird sonst automatisch erzeugt"
                  value={ruleName}
                  onChange={(e) => setRuleName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                />
              </div>
              <div>
                <label className="block text-[11px] text-slate-400 mb-1">Beschreibung (optional):</label>
                <input
                  type="text"
                  placeholder="sonst wird die Vorschau unten verwendet"
                  value={ruleDescription}
                  onChange={(e) => setRuleDescription(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                />
              </div>
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
                    onChange={(e) => setWhoType(e.target.value as WhoType)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-xs font-medium"
                  >
                    <option value="AGE">👤 Alter der Person</option>
                    <option value="GROUP">👥 Gruppe / Schulklasse</option>
                    <option value="GENDER">🚻 Geschlecht</option>
                    <option value="SPECIAL">⚠️ Sonderwunsch / Anmerkung</option>
                    <option value="ALL">🌐 Alle Personen</option>
                  </select>
                </div>

                {/* Je nach Auswahl ein passendes Eingabefeld */}
                {whoType === 'AGE' && (
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Vergleich:</label>
                      <select
                        value={whoAgeOp}
                        onChange={(e) => setWhoAgeOp(e.target.value as 'GREATER_THAN' | 'LESS_THAN' | 'EQUALS')}
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
                    <label className="block text-[11px] text-slate-400 mb-1">Gruppenname / Klasse (leer = alle Gruppen):</label>
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
                      onChange={(e) => setWhoGenderVal(e.target.value as 'maennlich' | 'weiblich' | 'divers')}
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
                    onChange={(e) => setWhereType(e.target.value as WhereType)}
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

              {sameRoomWithoutTarget && (
                <div className="flex items-center gap-1.5 text-[11px] text-amber-300">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>Für "Zusammen ins selbe Zimmer" bitte oben eine Zielgruppe wählen (z.B. Gruppe oder Alter).</span>
                </div>
              )}
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
                    <div className="text-[10px] text-slate-400 mt-1">-50 Punkte</div>
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
                    <div className="text-[10px] text-slate-400 mt-1">-25 Punkte</div>
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
                    <div className="text-[10px] text-slate-400 mt-1">+50 Punkte</div>
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
                    <div className="text-[10px] text-slate-400 mt-1">+25 Punkte</div>
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
                    <div className="text-[10px] text-slate-400 mt-1">+10 Punkte</div>
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
                disabled={sameRoomWithoutTarget}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg shadow-md transition disabled:opacity-40 disabled:cursor-not-allowed"
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
