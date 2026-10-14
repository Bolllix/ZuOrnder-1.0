// ============================================================================
// Datentypen (TypeScript-"Interfaces") für das Frontend
// ============================================================================
// Diese Typen beschreiben, wie die Daten aussehen, die zwischen Browser und
// Java-Server hin- und hergeschickt werden. Sie entsprechen den Java-Klassen
// im Ordner src/main/java/de/zuordner/model. Ändert man dort ein Feld, muss
// man es hier ebenfalls ändern.
//
// Kurz erklärt:
//   interface X { a: string }   -> ein Objekt vom Typ X hat ein Feld "a" (Text)
//   b?: number                  -> das "?" heißt: Feld ist optional (darf fehlen)
//   type Y = 'A' | 'B'          -> Y darf nur genau 'A' oder 'B' sein

/** Bettarten – müssen exakt den Werten im Java-Enum BedType entsprechen */
export type BedType = 'SINGLE' | 'DOUBLE' | 'TOP_BUNK' | 'BOTTOM_BUNK' | 'SOFA' | 'OTHER';

/** Ein Schlafplatz (ein Doppel- oder Hochbett besteht aus zwei Einträgen) */
export interface Bed {
  id: string;
  name: string;
  bedType: BedType;
  roomName: string;
  floor: number;
  buildingName: string;
  occupantId?: string;
  customAttributes?: Record<string, any>;
}

/** Ein Raum mit seinen Betten */
export interface Room {
  id: string;
  name: string;
  /** 0 = Erdgeschoss */
  floor: number;
  buildingName: string;
  /** Nur Mädchen/Frauen (gilt, wenn die Geschlechtertrennung aktiv ist) */
  girlsRoom: boolean;
  /** Nur Jungen/Männer (gilt, wenn die Geschlechtertrennung aktiv ist) */
  boysRoom: boolean;
  beds: Bed[];
  customAttributes?: Record<string, any>;
}

/** Ein Gebäude mit seinen Räumen */
export interface Building {
  id: string;
  name: string;
  rooms: Room[];
}

/** Ein Teilnehmer */
export interface Person {
  id: string;
  firstname: string;
  lastname: string;
  /** 'maennlich' | 'weiblich' | 'divers' */
  gender: string;
  age: number;
  /** Paare haben dieselbe Paar-ID */
  partnerId?: string;
  /** Gruppe / Klasse */
  groupId?: string;
  desiredFloor?: number;
  desiredRoom?: string;
  specialNeeds?: string;
  customAttributes?: Record<string, any>;
}

/** HARD = striktes Verbot, SOFT = Wunsch mit Punkten */
export type RuleType = 'HARD' | 'SOFT';

/**
 * Worauf bezieht sich eine Regel?
 *  - BED_PERSON / ROOM_PERSON: eine Person und ihr Bett/Raum
 *  - GENDER_SEPARATION:        Männer und Frauen in getrennten Zimmern
 *  - PAIR_CO_LOCATION:         Paare (gleiche Paar-ID) zusammen
 *  - GROUP_CO_LOCATION:        Gruppen (gleiche Gruppen-ID) zusammen
 *  - CONDITION_CO_LOCATION:    alle Personen, auf die die WENN-Bedingungen zutreffen, zusammen
 */
export type TargetScope =
  | 'BED_PERSON'
  | 'ROOM_PERSON'
  | 'GENDER_SEPARATION'
  | 'PAIR_CO_LOCATION'
  | 'GROUP_CO_LOCATION'
  | 'CONDITION_CO_LOCATION';

export type RuleAction = 'FORBID' | 'ADD_POINTS' | 'SUBTRACT_POINTS';
export type Operator = 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'GREATER_EQUAL' | 'LESS_THAN' | 'LESS_EQUAL' | 'IN' | 'CONTAINS' | 'MATCH_ROOM_PROP';

/**
 * Die 4 Standard-Regeln mit eigenem Schalter im Regel-Editor.
 * Über diesen festen Schlüssel findet der Regel-Editor "seine" Regel eindeutig.
 */
export type StandardRuleKey = 'GENDER_SEPARATION' | 'COUPLES_TOGETHER' | 'GROUPS_TOGETHER' | 'SENIORS_NO_TOP_BUNK';

/** Eine WENN-Bedingung, z.B. { field: 'person.age', operator: 'GREATER_THAN', value: 65 } */
export interface Condition {
  field: string;
  operator: Operator;
  value: any;
}

/** Eine Regel (siehe Java-Klasse DynamicRule) */
export interface DynamicRule {
  id: string;
  name: string;
  description: string;
  active: boolean;
  ruleType: RuleType;
  targetScope: TargetScope;
  conditions: Condition[];
  action: RuleAction;
  weight: number;
  /** Nur bei den 4 Standard-Regeln gesetzt, sonst leer */
  standardKey?: StandardRuleKey | null;
}

/** Eine neue Regel hat noch keine ID – die vergibt der Server. */
export type NewDynamicRule = Omit<DynamicRule, 'id'>;

/** Eine Begründung, warum eine Person Punkte bekommen hat (oder etwas verboten ist) */
export interface ScoreExplanation {
  ruleId: string;
  ruleName: string;
  points: number;
  ruleType: RuleType;
  reason: string;
}

/** Eine Zuordnung Person -> Bett im Ergebnis */
export interface AssignmentPair {
  personId: string;
  bedId: string;
  personName: string;
  bedName: string;
  roomName: string;
  buildingName: string;
  score: number;
  explanations: ScoreExplanation[];
  manualOverride: boolean;
}

/** Das Ergebnis einer Belegungs-Berechnung */
export interface AssignmentResult {
  assignments: AssignmentPair[];
  unassignedPersonIds: string[];
  /** Grund pro Person ohne Bett (Person-ID -> Text) – ab Version 2.0 */
  unassignedReasons?: Record<string, string>;
  unassignedBedIds: string[];
  totalScore: number;
  hardRuleViolationsCount: number;
  executionTimeMs: number;
}

/** Ein komplettes Projekt */
export interface Project {
  id: string;
  name: string;
  description: string;
  buildings: Building[];
  persons: Person[];
  rules: DynamicRule[];
  assignmentResult?: AssignmentResult;
  createdAt?: string;
  updatedAt?: string;
}

/** Eine eingelesene Excel/CSV-Tabelle (Import-Assistent) */
export interface TableData {
  sheetName: string;
  headers: string[];
  rows: string[][];
  totalRows: number;
}

/** Ergebnis der Spaltenzuordnung im Import-Assistenten */
export interface ImportValidationResult {
  validPersons: Person[];
  warnings: string[];
  errors: string[];
  totalRowsProcessed: number;
}

/** Versions- und Update-Informationen vom Server (/api/system/version) */
export interface SystemVersionInfo {
  currentVersion: string;
  latestVersion: string;
  updateAvailable: boolean;
  releaseNotes?: string;
  releaseDate?: string;
  downloadUrl?: string;
  isDesktop: boolean;
  isJarRunning: boolean;
  offline?: boolean;
}

/** Fortschritt eines laufenden Updates (/api/system/update-status) */
export interface UpdateStatusInfo {
  inProgress: boolean;
  progressPercent: number;
  step: 'IDLE' | 'DOWNLOADING' | 'RESTARTING' | 'ERROR' | 'COMPLETE';
  errorMessage?: string;
}
