import React, { useState, useRef } from 'react';
import type { Project, BedType } from '../types';
import { api } from '../services/api';
import { Building2, Plus, Bed as BedIcon, Trash2, AlertCircle } from 'lucide-react';

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
      const bedPayload = {
        name: singleBedName.trim(),
        bedType: singleBedType,
      };
      const updatedProject = await api.addBed(project.id, selectedBuildingId, addingBedRoomId, bedPayload);
      onProjectUpdate(updatedProject);
      setSingleBedName('');
      setAddingBedRoomId(null);
    } catch (e) {
      alert('Fehler beim Hinzufügen des Betts.');
    }
  };

  const handleDeleteBed = async (_buildingId: string, roomId: string, bedId: string) => {
    if (!selectedBuildingId) return;
    try {
      const updatedProject = await api.deleteBed(project.id, selectedBuildingId, roomId, bedId);
      onProjectUpdate(updatedProject);
    } catch (e) {
      alert('Fehler beim Löschen des Betts.');
    }
  };

  const selectedBuilding = project.buildings.find((b) => b.id === selectedBuildingId);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <Building2 className="w-7 h-7 text-indigo-400" /> Gebäude- & Bettenverwaltung
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Gebäude, Räume und einzelne Betten konfigurieren oder löschen.
          </p>
        </div>

        {/* Add Building Form */}
        <div className="flex flex-col items-end space-y-1 w-full sm:w-auto">
          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <input
              ref={buildingInputRef}
              type="text"
              placeholder="Gebäudename (z.B. Haupthaus, Haus A)"
              value={newBuildingName}
              onChange={(e) => {
                setNewBuildingName(e.target.value);
                if (e.target.value.trim()) setBuildingNameError(null);
              }}
              onKeyDown={(e) => e.key === 'Enter' && handleAddBuilding()}
              className={`px-3 py-2 bg-slate-800 border rounded-lg text-slate-200 text-sm focus:outline-none transition w-full sm:w-64 ${
                buildingNameError ? 'border-rose-500 ring-2 ring-rose-500/30' : 'border-slate-700 focus:border-indigo-500'
              }`}
            />
            <button
              onClick={handleAddBuilding}
              className="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold rounded-lg shadow-md shadow-indigo-500/20 transition whitespace-nowrap"
            >
              <Plus className="w-4 h-4" /> <span>Gebäude anlegen</span>
            </button>
          </div>

          {buildingNameError ? (
            <span className="text-xs font-semibold text-rose-400 flex items-center gap-1">
              <AlertCircle className="w-3.5 h-3.5" /> {buildingNameError}
            </span>
          ) : (
            <span className="text-[11px] text-slate-500">Name eingeben & "Gebäude anlegen" klicken.</span>
          )}
        </div>
      </div>

      {/* Buildings Tabs & Main Section */}
      {project.buildings.length === 0 ? (
        <div className="p-12 glass-panel rounded-2xl text-center space-y-4 border border-slate-800">
          <div className="w-16 h-16 bg-indigo-600/20 rounded-full flex items-center justify-center mx-auto text-indigo-400">
            <Building2 className="w-8 h-8" />
          </div>
          <h3 className="text-lg font-bold text-slate-200">Noch keine Gebäude vorhanden</h3>
          <p className="text-slate-400 text-xs max-w-md mx-auto">
            Trage oben rechts den Namen deines ersten Gebäudes ein (z. B. <i>Haupthaus</i> oder <i>Gebäude A</i>) und klicke auf <b>"Gebäude anlegen"</b>.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Building Selection List */}
          <div className="space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 px-1">Gebäude</h3>
            {project.buildings.map((b) => {
              const isSelected = b.id === selectedBuildingId;
              const totalBeds = b.rooms.reduce((acc, r) => acc + r.beds.length, 0);
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

                  <button
                    onClick={() => handleDeleteBuilding(b.id, b.name)}
                    title="Gebäude löschen"
                    className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-700/50 rounded-lg transition ml-2"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
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
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Etage (0 = EG, 1 = 1.OG)</label>
                      <input
                        type="number"
                        value={newRoomFloor}
                        onChange={(e) => setNewRoomFloor(parseInt(e.target.value) || 0)}
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-slate-400 mb-1">Start-Betten-Set</label>
                      <select
                        value={bedSetupType}
                        onChange={(e: any) => setBedSetupType(e.target.value)}
                        className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
                      >
                        <option value="BUNK">1x Etagenbett (2 Betten)</option>
                        <option value="SINGLE">2x Einzelbett</option>
                        <option value="DOUBLE">1x Doppelbett (2 Plätze)</option>
                        <option value="CUSTOM">Leerer Raum (Betten einzeln hinzufügen)</option>
                      </select>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-800">
                    <div className="flex items-center space-x-4">
                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={isGirlsRoom}
                          onChange={(e) => {
                            setIsGirlsRoom(e.target.checked);
                            if (e.target.checked) setIsBoysRoom(false);
                          }}
                          className="w-4 h-4 rounded text-pink-600 bg-slate-900 border-slate-700 focus:ring-pink-500"
                        />
                        <span>Mädchenzimmer</span>
                      </label>

                      <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={isBoysRoom}
                          onChange={(e) => {
                            setIsBoysRoom(e.target.checked);
                            if (e.target.checked) setIsGirlsRoom(false);
                          }}
                          className="w-4 h-4 rounded text-blue-600 bg-slate-900 border-slate-700 focus:ring-blue-500"
                        />
                        <span>Jungenzimmer</span>
                      </label>
                    </div>

                    <button
                      onClick={handleAddRoom}
                      className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg shadow-md transition"
                    >
                      Raum speichern
                    </button>
                  </div>
                </div>

                {/* Single Bed Add Drawer */}
                {addingBedRoomId && (
                  <div className="glass-panel p-4 rounded-xl border border-indigo-500/40 space-y-3 bg-indigo-950/30">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-300">
                      Einzelnes Bett hinzufügen
                    </h4>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <input
                        type="text"
                        placeholder="Bettbezeichnung (z.B. Bett 3)"
                        value={singleBedName}
                        onChange={(e) => setSingleBedName(e.target.value)}
                        className="px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs"
                      />
                      <select
                        value={singleBedType}
                        onChange={(e: any) => setSingleBedType(e.target.value)}
                        className="px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-xs"
                      >
                        <option value="SINGLE">Einzelbett</option>
                        <option value="TOP_BUNK">Hochbett Oben</option>
                        <option value="BOTTOM_BUNK">Hochbett Unten</option>
                        <option value="DOUBLE">Doppelbett</option>
                        <option value="SOFA">Schlafsofa</option>
                        <option value="OTHER">Sonstige Bettart</option>
                      </select>

                      <div className="flex items-center space-x-2">
                        <button
                          onClick={handleAddSingleBed}
                          className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded-lg"
                        >
                          Bett hinzufügen
                        </button>
                        <button
                          onClick={() => setAddingBedRoomId(null)}
                          className="px-3 py-1.5 bg-slate-800 text-slate-400 text-xs rounded-lg"
                        >
                          Abbrechen
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* Rooms List Display */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {selectedBuilding.rooms.map((room) => (
                    <div key={room.id} className="glass-card p-4 rounded-xl space-y-3 border border-slate-800">
                      <div className="flex items-center justify-between border-b border-slate-700/50 pb-2">
                        <div>
                          <span className="font-semibold text-slate-100">{room.name}</span>
                          <span className="ml-2 text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                            Etage {room.floor}
                          </span>
                          {room.girlsRoom && (
                            <span className="ml-2 text-xs px-2 py-0.5 rounded bg-pink-500/20 text-pink-300 font-medium">
                              Mädchenzimmer
                            </span>
                          )}
                          {room.boysRoom && (
                            <span className="ml-2 text-xs px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 font-medium">
                              Jungenzimmer
                            </span>
                          )}
                        </div>

                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => {
                              setAddingBedRoomId(room.id);
                              setSingleBedName(`Bett ${room.beds.length + 1}`);
                            }}
                            className="text-xs px-2 py-1 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 border border-indigo-500/30 rounded-lg flex items-center gap-1 transition"
                          >
                            <Plus className="w-3 h-3" /> Bett
                          </button>
                          <button
                            onClick={() => handleDeleteRoom(selectedBuilding.id, room.id, room.name)}
                            className="p-1 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>

                      {/* Individual Beds */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                        {room.beds.length === 0 ? (
                          <div className="col-span-2 text-xs text-slate-500 italic text-center py-2">
                            Keine Betten vorhanden. Klicken Sie auf "+ Bett".
                          </div>
                        ) : (
                          room.beds.map((bed) => (
                            <div
                              key={bed.id}
                              className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-xs flex items-center justify-between text-slate-300"
                            >
                              <div className="flex items-center space-x-2 truncate">
                                <BedIcon className={`w-4 h-4 flex-shrink-0 ${bed.bedType === 'TOP_BUNK' ? 'text-amber-400' : 'text-indigo-400'}`} />
                                <div className="truncate">
                                  <div className="font-medium text-slate-200 truncate">{bed.name}</div>
                                  <div className="text-[10px] text-slate-500">{bed.bedType}</div>
                                </div>
                              </div>

                              <button
                                onClick={() => handleDeleteBed(selectedBuilding.id, room.id, bed.id)}
                                title="Bett entfernen"
                                className="p-1 text-slate-600 hover:text-rose-400 hover:bg-slate-800 rounded transition"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
