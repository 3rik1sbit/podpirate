import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import SearchPage from './pages/SearchPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import FeedPage from './pages/FeedPage';
import PodcastDetailPage from './pages/PodcastDetailPage';
import EpisodePlayerPage from './pages/EpisodePlayerPage';

function Nav() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-lg transition-colors ${isActive ? 'bg-purple-600 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-700'}`;

  return (
    <nav className="bg-gray-800 border-b border-gray-700 px-6 py-3 flex items-center gap-4">
      <img src="/podpirate/logo.svg" alt="PodPirate" className="w-8 h-8" />
      <span className="text-xl font-bold text-purple-400 mr-6">PodPirate</span>
      <NavLink to="/" className={linkClass}>Feed</NavLink>
      <NavLink to="/search" className={linkClass}>Search</NavLink>
      <NavLink to="/subscriptions" className={linkClass}>Subscriptions</NavLink>
    </nav>
  );
}

export default function App() {
  return (
    <BrowserRouter basename="/podpirate">
      <div className="min-h-screen bg-gray-900 text-white">
        <Nav />
        <main className="max-w-6xl mx-auto p-6">
          <Routes>
            <Route path="/" element={<FeedPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/podcasts/:id" element={<PodcastDetailPage />} />
            <Route path="/episodes/:id" element={<EpisodePlayerPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
