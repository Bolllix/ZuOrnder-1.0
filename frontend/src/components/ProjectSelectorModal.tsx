import React, { useState } from 'react';
import type { Project } from '../types';
import { api } from '../services/api';
import { FolderOpen, Plus, X, Trash2 } from 'lucide-react';

interface ProjectSelectorModalProps {
  projects: Project[];
  currentProjectId: string | null;
  onSelectProject: (project: Project) => void;
  onClose: () => void;
  onProjectCreated: (newProject: Project) => void;
  onProjectDeleted: (deletedId: string) => void;
}

export const ProjectSelectorModal: React.FC<ProjectSelectorModalProps> = ({
  projects,
  currentProjectId,
  onSelectProject,
  onClose,
  onProjectCreated,
  onProjectDeleted,
}) => {
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const handleCreate = async () => {
    if (!name.trim()) return;
    try {
      const created = await api.createProject({ name: name.trim(), description: description.trim(), buildings: [], persons: [], rules: [] });
      onProjectCreated(created);
      onSelectProject(created);
      onClose();
    } catch (e) {
      alert('Fehler beim Erstellen des Projekts.');
    }
  };

  const handleDeleteProject = async (id: string, name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm(`Möchten Sie das Projekt '${name}' wirklich dauerhaft löschen?`)) return;
    try {
      await api.deleteProject(id);
      onProjectDeleted(id);
    } catch (err) {
      alert('Fehler beim Löschen des Projekts.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-panel p-6 rounded-2xl border border-slate-700 max-w-lg w-full space-y-5 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
          <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            <FolderOpen className="w-5 h-5 text-indigo-400" /> Projekt wählen oder löschen
          </h3>
          <button onClick={onClose} className="p-1 text-slate-400 hover:text-slate-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        {!showCreate ? (
          <div className="space-y-4">
            <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
              {projects.map((p) => {
                const isCurrent = p.id === currentProjectId;
                return (
                  <div
                    key={p.id}
                    className={`w-full p-3.5 rounded-xl border transition flex items-center justify-between ${
                      isCurrent
                        ? 'bg-indigo-600/20 border-indigo-500 text-slate-100'
                        : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <button
                      onClick={() => {
                        onSelectProject(p);
                        onClose();
                      }}
                      className="flex-1 text-left truncate"
                    >
                      <div className="font-semibold text-sm truncate">{p.name}</div>
                      <div className="text-xs text-slate-400 mt-0.5 truncate">{p.description || 'Keine Beschreibung'}</div>
                    </button>

                    <div className="flex items-center space-x-3 ml-3">
                      <div className="text-right text-xs text-slate-400 space-y-0.5">
                        <div>{p.buildings?.length || 0} Gebäude</div>
                        <div>{p.persons?.length || 0} Teilnehmer</div>
                      </div>

                      <button
                        onClick={(e) => handleDeleteProject(p.id, p.name, e)}
                        title="Projekt löschen"
                        className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <button
              onClick={() => setShowCreate(true)}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm rounded-xl flex items-center justify-center space-x-2 shadow-md"
            >
              <Plus className="w-4 h-4" /> <span>Neues Projekt anlegen</span>
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Projekt-Name *</label>
              <input
                type="text"
                placeholder="z.B. Sommerlager 2026"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Beschreibung</label>
              <input
                type="text"
                placeholder="Kurze Notiz oder Ort"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
              />
            </div>

            <div className="flex justify-end space-x-2 pt-2">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 bg-slate-800 text-slate-400 text-xs rounded-lg">
                Zurück
              </button>
              <button onClick={handleCreate} className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded-lg">
                Projekt erstellen
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
