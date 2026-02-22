import { useEffect, useRef } from 'react';
import { TranscriptionSegment, AdSegment } from '../api/client';

interface TranscriptionViewProps {
  segments: TranscriptionSegment[];
  adSegments: AdSegment[];
  currentTime: number;
  onSeek?: (time: number) => void;
}

function isInAdSegment(time: number, adSegments: AdSegment[]): boolean {
  return adSegments.some(ad => time >= ad.startTime && time <= ad.endTime);
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function TranscriptionView({ segments, adSegments, currentTime, onSeek }: TranscriptionViewProps) {
  const activeRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [currentTime]);

  if (segments.length === 0) {
    return <p className="text-gray-500 text-sm">No transcription available</p>;
  }

  return (
    <div className="max-h-96 overflow-y-auto space-y-1 pr-2">
      {segments.map((seg, i) => {
        const isActive = currentTime >= seg.start && currentTime < seg.end;
        const isAd = isInAdSegment(seg.start, adSegments);

        return (
          <div
            key={i}
            ref={isActive ? activeRef : undefined}
            onClick={() => onSeek?.(seg.start)}
            className={`px-3 py-1.5 rounded cursor-pointer text-sm transition-colors ${
              isActive
                ? 'bg-purple-600/30 text-white'
                : isAd
                  ? 'bg-red-900/30 text-red-300 hover:bg-red-900/50'
                  : 'text-gray-300 hover:bg-gray-800'
            }`}
          >
            <span className="text-xs text-gray-500 mr-2">{formatTime(seg.start)}</span>
            {seg.text}
          </div>
        );
      })}
    </div>
  );
}
