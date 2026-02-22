import { Link } from 'react-router-dom';
import { Episode } from '../api/client';

interface EpisodeListProps {
  episodes: Episode[];
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

export default function EpisodeList({ episodes }: EpisodeListProps) {
  return (
    <div className="space-y-2">
      {episodes.map(ep => (
        <Link
          key={ep.id}
          to={`/episodes/${ep.id}`}
          className="block bg-gray-800 rounded-lg p-4 hover:bg-gray-750 transition-colors"
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <h3 className="font-medium text-sm truncate">{ep.title}</h3>
              <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                {ep.publishedAt && <span>{formatDate(ep.publishedAt)}</span>}
                {ep.duration && <span>{formatDuration(ep.duration)}</span>}
              </div>
            </div>
            <span className={`text-xs font-medium ${statusColors[ep.status] ?? 'text-gray-400'}`}>
              {ep.status}
            </span>
          </div>
        </Link>
      ))}
    </div>
  );
}
