import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, Subscription } from '../api/client';
import PodcastCard from '../components/PodcastCard';

export default function SubscriptionsPage() {
  const [subs, setSubs] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getSubscriptions()
      .then(setSubs)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  async function handleUnsubscribe(subId: number) {
    await api.unsubscribe(subId);
    setSubs(prev => prev.filter(s => s.id !== subId));
  }

  if (loading) return <p className="text-gray-400">Loading...</p>;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Subscriptions</h1>

      {subs.length === 0 ? (
        <p className="text-gray-400">
          No subscriptions yet. <Link to="/search" className="text-purple-400 hover:underline">Search for podcasts</Link> to subscribe.
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {subs.map(sub => (
            <PodcastCard
              key={sub.id}
              title={sub.podcast.title}
              author={sub.podcast.author}
              artworkUrl={sub.podcast.artworkUrl}
              linkTo={`/podcasts/${sub.podcast.id}`}
              action={
                <button
                  onClick={() => handleUnsubscribe(sub.id)}
                  className="px-4 py-1.5 bg-red-600 rounded hover:bg-red-700 text-sm"
                >
                  Unsubscribe
                </button>
              }
            />
          ))}
        </div>
      )}
    </div>
  );
}
