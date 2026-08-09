import React, { useState } from 'react';
import type { Project } from '../types';
import { api } from '../services/api';
import { Users, Plus, Search, User, Heart, Users2, Trash2 } from 'lucide-react';

interface ParticipantManagerProps {
  project: Project;
  onProjectUpdate: (updated: Project) => void;
}

export const ParticipantManager: React.FC<ParticipantManagerProps> = ({ project, onProjectUpdate }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  // New Participant Form
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [gender, setGender] = useState('maennlich');
  const [age, setAge] = useState<number>(20);
  const [partnerId, setPartnerId] = useState('');
  const [groupId, setGroupId] = useState('');

  const handleAddPerson = async () => {
    if (!lastname.trim()) return;
    try {
      const personPayload = {
        firstname: firstname.trim(),
        lastname: lastname.trim(),
        gender,
        age,
        partnerId: partnerId.trim() || undefined,
        groupId: groupId.trim() || undefined,
      };
      const updated = await api.addPerson(project.id, personPayload);
      onProjectUpdate(updated);
      setFirstname('');
      setLastname('');
      setPartnerId('');
      setGroupId('');
      setShowAddForm(false);
    } catch (e) {
      alert('Fehler beim Erstellen des Teilnehmers.');
    }
  };

  const handleDeletePerson = async (id: string) => {
    try {
      const updated = await api.deletePerson(project.id, id);
      onProjectUpdate(updated);
    } catch (e) {
      alert('Fehler beim Löschen.');
    }
  };

  const filteredPersons = project.persons.filter((p) => {
    const full = `${p.firstname || ''} ${p.lastname || ''} ${p.groupId || ''}`.toLowerCase();
    return full.includes(searchTerm.toLowerCase());
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
            <Users className="w-7 h-7 text-indigo-400" /> Teilnehmerverwaltung
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Gesamt: {project.persons.length} Teilnehmer ({project.persons.filter((p) => p.gender === 'weiblich').length} W, {project.persons.filter((p) => p.gender === 'maennlich').length} M)
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Teilnehmer suchen..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-indigo-500"
            />
          </div>

          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg shadow-md transition"
          >
            <Plus className="w-4 h-4" /> <span>Teilnehmer manuell anlegen</span>
          </button>
        </div>
      </div>

      {/* Add Form Drawer/Modal */}
      {showAddForm && (
        <div className="glass-panel p-5 rounded-2xl border border-indigo-500/30 space-y-4">
          <h3 className="text-md font-semibold text-slate-200">Neuen Teilnehmer hinzufügen</h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 md:grid-cols-6 gap-3">
            <input
              type="text"
              placeholder="Vorname"
              value={firstname}
              onChange={(e) => setFirstname(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            />
            <input
              type="text"
              placeholder="Nachname *"
              value={lastname}
              onChange={(e) => setLastname(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            />
            <select
              value={gender}
              onChange={(e) => setGender(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            >
              <option value="maennlich">männlich</option>
              <option value="weiblich">weiblich</option>
              <option value="divers">divers</option>
            </select>
            <input
              type="number"
              placeholder="Alter"
              value={age}
              onChange={(e) => setAge(parseInt(e.target.value) || 0)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            />
            <input
              type="text"
              placeholder="Paar-ID (z.B. P1)"
              value={partnerId}
              onChange={(e) => setPartnerId(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            />
            <input
              type="text"
              placeholder="Gruppe (z.B. G1)"
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 text-sm"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2 border-t border-slate-800">
            <button
              onClick={() => setShowAddForm(false)}
              className="px-3 py-1.5 bg-slate-800 text-slate-400 hover:text-slate-200 text-xs rounded-lg"
            >
              Abbrechen
            </button>
            <button
              onClick={handleAddPerson}
              className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium rounded-lg"
            >
              Speichern
            </button>
          </div>
        </div>
      )}

      {/* Participant List Table */}
      <div className="glass-panel rounded-2xl overflow-hidden border border-slate-800">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800/80 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-700/60">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Geschlecht</th>
              <th className="px-4 py-3">Alter</th>
              <th className="px-4 py-3">Paar-ID</th>
              <th className="px-4 py-3">Gruppe</th>
              <th className="px-4 py-3 text-right">Aktionen</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {filteredPersons.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                  Keine Teilnehmer gefunden.
                </td>
              </tr>
            ) : (
              filteredPersons.map((p) => (
                <tr key={p.id} className="hover:bg-slate-800/40 transition">
                  <td className="px-4 py-3 font-medium text-slate-100 flex items-center gap-2">
                    <User className="w-4 h-4 text-indigo-400" />
                    {p.firstname} {p.lastname}
                  </td>
                  <td className="px-4 py-3 capitalize">{p.gender}</td>
                  <td className="px-4 py-3 font-mono">{p.age} Jahre</td>
                  <td className="px-4 py-3">
                    {p.partnerId ? (
                      <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">
                        <Heart className="w-3 h-3" /> {p.partnerId}
                      </span>
                    ) : (
                      <span className="text-slate-600">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {p.groupId ? (
                      <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                        <Users2 className="w-3 h-3" /> {p.groupId}
                      </span>
                    ) : (
                      <span className="text-slate-600">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleDeletePerson(p.id)}
                      className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
