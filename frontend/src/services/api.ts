import axios from 'axios';
import type { Project, Person, DynamicRule, Building, TableData, ImportValidationResult, AssignmentResult } from '../types';

const API_BASE = 'http://localhost:8080/api';

export const api = {
  // Projects
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

  // Buildings & Rooms
  addBuilding: async (projectId: string, building: Partial<Building>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings`, building);
    return res.data;
  },

  addRoom: async (projectId: string, buildingId: string, room: any): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/buildings/${buildingId}/rooms`, room);
    return res.data;
  },

  // Persons
  addPerson: async (projectId: string, person: Partial<Person>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/persons`, person);
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

  // Rules
  addRule: async (projectId: string, rule: Partial<DynamicRule>): Promise<Project> => {
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

  // Import
  getSheets: async (file: File): Promise<string[]> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axios.post(`${API_BASE}/import/sheets`, formData);
    return res.data;
  },

  getPreview: async (file: File, sheetName?: string, headerRowIndex: number = 0, maxRows: number = 10): Promise<TableData> => {
    const formData = new FormData();
    formData.append('file', file);
    if (sheetName) formData.append('sheetName', sheetName);
    formData.append('headerRowIndex', headerRowIndex.toString());
    formData.append('maxRows', maxRows.toString());
    const res = await axios.post(`${API_BASE}/import/preview`, formData);
    return res.data;
  },

  mapColumns: async (file: File, mapping: Record<string, string>, sheetName?: string, headerRowIndex: number = 0): Promise<ImportValidationResult> => {
    const formData = new FormData();
    formData.append('file', file);
    if (sheetName) formData.append('sheetName', sheetName);
    formData.append('headerRowIndex', headerRowIndex.toString());
    
    const res = await axios.post(`${API_BASE}/import/map`, mapping, {
      params: { sheetName, headerRowIndex },
      headers: { 'Content-Type': 'application/json' },
    });
    return res.data;
  },

  importToProject: async (projectId: string, persons: Person[]): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/import/project/${projectId}`, persons);
    return res.data;
  },

  // Optimization
  calculateAssignment: async (projectId: string): Promise<AssignmentResult> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/assignment/calculate`);
    return res.data;
  },

  manualOverride: async (projectId: string, personToBedMap: Record<string, string>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/assignment/override`, personToBedMap);
    return res.data;
  }
};
