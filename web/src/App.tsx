import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import SearchPage from './pages/SearchPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import FeedPage from './pages/FeedPage';
import PodcastDetailPage from './pages/PodcastDetailPage';
import EpisodePlayerPage from './pages/EpisodePlayerPage';
import StatsPage from './pages/StatsPage';

function Nav() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-lg transition-colors ${isActive ? 'bg-purple-600 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-700'}`;

  return (
    <nav className="bg-gray-800 border-b border-gray-700 px-4 sm:px-6 py-3 flex items-center gap-2 sm:gap-4 overflow-x-auto">
      <img src="/podpirate/logo.svg" alt="PodPirate" className="w-[60px] h-[60px] shrink-0" />
      <span className="text-xl font-bold text-purple-400 mr-2 sm:mr-6 shrink-0 hidden sm:inline">PodPirate</span>
      <NavLink to="/" className={linkClass}>Feed</NavLink>
      <NavLink to="/search" className={linkClass}>Search</NavLink>
      <NavLink to="/subscriptions" className={linkClass}>Subs</NavLink>
      <NavLink to="/stats" className={linkClass}>Stats</NavLink>
    </nav>
  );
}

export default function App() {
  return (
    <BrowserRouter basename="/podpirate">
      <div className="min-h-screen bg-gray-900 text-white">
        <Nav />
        <main className="max-w-6xl mx-auto p-4 sm:p-6">
          <Routes>
            <Route path="/" element={<FeedPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/podcasts/:id" element={<PodcastDetailPage />} />
            <Route path="/episodes/:id" element={<EpisodePlayerPage />} />
            <Route path="/stats" element={<StatsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
