import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, Episode } from '../api/client';

interface EpisodeListProps {
  episodes: Episode[];
  onUpdate?: () => void;
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
  });
}

function formatDuration(seconds: number | null): string {
  if (!seconds) return '';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

const statusColors: Record<string, string> = {
  READY: 'text-green-400',
  PENDING: 'text-gray-400',
  DOWNLOADING: 'text-blue-400',
  DOWNLOADED: 'text-blue-300',
  TRANSCRIBING: 'text-yellow-400',
  DETECTING_ADS: 'text-orange-400',
  PROCESSING: 'text-purple-400',
  ERROR: 'text-red-400',
};

const canPrioritize = (status: string) =>
  !['READY', 'ERROR'].includes(status);

export default function EpisodeList({ episodes, onUpdate }: EpisodeListProps) {
  const [prioritizing, setPrioritizing] = useState<number | null>(null);

  async function handlePrioritize(e: React.MouseEvent, episodeId: number) {
    e.preventDefault();
    setPrioritizing(episodeId);
    try {
      await api.prioritizeEpisode(episodeId);
      onUpdate?.();
    } catch (err) {
      console.error('Failed to prioritize', err);
    } finally {
      setPrioritizing(null);
    }
  }

  return (
    <div className="space-y-2">
      {episodes.map(ep => (
        <Link
          key={ep.id}
          to={`/episodes/${ep.id}`}
          className="block bg-gray-800 rounded-lg p-4 hover:bg-gray-750 transition-colors"
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 flex-1 min-w-0">
              {(ep.imageUrl || ep.podcast?.artworkUrl) && (
                <img
                  src={ep.imageUrl ?? ep.podcast?.artworkUrl ?? ''}
                  alt=""
                  className="w-12 h-12 rounded object-cover shrink-0"
                />
              )}
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-sm truncate">{ep.title}</h3>
                {ep.podcast?.title && (
                  <p className="text-xs text-gray-400 truncate">{ep.podcast.title}</p>
                )}
                <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                  {ep.publishedAt && <span>{formatDate(ep.publishedAt)}</span>}
                  {ep.duration && <span>{formatDuration(ep.duration)}</span>}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {canPrioritize(ep.status) && ep.priority < 1000 && (
                <button
                  onClick={e => handlePrioritize(e, ep.id)}
                  disabled={prioritizing === ep.id}
                  className="px-2 py-0.5 bg-yellow-600 hover:bg-yellow-700 disabled:opacity-50 rounded text-xs"
                >
                  {prioritizing === ep.id ? '...' : 'Prioritize'}
                </button>
              )}
              {ep.priority >= 1000 && (
                <span className="text-xs text-yellow-400">Prioritized</span>
              )}
              <span className={`text-xs font-medium ${statusColors[ep.status] ?? 'text-gray-400'}`}>
                {ep.status}
              </span>
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}
