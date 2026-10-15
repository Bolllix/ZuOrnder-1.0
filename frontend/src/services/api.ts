// ============================================================================
// api.ts – alle Aufrufe vom Browser an den Java-Server an EINER Stelle
// ============================================================================
// Jede Funktion hier schickt eine Anfrage an eine /api/...-Adresse des Servers
// (siehe die Controller-Klassen im Java-Code) und gibt die Antwort zurück.
//
// "async" / "await": Eine Anfrage übers Netzwerk dauert einen Moment. Mit
// "await" wartet die Funktion auf die Antwort, ohne die Oberfläche einzufrieren.
// Das Ergebnis ist ein "Promise" (ein Versprechen auf einen späteren Wert).
//
// axios ist eine kleine Bibliothek, die solche HTTP-Anfragen vereinfacht.
import axios from 'axios';
import type {
  Project,
  Person,
  DynamicRule,
  NewDynamicRule,
  StandardRuleKey,
  Building,
  TableData,
  ImportValidationResult,
  AssignmentResult,
  Bed,
  SystemVersionInfo,
  UpdateStatusInfo,
} from '../types';

// Basis-Adresse des Servers. Normalerweise "/api" (gleicher Server wie die Webseite).
// Über die Umgebungsvariable VITE_API_URL kann man beim Bauen eine andere Adresse setzen.
const API_BASE = import.meta.env.VITE_API_URL || '/api';

export const api = {
  // --------------------------------------------------------------------------
  // Projekte
  // --------------------------------------------------------------------------
  getProjects: async (): Promise<Project[]> => {
    const res = await axios.get(`${API_BASE}/projects`);
    return res.data;
  },

  getProject: async (id: string): Promise<Project> => {
    const res = await axios.get(`${API_BASE}/projects/${id}`);
    return res.data;
  },

  createProject: async (project: Partial<Project>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects`, project);
    return res.data;
  },

  updateProject: async (id: string, project: Project): Promise<Project> => {
    const res = await axios.put(`${API_BASE}/projects/${id}`, project);
    return res.data;
  },

  deleteProject: async (id: string): Promise<void> => {
    await axios.delete(`${API_BASE}/projects/${id}`);
  },

  // --------------------------------------------------------------------------
  // Gebäude, Räume und Betten
  // --------------------------------------------------------------------------
  addBuilding: async (projectId: string, building: Partial<Building>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings`, building);
    return res.data;
  },

  deleteBuilding: async (projectId: string, buildingId: string): Promise<Project> => {
    const res = await axios.delete(`${API_BASE}/projects/${projectId}/buildings/${buildingId}`);
    return res.data;
  },

  addRoom: async (projectId: string, buildingId: string, room: any): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings/${buildingId}/rooms`, room);
    return res.data;
  },

  deleteRoom: async (projectId: string, buildingId: string, roomId: string): Promise<Project> => {
    const res = await axios.delete(`${API_BASE}/projects/${projectId}/buildings/${buildingId}/rooms/${roomId}`);
    return res.data;
  },

  addBed: async (projectId: string, buildingId: string, roomId: string, bed: Partial<Bed>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings/${buildingId}/rooms/${roomId}/beds`, bed);
    return res.data;
  },

  deleteBed: async (projectId: string, buildingId: string, roomId: string, bedId: string): Promise<Project> => {
    const res = await axios.delete(`${API_BASE}/projects/${projectId}/buildings/${buildingId}/rooms/${roomId}/beds/${bedId}`);
    return res.data;
  },

  // --------------------------------------------------------------------------
  // Gebäude-Vorlagen (wiederverwendbare Gebäude)
  // --------------------------------------------------------------------------
  getBuildingTemplates: async (): Promise<Array<{ id: string; name: string; source: string; roomCount: number; bedCount: number; building: Building }>> => {
    const res = await axios.get(`${API_BASE}/building-templates`);
    return res.data;
  },

  saveBuildingTemplate: async (building: Building): Promise<Building> => {
    const res = await axios.post(`${API_BASE}/building-templates`, building);
    return res.data;
  },

  deleteBuildingTemplate: async (templateId: string): Promise<void> => {
    await axios.delete(`${API_BASE}/building-templates/${templateId}`);
  },

  importBuildingToProject: async (projectId: string, sourceBuilding: Building): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings/import-template`, sourceBuilding);
    return res.data;
  },

  // --------------------------------------------------------------------------
  // Teilnehmer
  // --------------------------------------------------------------------------
  addPerson: async (projectId: string, person: Partial<Person>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/persons`, person);
    return res.data;
  },

  /** Mehrere Personen auf einmal speichern (letzter Schritt des Import-Assistenten) */
  savePersons: async (projectId: string, persons: Person[]): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/import/save`, persons);
    return res.data;
  },

  updatePerson: async (projectId: string, personId: string, person: Person): Promise<Project> => {
    const res = await axios.put(`${API_BASE}/projects/${projectId}/persons/${personId}`, person);
    return res.data;
  },

  deletePerson: async (projectId: string, personId: string): Promise<Project> => {
    const res = await axios.delete(`${API_BASE}/projects/${projectId}/persons/${personId}`);
    return res.data;
  },

  // --------------------------------------------------------------------------
  // Regeln
  // --------------------------------------------------------------------------
  /** Neue Regel anlegen – OHNE ID, die eindeutige ID vergibt der Server. */
  addRule: async (projectId: string, rule: NewDynamicRule): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/rules`, rule);
    return res.data;
  },

  updateRule: async (projectId: string, ruleId: string, rule: DynamicRule): Promise<Project> => {
    const res = await axios.put(`${API_BASE}/projects/${projectId}/rules/${ruleId}`, rule);
    return res.data;
  },

  deleteRule: async (projectId: string, ruleId: string): Promise<Project> => {
    const res = await axios.delete(`${API_BASE}/projects/${projectId}/rules/${ruleId}`);
    return res.data;
  },

  /**
   * Schaltet GENAU EINE der 4 Standard-Regeln an oder aus.
   * Gibt es sie im Projekt noch nicht, legt der Server sie automatisch an.
   */
  setStandardRuleActive: async (projectId: string, key: StandardRuleKey, active: boolean): Promise<Project> => {
    const res = await axios.put(`${API_BASE}/projects/${projectId}/rules/standard/${key}`, { active });
    return res.data;
  },

  // --------------------------------------------------------------------------
  // Excel/CSV-Import
  // --------------------------------------------------------------------------
  /** Datei hochladen und die ersten Zeilen als Vorschau bekommen */
  parseTable: async (projectId: string, file: File, headerRowIndex: number = 0, maxRows: number = 10): Promise<TableData> => {
    // FormData = "Formular" mit Datei, wie bei einem Upload-Formular auf einer Webseite
    const formData = new FormData();
    formData.append('file', file);
    formData.append('headerRowIndex', headerRowIndex.toString());
    formData.append('maxRows', maxRows.toString());
    const res = await axios.post(`${API_BASE}/projects/${projectId}/import/preview`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  /** Komplette Datei mit der gewählten Spaltenzuordnung in Personen umwandeln (noch NICHT speichern) */
  mapColumns: async (projectId: string, file: File, columnMapping: Record<string, string>, headerRowIndex: number = 0): Promise<ImportValidationResult> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('headerRowIndex', headerRowIndex.toString());
    formData.append('columnMapping', JSON.stringify(columnMapping));
    const res = await axios.post(`${API_BASE}/projects/${projectId}/import/map`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  processImport: async (projectId: string, mappingRequest: any): Promise<ImportValidationResult> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/import/process`, mappingRequest);
    return res.data;
  },

  // --------------------------------------------------------------------------
  // Belegung berechnen & exportieren
  // --------------------------------------------------------------------------
  calculateAssignment: async (projectId: string): Promise<AssignmentResult> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/assignment/calculate`);
    return res.data;
  },

  /** Lädt die Excel-Datei und löst im Browser den Download aus */
  exportExcel: async (projectId: string, projectName: string): Promise<void> => {
    const res = await axios.get(`${API_BASE}/projects/${projectId}/assignment/export/excel`, {
      responseType: 'blob', // Antwort ist eine Datei, kein JSON
    });
    // Trick für Downloads: unsichtbaren Link erzeugen, anklicken, wieder entfernen
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Belegungsplan_${projectName.replace(/[^a-zA-Z0-9_-]/g, '_')}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // --------------------------------------------------------------------------
  // System: Version & automatisches Update (nur Windows-Desktop-Version)
  // --------------------------------------------------------------------------
  getVersionInfo: async (): Promise<SystemVersionInfo> => {
    const res = await axios.get(`${API_BASE}/system/version`);
    return res.data;
  },

  triggerUpdate: async (): Promise<{ status: string; message: string }> => {
    const res = await axios.post(`${API_BASE}/system/update`);
    return res.data;
  },

  getUpdateStatus: async (): Promise<UpdateStatusInfo> => {
    const res = await axios.get(`${API_BASE}/system/update-status`);
    return res.data;
  },
};
