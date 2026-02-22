import { useEffect, useState } from 'react';
import { api, Episode } from '../api/client';
import EpisodeList from '../components/EpisodeList';

export default function FeedPage() {
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    setLoading(true);
    api.getFeed(page)
      .then(data => {
        setEpisodes(data.content);
        setTotalPages(data.totalPages);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [page]);

  if (loading) return <p className="text-gray-400">Loading feed...</p>;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Your Feed</h1>

      {episodes.length === 0 ? (
        <p className="text-gray-400">No episodes yet. Subscribe to some podcasts to see your feed.</p>
      ) : (
        <>
          <EpisodeList episodes={episodes} />
          {totalPages > 1 && (
            <div className="flex justify-center gap-4 mt-6">
              <button
                onClick={() => setPage(p => p - 1)}
                disabled={page === 0}
                className="px-4 py-2 bg-gray-700 rounded disabled:opacity-50"
              >
                Previous
              </button>
              <span className="py-2 text-gray-400">Page {page + 1} of {totalPages}</span>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 bg-gray-700 rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
