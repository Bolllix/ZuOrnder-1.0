import React from 'react';
import type { Project } from '../types';
import { Building2, Users, FileSpreadsheet, SlidersHorizontal, Sparkles, FolderOpen } from 'lucide-react';

interface NavbarProps {
  currentProject: Project | null;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onOpenProjectModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentProject,
  activeTab,
  setActiveTab,
  onOpenProjectModal
}) => {
  const tabs = [
    { id: 'buildings', label: 'Gebäude & Betten', icon: Building2, count: currentProject?.buildings?.length || 0 },
    { id: 'persons', label: 'Teilnehmer', icon: Users, count: currentProject?.persons?.length || 0 },
    { id: 'import', label: 'Excel/CSV Import', icon: FileSpreadsheet },
    { id: 'rules', label: 'Regel-Editor', icon: SlidersHorizontal, count: currentProject?.rules?.length || 0 },
    { id: 'assignment', label: 'Belegungs-Optimierung', icon: Sparkles, highlight: true },
  ];

  return (
    <header className="sticky top-0 z-50 glass-panel border-b border-slate-700/50 bg-slate-900/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Project Switcher */}
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 cursor-pointer" onClick={() => setActiveTab('buildings')}>
              <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-indigo-500/30">
                Z
              </div>
              <span className="text-xl font-bold bg-gradient-to-r from-indigo-400 to-violet-300 bg-clip-text text-transparent">
                ZuORDNER
              </span>
            </div>

            <div className="h-6 w-px bg-slate-700 mx-2" />

            <button
              onClick={onOpenProjectModal}
              className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium border border-slate-700 transition"
            >
              <FolderOpen className="w-4 h-4 text-indigo-400" />
              <span className="max-w-[180px] truncate">{currentProject ? currentProject.name : 'Projekt wählen'}</span>
            </button>
          </div>

          {/* Navigation Tabs */}
          <nav className="flex items-center space-x-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive
                      ? tab.highlight
                        ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                        : 'bg-slate-800 text-indigo-400 border border-indigo-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <Icon className={`w-4 h-4 ${isActive ? (tab.highlight ? 'text-white' : 'text-indigo-400') : 'text-slate-400'}`} />
                  <span>{tab.label}</span>
                  {tab.count !== undefined && (
                    <span className={`ml-1.5 px-1.5 py-0.5 text-xs rounded-full ${isActive ? 'bg-indigo-500/30 text-indigo-200' : 'bg-slate-800 text-slate-400'}`}>
                      {tab.count}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>
    </header>
  );
};
