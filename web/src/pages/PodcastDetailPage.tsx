import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, Episode, Podcast } from '../api/client';
import EpisodeList from '../components/EpisodeList';

export default function PodcastDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    const podcastId = parseInt(id);

    Promise.all([
      api.getPodcast(podcastId),
      api.getEpisodes(podcastId),
    ]).then(([p, epPage]) => {
      setPodcast(p);
      setEpisodes(epPage.content);
    }).catch(console.error)
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className="text-gray-400">Loading...</p>;
  if (!podcast) return <p className="text-gray-400">Podcast not found</p>;

  return (
    <div>
      <div className="flex flex-col sm:flex-row gap-4 sm:gap-6 mb-8">
        {podcast.artworkUrl && (
          <img src={podcast.artworkUrl} alt="" className="w-24 h-24 sm:w-32 sm:h-32 rounded-lg object-cover" />
        )}
        <div className="min-w-0">
          <h1 className="text-xl sm:text-2xl font-bold">{podcast.title}</h1>
          {podcast.author && <p className="text-gray-400 mt-1">{podcast.author}</p>}
          {podcast.description && (
            <p className="text-gray-300 mt-3 text-sm line-clamp-3">{podcast.description}</p>
          )}
        </div>
      </div>

      <h2 className="text-xl font-semibold mb-4">Episodes ({episodes.length})</h2>
      <EpisodeList episodes={episodes} />
    </div>
  );
}
