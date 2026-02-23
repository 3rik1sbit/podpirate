import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { api, Episode, AdSegment, TranscriptionSegment } from '../api/client';
import TranscriptionView from '../components/TranscriptionView';
import AudioPlayer from '../components/AudioPlayer';

type SelectingAd = null | 'picking-start' | 'picking-end';

interface PendingAd {
  startTime: number;
  endTime: number;
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function EpisodePlayerPage() {
  const { id } = useParams<{ id: string }>();
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [segments, setSegments] = useState<TranscriptionSegment[]>([]);
  const [adSegments, setAdSegments] = useState<AdSegment[]>([]);
  const [currentTime, setCurrentTime] = useState(0);
  const [useProcessed, setUseProcessed] = useState(true);
  const audioRef = useRef<HTMLAudioElement>(null);

  // Ad selection state
  const [selectingAd, setSelectingAd] = useState<SelectingAd>(null);
  const [rangeStart, setRangeStart] = useState<TranscriptionSegment | null>(null);
  const [rangeEnd, setRangeEnd] = useState<TranscriptionSegment | null>(null);
  const [pendingAd, setPendingAd] = useState<PendingAd | null>(null);
  const [playingPreview, setPlayingPreview] = useState(false);

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

  // Escape key cancels selection
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      setSelectingAd(null);
      setRangeStart(null);
      setRangeEnd(null);
    }
  }, []);

  useEffect(() => {
    if (selectingAd) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [selectingAd, handleKeyDown]);

  // Stop preview playback at endTime
  useEffect(() => {
    if (!playingPreview || !pendingAd) return;
    const audio = audioRef.current;
    if (!audio) return;

    function onTimeUpdate() {
      if (audio && pendingAd && audio.currentTime >= pendingAd.endTime) {
        audio.pause();
        setPlayingPreview(false);
      }
    }

    audio.addEventListener('timeupdate', onTimeUpdate);
    return () => audio.removeEventListener('timeupdate', onTimeUpdate);
  }, [playingPreview, pendingAd]);

  function handleSeek(time: number) {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
    }
  }

  function handleSelectSegment(which: 'start' | 'end', seg: TranscriptionSegment) {
    if (which === 'start') {
      setRangeStart(seg);
      setRangeEnd(null);
      setSelectingAd('picking-end');
    } else {
      setRangeEnd(seg);
    }
  }

  function handleFinish() {
    if (!rangeStart || !rangeEnd) return;
    const startTime = Math.min(rangeStart.start, rangeEnd.start);
    const endTime = Math.max(rangeStart.end, rangeEnd.end);
    setPendingAd({ startTime, endTime });
    setSelectingAd(null);
  }

  function handleCancel() {
    setPendingAd(null);
    setSelectingAd(null);
    setRangeStart(null);
    setRangeEnd(null);
    setPlayingPreview(false);
  }

  function handlePlayPreview() {
    if (!pendingAd || !audioRef.current) return;
    if (playingPreview) {
      audioRef.current.pause();
      setPlayingPreview(false);
    } else {
      audioRef.current.currentTime = pendingAd.startTime;
      audioRef.current.play();
      setPlayingPreview(true);
    }
  }

  function addAdSegment(start: number, end: number): AdSegment[] {
    const newSeg: AdSegment = { startTime: start, endTime: end, source: 'MANUAL', confirmed: true };
    const nonOverlapping = adSegments.filter(s => s.endTime < start || s.startTime > end);
    const overlapping = adSegments.filter(s => s.endTime >= start && s.startTime <= end);
    const mergedStart = Math.min(start, ...overlapping.map(s => s.startTime));
    const mergedEnd = Math.max(end, ...overlapping.map(s => s.endTime));
    return [...nonOverlapping, { ...newSeg, startTime: mergedStart, endTime: mergedEnd }]
      .sort((a, b) => a.startTime - b.startTime);
  }

  async function handleDeleteFromEpisode() {
    if (!id || !pendingAd) return;
    const updated = addAdSegment(pendingAd.startTime, pendingAd.endTime);
    setAdSegments(updated);
    handleCancel();
    const saved = await api.updateAdSegments(parseInt(id), updated);
    setAdSegments(saved);
    await api.reprocessEpisode(parseInt(id));
  }

  async function handleDeleteFromAll() {
    if (!id || !pendingAd || !episode?.podcast?.id) return;
    const updated = addAdSegment(pendingAd.startTime, pendingAd.endTime);
    setAdSegments(updated);
    handleCancel();
    const saved = await api.updateAdSegments(parseInt(id), updated);
    setAdSegments(saved);
    await api.reprocessEpisode(parseInt(id));
    await api.redetectPodcastAds(episode.podcast.id);
  }

  async function handleReprocess() {
    if (!id) return;
    await api.reprocessEpisode(parseInt(id));
  }

  if (!episode) return <p className="text-gray-400">Loading...</p>;

  const audioSrc = id ? api.audioUrl(parseInt(id), useProcessed) : '';

  // Get transcript text for the pending ad range
  const pendingAdText = pendingAd
    ? segments
        .filter(s => s.start >= pendingAd.startTime && s.end <= pendingAd.endTime)
        .map(s => s.text)
        .join(' ')
    : '';

  return (
    <div>
      <div className="flex items-start gap-4 mb-2">
        {(episode.imageUrl || episode.podcast?.artworkUrl) && (
          <img
            src={episode.imageUrl ?? episode.podcast?.artworkUrl ?? ''}
            alt=""
            className="w-16 h-16 sm:w-20 sm:h-20 rounded-lg object-cover shrink-0"
          />
        )}
        <div className="flex-1 min-w-0">
          <h1 className="text-xl sm:text-2xl font-bold">{episode.title}</h1>
          {episode.podcast?.title && (
            <p className="text-gray-400 text-sm">{episode.podcast.title}</p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-3 mb-1">
        <p className="text-gray-400 text-sm">
          Status: <span className={`font-medium ${episode.status === 'READY' ? 'text-green-400' : 'text-yellow-400'}`}>
            {episode.status}
          </span>
        </p>
        {!['READY', 'ERROR'].includes(episode.status) && episode.priority < 1000 && (
          <button
            onClick={async () => {
              if (!id) return;
              await api.prioritizeEpisode(parseInt(id));
              const updated = await api.getEpisode(parseInt(id));
              setEpisode(updated);
            }}
            className="px-3 py-0.5 bg-yellow-600 hover:bg-yellow-700 rounded text-xs"
          >
            Prioritize
          </button>
        )}
        {episode.priority >= 1000 && (
          <span className="text-xs text-yellow-400">Prioritized</span>
        )}
      </div>
      {episode.description && (
        <p className="text-gray-300 text-sm mb-6 line-clamp-3">{episode.description}</p>
      )}

      <div className="mb-6">
        <div className="flex flex-wrap items-center gap-3 sm:gap-4 mb-3">
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
            selectingAd={selectingAd}
            rangeStart={rangeStart}
            rangeEnd={rangeEnd}
            onSelectSegment={handleSelectSegment}
            onFinish={handleFinish}
          />
        </div>
        <div>
          <h2 className="text-lg font-semibold mb-3">Ad Segments</h2>
          <AdSegmentPanel
            adSegments={adSegments}
            selectingAd={selectingAd}
            pendingAd={pendingAd}
            pendingAdText={pendingAdText}
            playingPreview={playingPreview}
            hasPodcast={!!episode?.podcast?.id}
            onAddSegment={() => {
              setSelectingAd('picking-start');
              setRangeStart(null);
              setRangeEnd(null);
              setPendingAd(null);
            }}
            onPlayPreview={handlePlayPreview}
            onDeleteFromEpisode={handleDeleteFromEpisode}
            onDeleteFromAll={handleDeleteFromAll}
            onCancel={handleCancel}
          />
        </div>
      </div>
    </div>
  );
}

function AdSegmentPanel({
  adSegments,
  selectingAd,
  pendingAd,
  pendingAdText,
  playingPreview,
  hasPodcast,
  onAddSegment,
  onPlayPreview,
  onDeleteFromEpisode,
  onDeleteFromAll,
  onCancel,
}: {
  adSegments: AdSegment[];
  selectingAd: SelectingAd;
  pendingAd: PendingAd | null;
  pendingAdText: string;
  playingPreview: boolean;
  hasPodcast: boolean;
  onAddSegment: () => void;
  onPlayPreview: () => void;
  onDeleteFromEpisode: () => void;
  onDeleteFromAll: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="space-y-3">
      {adSegments.length === 0 && !pendingAd && (
        <p className="text-gray-500 text-sm">No ad segments detected</p>
      )}
      {adSegments.map((seg, i) => (
        <div key={i} className="flex items-center gap-2 bg-gray-800 p-2 rounded text-sm">
          <span>{formatTime(seg.startTime)}</span>
          <span className="text-gray-500">→</span>
          <span>{formatTime(seg.endTime)}</span>
          <span className="text-xs text-gray-500 ml-auto">{seg.source}</span>
        </div>
      ))}

      {pendingAd && (
        <div className="bg-yellow-900/30 border border-yellow-600/50 rounded p-3 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-yellow-200">Pending ad segment</span>
            <span className="text-xs text-yellow-400">
              {formatTime(pendingAd.startTime)} → {formatTime(pendingAd.endTime)}
            </span>
          </div>
          {pendingAdText && (
            <p className="text-xs text-gray-300 max-h-24 overflow-y-auto leading-relaxed">
              {pendingAdText}
            </p>
          )}
          <div className="flex items-center gap-2">
            <button
              onClick={onPlayPreview}
              className="px-2 py-1 bg-gray-700 rounded text-sm hover:bg-gray-600"
              title={playingPreview ? 'Stop preview' : 'Play ad audio'}
            >
              {playingPreview ? '⏹ Stop' : '▶ Play'}
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={onDeleteFromEpisode}
              className="px-3 py-1.5 bg-green-600 rounded text-sm hover:bg-green-700"
            >
              Delete from this episode
            </button>
            {hasPodcast && (
              <button
                onClick={onDeleteFromAll}
                className="px-3 py-1.5 bg-blue-600 rounded text-sm hover:bg-blue-700"
              >
                Delete from all episodes
              </button>
            )}
            <button
              onClick={onCancel}
              className="px-3 py-1.5 bg-gray-700 rounded text-sm hover:bg-gray-600"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {!pendingAd && !selectingAd && (
        <button
          onClick={onAddSegment}
          className="px-3 py-1.5 bg-gray-700 rounded text-sm hover:bg-gray-600"
        >
          + Add Segment
        </button>
      )}

      {selectingAd && (
        <div className="text-sm text-yellow-300">
          Selecting ad range in transcript...
          <button
            onClick={onCancel}
            className="ml-3 px-2 py-0.5 bg-gray-700 rounded text-xs hover:bg-gray-600 text-gray-300"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  );
}
