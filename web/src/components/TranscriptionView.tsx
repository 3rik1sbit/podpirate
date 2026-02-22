import { useEffect, useRef, useState } from 'react';
import { TranscriptionSegment, AdSegment } from '../api/client';

type SelectingAd = null | 'picking-start' | 'picking-end';

interface TranscriptionViewProps {
  segments: TranscriptionSegment[];
  adSegments: AdSegment[];
  currentTime: number;
  onSeek?: (time: number) => void;
  selectingAd: SelectingAd;
  rangeStart: TranscriptionSegment | null;
  rangeEnd: TranscriptionSegment | null;
  onSelectSegment: (which: 'start' | 'end', seg: TranscriptionSegment) => void;
  onFinish: () => void;
}

function isInAdSegment(time: number, adSegments: AdSegment[]): boolean {
  return adSegments.some(ad => time >= ad.startTime && time <= ad.endTime);
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function TranscriptionView({
  segments, adSegments, currentTime, onSeek,
  selectingAd, rangeStart, rangeEnd, onSelectSegment, onFinish,
}: TranscriptionViewProps) {
  const activeRef = useRef<HTMLDivElement>(null);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [currentTime]);

  if (segments.length === 0) {
    return <p className="text-gray-500 text-sm">No transcription available</p>;
  }

  const rangeStartIndex = rangeStart ? segments.findIndex(s => s.start === rangeStart.start && s.end === rangeStart.end) : -1;
  const rangeEndIndex = rangeEnd ? segments.findIndex(s => s.start === rangeEnd.start && s.end === rangeEnd.end) : -1;

  // Confirmed range (both start and end selected)
  const confirmedMin = rangeStartIndex >= 0 && rangeEndIndex >= 0
    ? Math.min(rangeStartIndex, rangeEndIndex) : -1;
  const confirmedMax = rangeStartIndex >= 0 && rangeEndIndex >= 0
    ? Math.max(rangeStartIndex, rangeEndIndex) : -1;

  // Preview range on hover (only when picking end)
  const previewMin = selectingAd === 'picking-end' && rangeStartIndex >= 0 && hoverIndex !== null
    ? Math.min(rangeStartIndex, hoverIndex) : -1;
  const previewMax = selectingAd === 'picking-end' && rangeStartIndex >= 0 && hoverIndex !== null
    ? Math.max(rangeStartIndex, hoverIndex) : -1;

  function handleClick(seg: TranscriptionSegment) {
    if (selectingAd === 'picking-start') {
      onSelectSegment('start', seg);
    } else if (selectingAd === 'picking-end') {
      onSelectSegment('end', seg);
    } else {
      onSeek?.(seg.start);
    }
  }

  const bannerText = selectingAd === 'picking-start'
    ? 'Click a transcript segment to mark ad start'
    : selectingAd === 'picking-end' && !rangeEnd
      ? 'Click another segment to mark ad end — Esc to cancel'
      : rangeEnd
        ? null // will show finish button
        : null;

  return (
    <div className="max-h-96 overflow-y-auto space-y-1 pr-2">
      {bannerText && (
        <div className="sticky top-0 z-10 bg-yellow-900/80 text-yellow-200 text-xs px-3 py-1.5 rounded mb-1">
          {bannerText}
        </div>
      )}
      {rangeEnd && selectingAd === null && (
        <div className="sticky top-0 z-10 bg-blue-900/80 text-blue-200 text-xs px-3 py-1.5 rounded mb-1 flex items-center gap-2">
          <span>Range selected — {formatTime(Math.min(rangeStart!.start, rangeEnd.start))} to {formatTime(Math.max(rangeStart!.end, rangeEnd.end))}</span>
        </div>
      )}
      {rangeEnd && selectingAd === 'picking-end' && (
        <div className="sticky top-0 z-10 bg-blue-900/80 text-blue-200 text-xs px-3 py-1.5 rounded mb-1 flex items-center gap-2">
          <span>Range selected</span>
          <button
            onClick={onFinish}
            className="px-2 py-0.5 bg-blue-600 rounded text-xs hover:bg-blue-700 text-white"
          >
            Finish
          </button>
        </div>
      )}
      {segments.map((seg, i) => {
        const isActive = currentTime >= seg.start && currentTime < seg.end;
        const isAd = isInAdSegment(seg.start, adSegments);
        const isRangeAnchor = rangeStart && seg.start === rangeStart.start && seg.end === rangeStart.end;
        const isInConfirmedRange = confirmedMin >= 0 && i >= confirmedMin && i <= confirmedMax;
        const isInPreview = previewMin >= 0 && i >= previewMin && i <= previewMax;

        return (
          <div
            key={i}
            ref={isActive ? activeRef : undefined}
            onClick={() => handleClick(seg)}
            onMouseEnter={() => selectingAd && setHoverIndex(i)}
            onMouseLeave={() => selectingAd && setHoverIndex(null)}
            className={`px-3 py-1.5 rounded cursor-pointer text-sm transition-colors ${
              isRangeAnchor
                ? 'bg-yellow-700/40 text-yellow-200 border border-yellow-500'
                : isInConfirmedRange
                  ? 'bg-blue-800/40 text-blue-200 border border-blue-500/50'
                  : isInPreview
                    ? 'bg-blue-800/20 text-blue-200 border border-blue-500/30'
                    : isActive
                      ? 'bg-purple-600/30 text-white'
                      : isAd
                        ? 'bg-red-900/30 text-red-300'
                        : selectingAd
                          ? 'text-gray-300 hover:bg-gray-700 border border-transparent hover:border-gray-600'
                          : 'text-gray-300 hover:bg-gray-800'
            }`}
          >
            <span className="text-xs text-gray-500 mr-2">{formatTime(seg.start)}</span>
            {seg.text}
            {isRangeAnchor && (
              <span className="ml-2 text-xs bg-yellow-600 text-yellow-100 px-1.5 py-0.5 rounded">Start</span>
            )}
            {isInConfirmedRange && rangeEnd && seg.start === rangeEnd.start && seg.end === rangeEnd.end && (
              <span className="ml-2 text-xs bg-blue-600 text-blue-100 px-1.5 py-0.5 rounded">End</span>
            )}
          </div>
        );
      })}
    </div>
  );
}
