import { forwardRef, useEffect, useRef } from 'react';

interface AudioPlayerProps {
  src: string;
  onTimeUpdate?: (time: number) => void;
}

const AudioPlayer = forwardRef<HTMLAudioElement, AudioPlayerProps>(
  ({ src, onTimeUpdate }, ref) => {
    const internalRef = useRef<HTMLAudioElement>(null);
    const audioEl = (ref as React.RefObject<HTMLAudioElement>) ?? internalRef;

    useEffect(() => {
      const el = audioEl.current;
      if (!el || !onTimeUpdate) return;

      const handler = () => onTimeUpdate(el.currentTime);
      el.addEventListener('timeupdate', handler);
      return () => el.removeEventListener('timeupdate', handler);
    }, [audioEl, onTimeUpdate]);

    return (
      <audio
        ref={audioEl}
        src={src}
        controls
        className="w-full"
        preload="metadata"
      />
    );
  }
);

AudioPlayer.displayName = 'AudioPlayer';
export default AudioPlayer;
