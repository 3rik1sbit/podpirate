import { useState } from 'react';
import { api, PodcastSearchResult } from '../api/client';
import PodcastCard from '../components/PodcastCard';

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<PodcastSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [subscribing, setSubscribing] = useState<Set<number>>(new Set());

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    try {
      const data = await api.searchPodcasts(query);
      setResults(data);
    } catch (err) {
      console.error('Search failed:', err);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubscribe(result: PodcastSearchResult) {
    if (!result.feedUrl) return;
    setSubscribing(prev => new Set(prev).add(result.itunesId));
    try {
      await api.subscribe({
        feedUrl: result.feedUrl,
        itunesId: result.itunesId,
        title: result.title,
        author: result.author ?? undefined,
        artworkUrl: result.artworkUrl ?? undefined,
      });
    } catch (err) {
      console.error('Subscribe failed:', err);
    } finally {
      setSubscribing(prev => {
        const next = new Set(prev);
        next.delete(result.itunesId);
        return next;
      });
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Search Podcasts</h1>
      <form onSubmit={handleSearch} className="flex gap-3 mb-8">
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Search for podcasts..."
          className="flex-1 px-4 py-2 bg-gray-800 border border-gray-600 rounded-lg focus:outline-none focus:border-purple-500"
        />
        <button
          type="submit"
          disabled={loading}
          className="px-6 py-2 bg-purple-600 rounded-lg hover:bg-purple-700 disabled:opacity-50"
        >
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {results.map(result => (
          <PodcastCard
            key={result.itunesId}
            title={result.title}
            author={result.author}
            artworkUrl={result.artworkUrl}
            action={
              <button
                onClick={() => handleSubscribe(result)}
                disabled={subscribing.has(result.itunesId) || !result.feedUrl}
                className="px-4 py-1.5 bg-purple-600 rounded hover:bg-purple-700 disabled:opacity-50 text-sm"
              >
                {subscribing.has(result.itunesId) ? 'Subscribing...' : 'Subscribe'}
              </button>
            }
          />
        ))}
      </div>

      {results.length === 0 && !loading && query && (
        <p className="text-gray-400 text-center mt-8">No results found</p>
      )}
    </div>
  );
}
