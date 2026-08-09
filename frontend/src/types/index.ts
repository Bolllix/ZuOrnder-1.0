export type BedType = 'SINGLE' | 'DOUBLE' | 'TOP_BUNK' | 'BOTTOM_BUNK' | 'SOFA' | 'OTHER';

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

export interface Room {
  id: string;
  name: string;
  floor: number;
  buildingName: string;
  girlsRoom: boolean;
  boysRoom: boolean;
  beds: Bed[];
  customAttributes?: Record<string, any>;
}

export interface Building {
  id: string;
  name: string;
  rooms: Room[];
}

export interface Person {
  id: string;
  firstname: string;
  lastname: string;
  gender: string;
  age: number;
  partnerId?: string;
  groupId?: string;
  desiredFloor?: number;
  desiredRoom?: string;
  specialNeeds?: string;
  customAttributes?: Record<string, any>;
}

export type RuleType = 'HARD' | 'SOFT';
export type TargetScope = 'BED_PERSON' | 'ROOM_PERSON' | 'PAIR_CO_LOCATION' | 'GROUP_CO_LOCATION';
export type RuleAction = 'FORBID' | 'ADD_POINTS' | 'SUBTRACT_POINTS';
export type Operator = 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'GREATER_EQUAL' | 'LESS_THAN' | 'LESS_EQUAL' | 'IN' | 'CONTAINS' | 'MATCH_ROOM_PROP';

export interface Condition {
  field: string;
  operator: Operator;
  value: any;
}

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
}

export interface ScoreExplanation {
  ruleId: string;
  ruleName: string;
  points: number;
  ruleType: RuleType;
  reason: string;
}

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

export interface AssignmentResult {
  assignments: AssignmentPair[];
  unassignedPersonIds: string[];
  unassignedBedIds: string[];
  totalScore: number;
  hardRuleViolationsCount: number;
  executionTimeMs: number;
}

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

export interface TableData {
  sheetName: string;
  headers: string[];
  rows: string[][];
  totalRows: number;
}

export interface ImportValidationResult {
  validPersons: Person[];
  warnings: string[];
  errors: string[];
  totalRowsProcessed: number;
}
