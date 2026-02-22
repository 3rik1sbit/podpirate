import { useEffect, useState } from 'react';
import { api, StatsResponse } from '../api/client';

const STATUS_ORDER = ['PENDING', 'DOWNLOADING', 'DOWNLOADED', 'TRANSCRIBING', 'DETECTING_ADS', 'PROCESSING', 'READY', 'ERROR'];
const STATUS_COLORS: Record<string, string> = {
  PENDING: '#6b7280',
  DOWNLOADING: '#3b82f6',
  DOWNLOADED: '#60a5fa',
  TRANSCRIBING: '#f59e0b',
  DETECTING_ADS: '#f97316',
  PROCESSING: '#a855f7',
  READY: '#22c55e',
  ERROR: '#ef4444',
};

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${Math.round(seconds)}s`;
  if (seconds < 3600) return `${Math.round(seconds / 60)}m`;
  const days = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.round((seconds % 3600) / 60);
  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (h > 0) parts.push(`${h}h`);
  if (m > 0 && days < 7) parts.push(`${m}m`);
  return parts.join(' ') || '0m';
}

function formatLargeTime(seconds: number): string {
  const hours = seconds / 3600;
  if (hours < 24) return `${hours.toFixed(1)} hours`;
  const days = hours / 24;
  if (days < 7) return `${days.toFixed(1)} days`;
  const weeks = days / 7;
  if (weeks < 4.3) return `${weeks.toFixed(1)} weeks`;
  const months = days / 30.44;
  if (months < 12) return `${months.toFixed(1)} months`;
  const years = days / 365.25;
  return `${years.toFixed(1)} years`;
}

function formatLargeTimeFull(seconds: number): string {
  const hours = seconds / 3600;
  if (hours < 24) return `${hours.toFixed(1)} hours`;
  const days = hours / 24;
  const parts: string[] = [];
  if (days >= 365.25) {
    const y = Math.floor(days / 365.25);
    parts.push(`${y}y`);
  }
  if (days >= 30.44) {
    const m = Math.floor((days % 365.25) / 30.44);
    if (m > 0) parts.push(`${m}mo`);
  }
  if (days >= 7) {
    const w = Math.floor((days % 30.44) / 7);
    if (w > 0) parts.push(`${w}w`);
  }
  const d = Math.floor(days % 7);
  if (d > 0) parts.push(`${d}d`);
  return parts.join(' ') || '0d';
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

function Stat({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="bg-gray-800 rounded-lg p-3 sm:p-4 border border-gray-700">
      <div className="text-xs sm:text-sm text-gray-400 mb-1">{label}</div>
      <div className="text-lg sm:text-2xl font-bold break-words">{value}</div>
      {sub && <div className="text-xs text-gray-500 mt-1">{sub}</div>}
    </div>
  );
}

function EpisodeCard({ label, ep }: { label: string; ep: { id: number; title: string; podcastTitle: string; durationSeconds: number | null } | null }) {
  if (!ep) return null;
  return (
    <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
      <div className="text-sm text-gray-400 mb-1">{label}</div>
      <div className="font-semibold truncate" title={ep.title}>{ep.title}</div>
      <div className="text-xs text-gray-500">{ep.podcastTitle}</div>
      {ep.durationSeconds != null && (
        <div className="text-sm text-purple-400 mt-1">{formatDuration(ep.durationSeconds)}</div>
      )}
    </div>
  );
}

export default function StatsPage() {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = () => {
    api.getStats()
      .then(data => { setStats(data); setError(null); })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading) return <p className="text-gray-400">Loading stats...</p>;
  if (error) return <p className="text-red-400">Error: {error}</p>;
  if (!stats) return null;

  const readyCount = stats.pipeline['READY'] || 0;
  const errorCount = stats.pipeline['ERROR'] || 0;
  const total = stats.totalEpisodes;
  const adPct = stats.totalAudioSeconds > 0
    ? ((stats.totalAdSeconds / stats.totalAudioSeconds) * 100).toFixed(1)
    : '0';
  const autoAds = stats.adSourceCounts['AUTO'] || 0;
  const manualAds = stats.adSourceCounts['MANUAL'] || 0;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Stats</h1>

      {/* Pipeline Progress */}
      <div className="mb-8">
        <div className="flex flex-col sm:flex-row sm:items-baseline justify-between mb-2 gap-1">
          <h2 className="text-lg font-semibold">Pipeline Progress</h2>
          <span className="text-sm text-gray-400">
            {readyCount} of {total} ready
            {errorCount > 0 && <span className="text-red-400 ml-2">({errorCount} errors)</span>}
          </span>
        </div>

        {/* Stacked bar */}
        <div className="w-full h-6 rounded-full overflow-hidden flex bg-gray-800 border border-gray-700">
          {STATUS_ORDER.map(status => {
            const count = stats.pipeline[status] || 0;
            if (count === 0) return null;
            const pct = (count / total) * 100;
            return (
              <div
                key={status}
                style={{ width: `${pct}%`, backgroundColor: STATUS_COLORS[status] }}
                className="h-full transition-all duration-500"
                title={`${status}: ${count}`}
              />
            );
          })}
        </div>

        {/* Legend */}
        <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2">
          {STATUS_ORDER.map(status => {
            const count = stats.pipeline[status] || 0;
            if (count === 0) return null;
            return (
              <div key={status} className="flex items-center gap-1.5 text-xs text-gray-400">
                <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: STATUS_COLORS[status] }} />
                {status} ({count})
              </div>
            );
          })}
        </div>
      </div>

      {/* ETA Card */}
      <div className="mb-8 bg-gray-800 rounded-lg p-4 border border-gray-700">
        {stats.remainingEpisodes > 0 ? (
          <>
            <div className="text-sm text-gray-400">Estimated time remaining</div>
            <div className="text-2xl font-bold text-purple-400">
              {stats.etaSeconds ? formatDuration(stats.etaSeconds) : 'Calculating...'}
            </div>
            <div className="text-xs text-gray-500 mt-1">
              {stats.remainingEpisodes} episodes in queue
            </div>
          </>
        ) : (
          <>
            <div className="text-sm text-gray-400">Pipeline status</div>
            <div className="text-2xl font-bold text-green-400">All caught up!</div>
          </>
        )}
      </div>

      {/* Fun Facts Grid */}
      <h2 className="text-lg font-semibold mb-3">Library</h2>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 mb-6">
        <Stat label="Podcasts" value={stats.totalPodcasts} />
        <Stat label="Episodes" value={stats.totalEpisodes} />
        <Stat label="Total Audio" value={formatLargeTime(stats.totalAudioSeconds)} sub={formatLargeTimeFull(stats.totalAudioSeconds)} />
        <Stat label="Avg Duration" value={formatDuration(stats.avgDurationSeconds)} />
        <Stat label="Total Ad Time" value={formatLargeTime(stats.totalAdSeconds)} sub={stats.totalAdSeconds >= 86400 ? formatLargeTimeFull(stats.totalAdSeconds) : undefined} />
        <Stat label="Ad Percentage" value={`${adPct}%`} />
        <Stat label="Auto Ads" value={autoAds} sub="detected by AI" />
        <Stat label="Manual Ads" value={manualAds} sub="marked by hand" />
        <Stat label="Transcription Segments" value={stats.transcriptionSegments.toLocaleString()} />
      </div>

      {/* Storage */}
      {stats.storage && stats.storage.length > 0 && (
        <>
          <h2 className="text-lg font-semibold mb-3 mt-6">Storage</h2>
          <div className="bg-gray-800 rounded-lg border border-gray-700 overflow-x-auto">
            <table className="w-full text-sm min-w-[480px]">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400">
                  <th className="text-left p-3">Podcast</th>
                  <th className="text-right p-3">Episodes</th>
                  <th className="text-right p-3">Audio</th>
                  <th className="text-right p-3">Processed</th>
                  <th className="text-right p-3">Total</th>
                </tr>
              </thead>
              <tbody>
                {stats.storage.map(s => (
                  <tr key={s.podcastId} className="border-b border-gray-700/50 hover:bg-gray-700/30">
                    <td className="p-3 font-medium truncate max-w-xs" title={s.podcastTitle}>{s.podcastTitle}</td>
                    <td className="p-3 text-right text-gray-400">{s.episodeCount}</td>
                    <td className="p-3 text-right text-gray-400">{formatBytes(s.audioBytes)}</td>
                    <td className="p-3 text-right text-gray-400">{formatBytes(s.processedBytes)}</td>
                    <td className="p-3 text-right font-medium text-purple-400">{formatBytes(s.audioBytes + s.processedBytes)}</td>
                  </tr>
                ))}
                <tr className="bg-gray-700/30 font-semibold">
                  <td className="p-3">Total</td>
                  <td className="p-3 text-right">{stats.storage.reduce((sum, s) => sum + s.episodeCount, 0)}</td>
                  <td className="p-3 text-right">{formatBytes(stats.storage.reduce((sum, s) => sum + s.audioBytes, 0))}</td>
                  <td className="p-3 text-right">{formatBytes(stats.storage.reduce((sum, s) => sum + s.processedBytes, 0))}</td>
                  <td className="p-3 text-right text-purple-400">{formatBytes(stats.storage.reduce((sum, s) => sum + s.audioBytes + s.processedBytes, 0))}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Notable Episodes */}
      {(stats.longestEpisode || stats.shortestEpisode || stats.mostAdHeavyEpisode) && (
        <>
          <h2 className="text-lg font-semibold mb-3 mt-6">Notable Episodes</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <EpisodeCard label="Longest" ep={stats.longestEpisode} />
            <EpisodeCard label="Shortest" ep={stats.shortestEpisode} />
            <EpisodeCard label="Most Ads" ep={stats.mostAdHeavyEpisode} />
          </div>
        </>
      )}
    </div>
  );
}
