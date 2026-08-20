import axios from 'axios';
import type { Project, Person, DynamicRule, Building, TableData, ImportValidationResult, AssignmentResult, Bed } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

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

  // Buildings & Rooms & Beds
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

  // Persons
  addPerson: async (projectId: string, person: Partial<Person>): Promise<Project> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/persons`, person);
    return res.data;
  },

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

  // Rules
  addRule: async (projectId: string, rule: DynamicRule): Promise<Project> => {
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

  // Import Pipeline
  parseTable: async (projectId: string, file: File, headerRowIndex: number = 0, maxRows: number = 10): Promise<TableData> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('headerRowIndex', headerRowIndex.toString());
    formData.append('maxRows', maxRows.toString());
    const res = await axios.post(`${API_BASE}/projects/${projectId}/import/preview`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

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

  // Optimization Assignment Engine
  calculateAssignment: async (projectId: string): Promise<AssignmentResult> => {
    const res = await axios.post(`${API_BASE}/projects/${projectId}/assignment/calculate`);
    return res.data;
  },

  exportExcel: async (projectId: string, projectName: string): Promise<void> => {
    const res = await axios.get(`${API_BASE}/projects/${projectId}/assignment/export/excel`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Belegungsplan_${projectName.replace(/[^a-zA-Z0-9_-]/g, '_')}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
