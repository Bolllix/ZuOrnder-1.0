import React, { useEffect, useState } from 'react';
import type { Project } from './types';
import { api } from './services/api';
import { Navbar } from './components/Navbar';
import { BuildingManager } from './components/BuildingManager';
import { ParticipantManager } from './components/ParticipantManager';
import { ImportWizard } from './components/ImportWizard';
import { RuleEditor } from './components/RuleEditor';
import { AssignmentDashboard } from './components/AssignmentDashboard';
import { ProjectSelectorModal } from './components/ProjectSelectorModal';

export const App: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [activeTab, setActiveTab] = useState<string>('assignment');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchProjects = async () => {
    try {
      const data = await api.getProjects();
      setProjects(data);
      if (data.length > 0) {
        if (!currentProject) {
          setCurrentProject(data[0]);
        } else {
          const fresh = data.find((p) => p.id === currentProject.id);
          if (fresh) setCurrentProject(fresh);
          else setCurrentProject(data[0]);
        }
      } else {
        setCurrentProject(null);
      }
    } catch (e) {
      console.error('Fehler beim Laden der Projekte:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleProjectUpdate = (updated: Project) => {
    setCurrentProject(updated);
    setProjects((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
  };

  const handleProjectDeleted = (deletedId: string) => {
    const remaining = projects.filter((p) => p.id !== deletedId);
    setProjects(remaining);
    if (currentProject?.id === deletedId) {
      setCurrentProject(remaining.length > 0 ? remaining[0] : null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-indigo-500 selection:text-white">
      {/* Navigation */}
      <Navbar
        currentProject={currentProject}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenProjectModal={() => setIsModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loading ? (
          <div className="flex items-center justify-center h-64 text-slate-400">
            Lade Daten...
          </div>
        ) : !currentProject ? (
          <div className="text-center py-20 space-y-4">
            <h2 className="text-xl font-bold text-slate-200">Kein Projekt vorhanden</h2>
            <button
              onClick={() => setIsModalOpen(true)}
              className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm rounded-xl shadow-lg shadow-indigo-500/20"
            >
              Projekt erstellen
            </button>
          </div>
        ) : (
          <>
            {activeTab === 'buildings' && (
              <BuildingManager project={currentProject} onProjectUpdate={handleProjectUpdate} />
            )}
            {activeTab === 'persons' && (
              <ParticipantManager project={currentProject} onProjectUpdate={handleProjectUpdate} />
            )}
            {activeTab === 'import' && (
              <ImportWizard
                project={currentProject}
                onProjectUpdate={handleProjectUpdate}
                onComplete={() => setActiveTab('persons')}
              />
            )}
            {activeTab === 'rules' && (
              <RuleEditor project={currentProject} onProjectUpdate={handleProjectUpdate} />
            )}
            {activeTab === 'assignment' && (
              <AssignmentDashboard project={currentProject} onProjectUpdate={handleProjectUpdate} />
            )}
          </>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 py-6 text-center text-xs text-slate-500">
        ZuORDNER • Automatische Zimmer- & Bettenzuordnung für Jugendherbergen
      </footer>

      {/* Project Switcher Modal */}
      {isModalOpen && (
        <ProjectSelectorModal
          projects={projects}
          currentProjectId={currentProject?.id || null}
          onSelectProject={(p) => setCurrentProject(p)}
          onClose={() => setIsModalOpen(false)}
          onProjectCreated={(newP) => {
            setProjects([...projects, newP]);
            setCurrentProject(newP);
          }}
          onProjectDeleted={handleProjectDeleted}
        />
      )}
    </div>
  );
};

export default App;
