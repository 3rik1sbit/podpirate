const API_BASE = '/podpirate/api';

export interface PodcastSearchResult {
  itunesId: number;
  title: string;
  author: string | null;
  description: string | null;
  artworkUrl: string | null;
  feedUrl: string | null;
}

export interface Podcast {
  id: number;
  title: string;
  author: string | null;
  description: string | null;
  artworkUrl: string | null;
  feedUrl: string;
  itunesId: number | null;
}

export interface Episode {
  id: number;
  title: string;
  description: string | null;
  publishedAt: string | null;
  audioUrl: string;
  localAudioPath: string | null;
  processedAudioPath: string | null;
  duration: number | null;
  imageUrl: string | null;
  priority: number;
  status: string;
  podcast?: Podcast;
}

export interface Subscription {
  id: number;
  podcast: Podcast;
  subscribedAt: string;
}

export interface TranscriptionSegment {
  start: number;
  end: number;
  text: string;
}

export interface Transcription {
  id: number;
  segments: string; // JSON string
}

export interface AdSegment {
  id?: number;
  startTime: number;
  endTime: number;
  source: string;
  confirmed: boolean;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  if (!res.ok) throw new Error(`${res.status}: ${res.statusText}`);
  return res.json();
}

export const api = {
  searchPodcasts(query: string) {
    return fetchJson<PodcastSearchResult[]>(`${API_BASE}/podcasts/search?q=${encodeURIComponent(query)}`);
  },

  getSubscriptions() {
    return fetchJson<Subscription[]>(`${API_BASE}/subscriptions`);
  },

  subscribe(data: { feedUrl: string; itunesId?: number; title?: string; author?: string; artworkUrl?: string }) {
    return fetchJson<Subscription>(`${API_BASE}/subscriptions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  unsubscribe(id: number) {
    return fetch(`${API_BASE}/subscriptions/${id}`, { method: 'DELETE' });
  },

  getFeed(page = 0, size = 20) {
    return fetchJson<Page<Episode>>(`${API_BASE}/feed?page=${page}&size=${size}`);
  },

  getPodcast(id: number) {
    return fetchJson<Podcast>(`${API_BASE}/podcasts/${id}`);
  },

  getEpisodes(podcastId: number, page = 0, size = 50) {
    return fetchJson<Page<Episode>>(`${API_BASE}/podcasts/${podcastId}/episodes?page=${page}&size=${size}`);
  },

  getActiveEpisodes() {
    return fetchJson<Episode[]>(`${API_BASE}/episodes/active`);
  },

  getEpisode(id: number) {
    return fetchJson<Episode>(`${API_BASE}/episodes/${id}`);
  },

  getTranscription(episodeId: number) {
    return fetchJson<Transcription>(`${API_BASE}/episodes/${episodeId}/transcription`);
  },

  getAdSegments(episodeId: number) {
    return fetchJson<AdSegment[]>(`${API_BASE}/episodes/${episodeId}/ad-segments`);
  },

  updateAdSegments(episodeId: number, segments: AdSegment[]) {
    return fetchJson<AdSegment[]>(`${API_BASE}/episodes/${episodeId}/ad-segments`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(segments),
    });
  },

  reprocessEpisode(episodeId: number) {
    return fetchJson<{ message: string }>(`${API_BASE}/episodes/${episodeId}/reprocess`, {
      method: 'POST',
    });
  },

  prioritizeEpisode(episodeId: number) {
    return fetchJson<Episode>(`${API_BASE}/episodes/${episodeId}/prioritize`, {
      method: 'POST',
    });
  },

  redetectPodcastAds(podcastId: number) {
    return fetchJson<{ message: string }>(`${API_BASE}/podcasts/${podcastId}/redetect-ads`, {
      method: 'POST',
    });
  },

  audioUrl(episodeId: number, processed = true) {
    return `${API_BASE}/episodes/${episodeId}/audio?processed=${processed}`;
  },

  getStats() {
    return fetchJson<StatsResponse>(`${API_BASE}/stats`);
  },

  getAiPaused() {
    return fetchJson<{ paused: boolean }>(`${API_BASE}/config/ai-paused`);
  },

  async setAiPaused(paused: boolean): Promise<{ paused: boolean } | { error: string; details: string }> {
    const res = await fetch(`${API_BASE}/config/ai-paused`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ paused }),
    });
    if (res.status === 503) {
      return res.json();
    }
    if (!res.ok) throw new Error(`${res.status}: ${res.statusText}`);
    return res.json();
  },
};

export interface EpisodeSummary {
  id: number;
  title: string;
  podcastTitle: string;
  durationSeconds: number | null;
}

export interface PodcastStorage {
  podcastId: number;
  podcastTitle: string;
  audioBytes: number;
  processedBytes: number;
  episodeCount: number;
}

export interface StatsResponse {
  pipeline: Record<string, number>;
  totalEpisodes: number;
  remainingEpisodes: number;
  etaSeconds: number | null;
  totalPodcasts: number;
  totalAudioSeconds: number;
  avgDurationSeconds: number;
  totalAdSeconds: number;
  adSourceCounts: Record<string, number>;
  transcriptionSegments: number;
  longestEpisode: EpisodeSummary | null;
  shortestEpisode: EpisodeSummary | null;
  mostAdHeavyEpisode: EpisodeSummary | null;
  storage: PodcastStorage[];
}
