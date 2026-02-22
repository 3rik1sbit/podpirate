import { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface PodcastCardProps {
  title: string;
  author: string | null;
  artworkUrl: string | null;
  linkTo?: string;
  action?: ReactNode;
}

export default function PodcastCard({ title, author, artworkUrl, linkTo, action }: PodcastCardProps) {
  const content = (
    <div className="bg-gray-800 rounded-lg overflow-hidden hover:bg-gray-750 transition-colors">
      {artworkUrl ? (
        <img src={artworkUrl} alt="" className="w-full aspect-square object-cover" />
      ) : (
        <div className="w-full aspect-square bg-gray-700 flex items-center justify-center text-4xl">🎙</div>
      )}
      <div className="p-4">
        <h3 className="font-semibold text-sm line-clamp-2">{title}</h3>
        {author && <p className="text-gray-400 text-xs mt-1 truncate">{author}</p>}
        {action && <div className="mt-3">{action}</div>}
      </div>
    </div>
  );

  if (linkTo) {
    return <Link to={linkTo}>{content}</Link>;
  }

  return content;
}
