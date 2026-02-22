import { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { api, Episode, AdSegment, TranscriptionSegment } from '../api/client';
import TranscriptionView from '../components/TranscriptionView';
import AudioPlayer from '../components/AudioPlayer';

export default function EpisodePlayerPage() {
  const { id } = useParams<{ id: string }>();
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [segments, setSegments] = useState<TranscriptionSegment[]>([]);
  const [adSegments, setAdSegments] = useState<AdSegment[]>([]);
  const [currentTime, setCurrentTime] = useState(0);
  const [useProcessed, setUseProcessed] = useState(true);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    if (!id) return;
    const episodeId = parseInt(id);

    api.getEpisode(episodeId).then(setEpisode).catch(console.error);

    api.getTranscription(episodeId)
      .then(t => setSegments(JSON.parse(t.segments)))
      .catch(() => setSegments([]));

    api.getAdSegments(episodeId)
      .then(setAdSegments)
      .catch(() => setAdSegments([]));
  }, [id]);

  async function handleSaveAdSegments(updated: AdSegment[]) {
    if (!id) return;
    const saved = await api.updateAdSegments(parseInt(id), updated);
    setAdSegments(saved);
  }

  async function handleReprocess() {
    if (!id) return;
    await api.reprocessEpisode(parseInt(id));
  }

  function handleSeek(time: number) {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
    }
  }

  if (!episode) return <p className="text-gray-400">Loading...</p>;

  const audioSrc = id ? api.audioUrl(parseInt(id), useProcessed) : '';

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">{episode.title}</h1>
      <p className="text-gray-400 mb-1 text-sm">
        Status: <span className={`font-medium ${episode.status === 'READY' ? 'text-green-400' : 'text-yellow-400'}`}>
          {episode.status}
        </span>
      </p>
      {episode.description && (
        <p className="text-gray-300 text-sm mb-6 line-clamp-3">{episode.description}</p>
      )}

      <div className="mb-6">
        <div className="flex items-center gap-4 mb-3">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={useProcessed}
              onChange={e => setUseProcessed(e.target.checked)}
              className="rounded"
            />
            Play ad-free version
          </label>
          <button
            onClick={handleReprocess}
            className="px-3 py-1 bg-gray-700 rounded text-sm hover:bg-gray-600"
          >
            Reprocess Audio
          </button>
        </div>
        <AudioPlayer
          ref={audioRef}
          src={audioSrc}
          onTimeUpdate={setCurrentTime}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <h2 className="text-lg font-semibold mb-3">Transcript</h2>
          <TranscriptionView
            segments={segments}
            adSegments={adSegments}
            currentTime={currentTime}
            onSeek={handleSeek}
          />
        </div>
        <div>
          <h2 className="text-lg font-semibold mb-3">Ad Segments</h2>
          <AdSegmentEditor
            adSegments={adSegments}
            onSave={handleSaveAdSegments}
            onReprocess={handleReprocess}
          />
        </div>
      </div>
    </div>
  );
}

function AdSegmentEditor({
  adSegments,
  onSave,
  onReprocess,
}: {
  adSegments: AdSegment[];
  onSave: (segments: AdSegment[]) => void;
  onReprocess: () => void;
}) {
  const [editing, setEditing] = useState<AdSegment[]>([]);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    setEditing(adSegments.map(s => ({ ...s })));
    setDirty(false);
  }, [adSegments]);

  function updateSegment(index: number, field: 'startTime' | 'endTime', value: string) {
    const updated = [...editing];
    updated[index] = { ...updated[index], [field]: parseFloat(value) || 0 };
    setEditing(updated);
    setDirty(true);
  }

  function addSegment() {
    setEditing([...editing, { startTime: 0, endTime: 0, source: 'MANUAL', confirmed: true }]);
    setDirty(true);
  }

  function removeSegment(index: number) {
    setEditing(editing.filter((_, i) => i !== index));
    setDirty(true);
  }

  function handleSave() {
    onSave(editing);
    setDirty(false);
  }

  return (
    <div className="space-y-3">
      {editing.length === 0 && (
        <p className="text-gray-500 text-sm">No ad segments detected</p>
      )}
      {editing.map((seg, i) => (
        <div key={i} className="flex items-center gap-2 bg-gray-800 p-2 rounded">
          <input
            type="number"
            step="0.1"
            value={seg.startTime}
            onChange={e => updateSegment(i, 'startTime', e.target.value)}
            className="w-24 px-2 py-1 bg-gray-700 rounded text-sm"
            placeholder="Start (s)"
          />
          <span className="text-gray-500">→</span>
          <input
            type="number"
            step="0.1"
            value={seg.endTime}
            onChange={e => updateSegment(i, 'endTime', e.target.value)}
            className="w-24 px-2 py-1 bg-gray-700 rounded text-sm"
            placeholder="End (s)"
          />
          <span className="text-xs text-gray-500">{seg.source}</span>
          <button onClick={() => removeSegment(i)} className="text-red-400 hover:text-red-300 ml-auto text-sm">
            Remove
          </button>
        </div>
      ))}
      <div className="flex gap-2">
        <button onClick={addSegment} className="px-3 py-1.5 bg-gray-700 rounded text-sm hover:bg-gray-600">
          + Add Segment
        </button>
        {dirty && (
          <>
            <button onClick={handleSave} className="px-3 py-1.5 bg-purple-600 rounded text-sm hover:bg-purple-700">
              Save Changes
            </button>
            <button
              onClick={() => { handleSave(); onReprocess(); }}
              className="px-3 py-1.5 bg-green-600 rounded text-sm hover:bg-green-700"
            >
              Save & Reprocess
            </button>
          </>
        )}
      </div>
    </div>
  );
}
