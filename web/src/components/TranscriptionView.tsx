import { useEffect, useRef, useState, useCallback } from 'react';
import { TranscriptionSegment, AdSegment } from '../api/client';

interface TranscriptionViewProps {
  segments: TranscriptionSegment[];
  adSegments: AdSegment[];
  currentTime: number;
  onSeek?: (time: number) => void;
  editable?: boolean;
  onToggleAd?: (start: number, end: number, isAd: boolean) => void;
}

function isInAdSegment(time: number, adSegments: AdSegment[]): boolean {
  return adSegments.some(ad => time >= ad.startTime && time <= ad.endTime);
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function TranscriptionView({ segments, adSegments, currentTime, onSeek, editable, onToggleAd }: TranscriptionViewProps) {
  const activeRef = useRef<HTMLDivElement>(null);
  const [rangeStart, setRangeStart] = useState<{ start: number; end: number } | null>(null);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [currentTime]);

  // Clear range selection when leaving edit mode
  useEffect(() => {
    if (!editable) setRangeStart(null);
  }, [editable]);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') setRangeStart(null);
  }, []);

  useEffect(() => {
    if (rangeStart) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [rangeStart, handleKeyDown]);

  if (segments.length === 0) {
    return <p className="text-gray-500 text-sm">No transcription available</p>;
  }

  // Compute the preview range (from rangeStart to hovered segment)
  const rangeStartIndex = rangeStart ? segments.findIndex(s => s.start === rangeStart.start) : -1;
  const previewMin = rangeStart && hoverIndex !== null
    ? Math.min(rangeStartIndex, hoverIndex)
    : -1;
  const previewMax = rangeStart && hoverIndex !== null
    ? Math.max(rangeStartIndex, hoverIndex)
    : -1;

  function handleClick(seg: TranscriptionSegment, isAd: boolean) {
    if (editable && onToggleAd) {
      if (isAd && !rangeStart) {
        // Single-click unmark ad segment
        onToggleAd(seg.start, seg.end, false);
      } else if (rangeStart) {
        // Second click — mark range
        const startTime = Math.min(rangeStart.start, seg.start);
        const endTime = Math.max(rangeStart.end, seg.end);
        onToggleAd(startTime, endTime, true);
        setRangeStart(null);
      } else {
        // First click — start range
        setRangeStart({ start: seg.start, end: seg.end });
      }
    } else {
      onSeek?.(seg.start);
    }
  }

  return (
    <div className="max-h-96 overflow-y-auto space-y-1 pr-2">
      {rangeStart && (
        <div className="sticky top-0 z-10 bg-yellow-900/80 text-yellow-200 text-xs px-3 py-1 rounded mb-1">
          Range selecting from {formatTime(rangeStart.start)} — click another segment to mark range, Esc to cancel
        </div>
      )}
      {segments.map((seg, i) => {
        const isActive = currentTime >= seg.start && currentTime < seg.end;
        const isAd = isInAdSegment(seg.start, adSegments);
        const isRangeAnchor = rangeStart && seg.start === rangeStart.start;
        const isInPreview = previewMin >= 0 && i >= previewMin && i <= previewMax;

        return (
          <div
            key={i}
            ref={isActive ? activeRef : undefined}
            onClick={() => handleClick(seg, isAd)}
            onMouseEnter={() => editable && setHoverIndex(i)}
            onMouseLeave={() => editable && setHoverIndex(null)}
            className={`px-3 py-1.5 rounded cursor-pointer text-sm transition-colors ${
              isRangeAnchor
                ? 'bg-yellow-700/40 text-yellow-200 border border-yellow-500'
                : isInPreview
                  ? 'bg-blue-800/30 text-blue-200 border border-blue-500/50'
                  : isActive
                    ? 'bg-purple-600/30 text-white'
                    : isAd
                      ? 'bg-red-900/30 text-red-300 hover:bg-red-900/50'
                      : editable
                        ? 'text-gray-300 hover:bg-gray-700 border border-transparent hover:border-gray-600'
                        : 'text-gray-300 hover:bg-gray-800'
            }`}
          >
            <span className="text-xs text-gray-500 mr-2">{formatTime(seg.start)}</span>
            {seg.text}
            {isRangeAnchor && (
              <span className="ml-2 text-xs bg-yellow-600 text-yellow-100 px-1.5 py-0.5 rounded">Ad start</span>
            )}
            {editable && isAd && !isRangeAnchor && (
              <span className="ml-2 text-xs text-red-400" title="Click to remove ad marking">✕</span>
            )}
          </div>
        );
      })}
    </div>
  );
}
