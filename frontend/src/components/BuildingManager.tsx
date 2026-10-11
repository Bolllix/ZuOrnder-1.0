import React, { useState, useRef } from 'react';
import type { Project, BedType, Bed, Building } from '../types';
import { api } from '../services/api';
import { Building2, Plus, Bed as BedIcon, Trash2, AlertCircle, Bookmark, Check, FolderDown, X, Copy } from 'lucide-react';

interface BuildingManagerProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

export const BuildingManager: React.FC<BuildingManagerProps> = ({ project, onProjectUpdate }) => {
  const [newBuildingName, setNewBuildingName] = useState('');
  const [buildingNameError, setBuildingNameError] = useState<string | null>(null);
  const buildingInputRef = useRef<HTMLInputElement>(null);

  const [selectedBuildingId, setSelectedBuildingId] = useState<string | null>(
    project.buildings.length > 0 ? project.buildings[0].id : null
  );

  // Template / Reusable Buildings State
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [availableTemplates, setAvailableTemplates] = useState<Array<{ id: string; name: string; source: string; roomCount: number; bedCount: number; building: Building }>>([]);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [savedTemplateSuccessId, setSavedTemplateSuccessId] = useState<string | null>(null);
  const [importingId, setImportingId] = useState<string | null>(null);

  // New Room Form State
  const [newRoomName, setNewRoomName] = useState('');
  const [newRoomFloor, setNewRoomFloor] = useState(0);
  const [isGirlsRoom, setIsGirlsRoom] = useState(false);
  const [isBoysRoom, setIsBoysRoom] = useState(false);
  const [bedSetupType, setBedSetupType] = useState<'SINGLE' | 'DOUBLE' | 'BUNK' | 'CUSTOM'>('BUNK');

  // Single Bed Add Modal State
  const [addingBedRoomId, setAddingBedRoomId] = useState<string | null>(null);
  const [singleBedName, setSingleBedName] = useState('');
  const [singleBedType, setSingleBedType] = useState<BedType>('SINGLE');

  const handleAddBuilding = async () => {
    if (!newBuildingName.trim()) {
      setBuildingNameError('Bitte gib zuerst einen Namen für das Gebäude ein!');
      if (buildingInputRef.current) {
        buildingInputRef.current.focus();
      }
      return;
    }

    setBuildingNameError(null);
    try {
      const updated = await api.addBuilding(project.id, { name: newBuildingName.trim(), rooms: [] });
      onProjectUpdate(updated);
      setNewBuildingName('');
      if (updated.buildings.length > 0) {
        setSelectedBuildingId(updated.buildings[updated.buildings.length - 1].id);
      }
    } catch (e) {
      alert('Fehler beim Erstellen des Gebäudes.');
    }
  };

  const handleDeleteBuilding = async (buildingId: string, name: string) => {
    if (!confirm(`Möchten Sie das Gebäude '${name}' wirklich löschen?`)) return;
    try {
      const updatedProject = await api.deleteBuilding(project.id, buildingId);
      onProjectUpdate(updatedProject);
      if (selectedBuildingId === buildingId) {
        setSelectedBuildingId(updatedProject.buildings.length > 0 ? updatedProject.buildings[0].id : null);
      }
    } catch (e) {
      alert('Fehler beim Löschen des Gebäudes.');
    }
  };

  // Template / Reusable Buildings Handlers
  const handleOpenTemplateModal = async () => {
    setShowTemplateModal(true);
    setLoadingTemplates(true);
    try {
      const list = await api.getBuildingTemplates();
      setAvailableTemplates(list);
    } catch (e) {
      alert('Fehler beim Laden der Gebäude-Vorlagen.');
    } finally {
      setLoadingTemplates(false);
    }
  };

  const handleSaveAsTemplate = async (building: Building) => {
    try {
      await api.saveBuildingTemplate(building);
      setSavedTemplateSuccessId(building.id);
      setTimeout(() => setSavedTemplateSuccessId(null), 3500);
    } catch (e) {
      alert('Fehler beim Speichern der Vorlage.');
    }
  };

  const handleImportBuilding = async (item: { id: string; building: Building }) => {
    setImportingId(item.id);
    try {
      const updated = await api.importBuildingToProject(project.id, item.building);
      onProjectUpdate(updated);
      setShowTemplateModal(false);
      if (updated.buildings.length > 0) {
        setSelectedBuildingId(updated.buildings[updated.buildings.length - 1].id);
      }
    } catch (e) {
      alert('Fehler beim Übernehmen des Gebäudes.');
    } finally {
      setImportingId(null);
    }
  };

  const handleDeleteTemplate = async (templateId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('Möchten Sie diese Gebäude-Vorlage wirklich aus der Bibliothek löschen?')) return;
    try {
      await api.deleteBuildingTemplate(templateId);
      setAvailableTemplates(prev => prev.filter(t => t.id !== templateId));
    } catch (e) {
      alert('Fehler beim Löschen der Vorlage.');
    }
  };

  const handleAddRoom = async () => {
    if (!selectedBuildingId || !newRoomName.trim()) return;
    try {
      const beds = [];
      if (bedSetupType === 'SINGLE') {
        beds.push({ name: `Einzelbett 1`, bedType: 'SINGLE' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
        beds.push({ name: `Einzelbett 2`, bedType: 'SINGLE' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
      } else if (bedSetupType === 'DOUBLE') {
        beds.push({ name: `Doppelbett 1A`, bedType: 'DOUBLE' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
        beds.push({ name: `Doppelbett 1B`, bedType: 'DOUBLE' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
      } else if (bedSetupType === 'BUNK') {
        beds.push({ name: `Etagenbett 1 Unten`, bedType: 'BOTTOM_BUNK' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
        beds.push({ name: `Etagenbett 1 Oben`, bedType: 'TOP_BUNK' as BedType, roomName: newRoomName.trim(), floor: newRoomFloor, buildingName: '' });
      }

      const roomPayload = {
        name: newRoomName.trim(),
        floor: newRoomFloor,
        girlsRoom: isGirlsRoom,
        boysRoom: isBoysRoom,
        beds
      };

      const updated = await api.addRoom(project.id, selectedBuildingId, roomPayload);
      onProjectUpdate(updated);
      setNewRoomName('');
    } catch (e) {
      alert('Fehler beim Hinzufügen des Raums.');
    }
  };

  const handleDeleteRoom = async (buildingId: string, roomId: string, roomName: string) => {
    if (!confirm(`Raum '${roomName}' löschen?`)) return;
    try {
      const updatedProject = await api.deleteRoom(project.id, buildingId, roomId);
      onProjectUpdate(updatedProject);
    } catch (e) {
      alert('Fehler beim Löschen des Raums.');
    }
  };

  const handleAddSingleBed = async () => {
    if (!selectedBuildingId || !addingBedRoomId || !singleBedName.trim()) return;
    try {
      const bedPayload: Partial<Bed> = {
        name: singleBedName.trim(),
        bedType: singleBedType,
      };

      const updated = await api.addBed(project.id, selectedBuildingId, addingBedRoomId, bedPayload);
      onProjectUpdate(updated);
      setAddingBedRoomId(null);
      setSingleBedName('');
    } catch (e) {
      alert('Fehler beim Hinzufügen des Betts.');
    }
  };

  const handleDeleteBed = async (buildingId: string, roomId: string, bedId: string, bedName: string) => {
    if (!confirm(`Bett '${bedName}' wirklich löschen?`)) return;
    try {
      const updated = await api.deleteBed(project.id, buildingId, roomId, bedId);
      onProjectUpdate(updated);
    } catch (e) {
      alert('Fehler beim Löschen des Betts.');
    }
  };

  const selectedBuilding = project.buildings.find((b) => b.id === selectedBuildingId);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <Building2 className="w-7 h-7 text-indigo-400" /> Gebäude- & Bettenverwaltung
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Gebäude, Räume und einzelne Betten konfigurieren oder aus bestehenden Vorlagen übernehmen.
          </p>
        </div>

        {/* Action Buttons: Add Building or Import from Template */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full lg:w-auto">
          <button
            onClick={handleOpenTemplateModal}
            className="flex items-center justify-center space-x-1.5 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-indigo-300 hover:text-indigo-200 border border-indigo-500/30 text-sm font-semibold rounded-lg shadow transition whitespace-nowrap"
          >
            <FolderDown className="w-4 h-4 text-indigo-400" />
            <span>Aus Vorlage / anderem Projekt</span>
          </button>

          <div className="flex flex-col items-end space-y-1 w-full sm:w-auto">
            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <input
                ref={buildingInputRef}
                type="text"
                placeholder="Neuer Gebäudename..."
                value={newBuildingName}
                onChange={(e) => {
                  setNewBuildingName(e.target.value);
                  if (e.target.value.trim()) setBuildingNameError(null);
                }}
                onKeyDown={(e) => e.key === 'Enter' && handleAddBuilding()}
                className={`px-3 py-2 bg-slate-800 border rounded-lg text-slate-200 text-sm focus:outline-none transition w-full sm:w-56 ${
                  buildingNameError ? 'border-rose-500 ring-2 ring-rose-500/30' : 'border-slate-700 focus:border-indigo-500'
                }`}
              />
              <button
                onClick={handleAddBuilding}
                className="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-lg shadow-md shadow-indigo-500/20 transition whitespace-nowrap"
              >
                <Plus className="w-4 h-4" /> <span>Anlegen</span>
              </button>
            </div>

            {buildingNameError && (
              <span className="text-xs font-semibold text-rose-400 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" /> {buildingNameError}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Buildings Tabs & Main Section */}
      {project.buildings.length === 0 ? (
        <div className="p-12 glass-panel rounded-2xl text-center space-y-5 border border-slate-800">
          <div className="w-16 h-16 bg-indigo-600/20 rounded-full flex items-center justify-center mx-auto text-indigo-400">
            <Building2 className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-slate-200">Noch keine Gebäude in diesem Projekt</h3>
            <p className="text-slate-400 text-xs max-w-md mx-auto mt-1">
              Erstelle ein neues Gebäude oder übernimm ein bereits vorhandenes Gebäude aus einem deiner anderen Projekte oder Vorlagen!
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <button
              onClick={handleOpenTemplateModal}
              className="flex items-center space-x-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-xl shadow-lg shadow-indigo-500/20 transition"
            >
              <FolderDown className="w-4 h-4" />
              <span>Gebäude aus Vorlage / anderem Projekt laden</span>
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Building Selection List */}
          <div className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Gebäude ({project.buildings.length})</h3>
            </div>

            {project.buildings.map((b) => {
              const isSelected = b.id === selectedBuildingId;
              const totalBeds = b.rooms.reduce((acc, r) => acc + r.beds.length, 0);
              const isSaved = savedTemplateSuccessId === b.id;

              return (
                <div
                  key={b.id}
                  className={`w-full p-3 rounded-xl transition flex items-center justify-between border ${
                    isSelected
                      ? 'bg-slate-800 border-indigo-500 text-slate-100 shadow-md'
                      : 'glass-panel border-slate-800 text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                  }`}
                >
                  <button
                    onClick={() => setSelectedBuildingId(b.id)}
                    className="flex-1 text-left flex items-center space-x-3 truncate"
                  >
                    <Building2 className={`w-5 h-5 flex-shrink-0 ${isSelected ? 'text-indigo-400' : 'text-slate-500'}`} />
                    <div className="truncate">
                      <div className="font-medium text-sm text-slate-100 truncate">{b.name}</div>
                      <div className="text-xs text-slate-500">{b.rooms.length} Räume • {totalBeds} Betten</div>
                    </div>
                  </button>

                  <div className="flex items-center space-x-1 ml-2">
                    {/* Save as Template Bookmark Button */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSaveAsTemplate(b);
                      }}
                      title="Als Vorlage für andere Projekte in Bibliothek speichern"
                      className={`p-1.5 rounded-lg transition ${
                        isSaved
                          ? 'text-emerald-400 bg-emerald-500/20'
                          : 'text-slate-500 hover:text-indigo-400 hover:bg-slate-700/50'
                      }`}
                    >
                      {isSaved ? <Check className="w-4 h-4" /> : <Bookmark className="w-4 h-4" />}
                    </button>

                    <button
                      onClick={() => handleDeleteBuilding(b.id, b.name)}
                      title="Gebäude löschen"
                      className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-700/50 rounded-lg transition"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Rooms and Individual Beds Details Area */}
          <div className="lg:col-span-3 space-y-6">
            {selectedBuilding && (
              <>
                {/* Create Room Form */}
                <div className="glass-panel p-5 rounded-2xl border border-slate-700/60 space-y-4">
                  <h3 className="text-md font-semibold text-slate-200 flex items-center gap-2">
                    <Plus className="w-4 h-4 text-indigo-400" /> Neuen Raum in '{selectedBuilding.name}' anlegen
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Raumname / Nummer *</label>
                      <input
                        type="text"
                        placeholder="z.B. Raum 101"
                        value={newRoomName}
                        onChange={(e) => setNewRoomName(e.target.value)}
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-indigo-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Stockwerk / Etage</label>
                      <input
                        type="number"
                        value={newRoomFloor}
                        onChange={(e) => setNewRoomFloor(parseInt(e.target.value) || 0)}
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-indigo-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Betten-Vorkonfiguration</label>
                      <select
                        value={bedSetupType}
                        onChange={(e: any) => setBedSetupType(e.target.value)}
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-indigo-500"
                      >
                        <option value="BUNK">1x Etagenbett (2 Plätze)</option>
                        <option value="SINGLE">2x Einzelbetten (2 Plätze)</option>
                        <option value="DOUBLE">1x Doppelbett (2 Plätze)</option>
                        <option value="CUSTOM">Leer (Betten einzeln hinzufügen)</option>
                      </select>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <div className="flex items-center space-x-6">
                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={isGirlsRoom}
                          onChange={(e) => {
                            setIsGirlsRoom(e.target.checked);
                            if (e.target.checked) setIsBoysRoom(false);
                          }}
                          className="rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-0"
                        />
                        <span>Mädchenzimmer (Präferenz)</span>
                      </label>

                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={isBoysRoom}
                          onChange={(e) => {
                            setIsBoysRoom(e.target.checked);
                            if (e.target.checked) setIsGirlsRoom(false);
                          }}
                          className="rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-0"
                        />
                        <span>Jungenzimmer (Präferenz)</span>
                      </label>
                    </div>

                    <button
                      onClick={handleAddRoom}
                      className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg shadow transition"
                    >
                      Raum speichern
                    </button>
                  </div>
                </div>

                {/* Rooms List */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
                      Räume in '{selectedBuilding.name}' ({selectedBuilding.rooms.length})
                    </h4>
                  </div>

                  {selectedBuilding.rooms.length === 0 ? (
                    <div className="p-8 border border-dashed border-slate-800 rounded-2xl text-center text-slate-500 text-xs">
                      Noch keine Räume in diesem Gebäude vorhanden. Lege oben den ersten Raum an.
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {selectedBuilding.rooms.map((room) => (
                        <div key={room.id} className="glass-panel p-4 rounded-xl border border-slate-800 space-y-3">
                          <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
                            <div>
                              <div className="font-semibold text-slate-100 text-sm flex items-center space-x-2">
                                <span>{room.name}</span>
                                {room.girlsRoom && (
                                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-pink-500/20 text-pink-300 border border-pink-500/30">
                                    Mädchen
                                  </span>
                                )}
                                {room.boysRoom && (
                                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                                    Jungen
                                  </span>
                                )}
                              </div>
                              <span className="text-xs text-slate-500">Etage {room.floor} • {room.beds.length} Betten</span>
                            </div>

                            <button
                              onClick={() => handleDeleteRoom(selectedBuilding.id, room.id, room.name)}
                              title="Raum löschen"
                              className="p-1 text-slate-500 hover:text-rose-400 transition"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>

                          {/* Beds in Room */}
                          <div className="space-y-1.5">
                            {room.beds.map((bed) => (
                              <div
                                key={bed.id}
                                className="flex items-center justify-between p-2 rounded-lg bg-slate-900/60 border border-slate-800/60 text-xs"
                              >
                                <div className="flex items-center space-x-2">
                                  <BedIcon className="w-3.5 h-3.5 text-indigo-400" />
                                  <span className="text-slate-300">{bed.name}</span>
                                  <span className="text-[10px] text-slate-500 font-mono">({bed.bedType})</span>
                                </div>
                                <button
                                  onClick={() => handleDeleteBed(selectedBuilding.id, room.id, bed.id, bed.name)}
                                  title="Bett entfernen"
                                  className="text-slate-600 hover:text-rose-400 transition p-0.5"
                                >
                                  <Trash2 className="w-3 h-3" />
                                </button>
                              </div>
                            ))}
                          </div>

                          {/* Add Single Bed Button / Form */}
                          {addingBedRoomId === room.id ? (
                            <div className="p-2.5 bg-slate-900 rounded-lg border border-indigo-500/40 space-y-2 mt-2">
                              <div className="text-[11px] font-semibold text-indigo-300">Neues Einzelbett hinzufügen:</div>
                              <div className="grid grid-cols-2 gap-2">
                                <input
                                  type="text"
                                  placeholder="Bettname (z.B. Bett 3)"
                                  value={singleBedName}
                                  onChange={(e) => setSingleBedName(e.target.value)}
                                  className="px-2 py-1 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs"
                                />
                                <select
                                  value={singleBedType}
                                  onChange={(e: any) => setSingleBedType(e.target.value)}
                                  className="px-2 py-1 bg-slate-800 border border-slate-700 rounded text-slate-200 text-xs"
                                >
                                  <option value="SINGLE">Einzelbett</option>
                                  <option value="DOUBLE">Doppelbett</option>
                                  <option value="TOP_BUNK">Hochbett (Oben)</option>
                                  <option value="BOTTOM_BUNK">Hochbett (Unten)</option>
                                  <option value="SOFA_BED">Schlafsofa</option>
                                </select>
                              </div>
                              <div className="flex justify-end space-x-2">
                                <button
                                  onClick={() => setAddingBedRoomId(null)}
                                  className="px-2 py-1 bg-slate-800 text-slate-400 hover:text-slate-200 text-[11px] rounded"
                                >
                                  Abbrechen
                                </button>
                                <button
                                  onClick={handleAddSingleBed}
                                  className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-500 text-white text-[11px] font-semibold rounded"
                                >
                                  Bett hinzufügen
                                </button>
                              </div>
                            </div>
                          ) : (
                            <button
                              onClick={() => {
                                setAddingBedRoomId(room.id);
                                setSingleBedName(`Bett ${room.beds.length + 1}`);
                              }}
                              className="w-full py-1.5 border border-dashed border-slate-700 hover:border-indigo-500 text-slate-400 hover:text-indigo-400 rounded-lg text-xs flex items-center justify-center space-x-1 transition"
                            >
                              <Plus className="w-3.5 h-3.5" />
                              <span>Bett hinzufügen</span>
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Modal: Import Building from Template or Another Project */}
      {showTemplateModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-panel p-6 rounded-2xl border border-indigo-500/40 max-w-2xl w-full space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
              <div className="flex items-center gap-2">
                <FolderDown className="w-5 h-5 text-indigo-400" />
                <h3 className="text-lg font-bold text-slate-100">Gebäude aus Vorlage oder anderem Projekt übernehmen</h3>
              </div>
              <button onClick={() => setShowTemplateModal(false)} className="p-1 text-slate-400 hover:text-slate-200 rounded-lg transition">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-400">
              Wähle ein gespeichertes Gebäude aus. Alle Zimmer und Betten werden 1:1 in dein aktuelles Projekt <b>{project.name}</b> kopiert!
            </p>

            {loadingTemplates ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                Lade verfügbare Gebäude...
              </div>
            ) : availableTemplates.length === 0 ? (
              <div className="py-10 text-center space-y-2 border border-dashed border-slate-800 rounded-xl">
                <div className="text-slate-400 text-sm font-medium">Keine Gebäude-Vorlagen gefunden</div>
                <p className="text-slate-500 text-xs max-w-sm mx-auto">
                  Du kannst in jedem deiner Projekte ein Gebäude mit dem Lesezeichen-Symbol (🔖) als dauerhafte Vorlage in der Bibliothek speichern.
                </p>
              </div>
            ) : (
              <div className="space-y-2.5 max-h-80 overflow-y-auto pr-1">
                {availableTemplates.map((item, idx) => (
                  <div
                    key={`${item.id}-${idx}`}
                    className="p-3.5 bg-slate-900/80 hover:bg-slate-800/80 border border-slate-800 hover:border-indigo-500/50 rounded-xl transition flex items-center justify-between group"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-lg bg-indigo-600/20 text-indigo-400 flex items-center justify-center font-bold">
                        <Building2 className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="font-semibold text-slate-200 text-sm flex items-center gap-2">
                          <span>{item.name}</span>
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-indigo-300 border border-slate-700">
                            {item.source}
                          </span>
                        </div>
                        <div className="text-xs text-slate-400 mt-0.5">
                          {item.roomCount} Räume • {item.bedCount} Betten
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2">
                      {item.source.includes('Bibliothek') && (
                        <button
                          onClick={(e) => handleDeleteTemplate(item.id, e)}
                          title="Aus Bibliothek löschen"
                          className="p-2 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}

                      <button
                        disabled={importingId === item.id}
                        onClick={() => handleImportBuilding(item)}
                        className="flex items-center space-x-1.5 px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-lg shadow transition disabled:opacity-50"
                      >
                        <Copy className="w-3.5 h-3.5" />
                        <span>{importingId === item.id ? 'Wird kopiert...' : 'Übernehmen'}</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="flex justify-end pt-2 border-t border-slate-800">
              <button
                onClick={() => setShowTemplateModal(false)}
                className="px-4 py-2 bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs font-medium rounded-lg transition"
              >
                Schließen
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
