import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  /** Current page index (0-based) */
  page: number;
  /** Number of items per page */
  pageSize: number;
  /** Total number of items across all pages */
  totalElements: number;
  /** Total number of pages */
  totalPages: number;
  /** Whether the current page is the first */
  isFirst: boolean;
  /** Whether the current page is the last */
  isLast: boolean;
  /** Disables all controls when true (e.g. while loading) */
  isLoading?: boolean;
  /** Called when the user navigates to a different page */
  onPageChange: (page: number) => void;
  /** Called when the user changes the page size; should also reset page to 0 */
  onPageSizeChange: (size: number) => void;
  /** Available page-size options — defaults to [10, 20, 50] */
  pageSizeOptions?: number[];
  /** Label used in "Affichage de X à Y sur Z <label>" — defaults to "éléments" */
  label?: string;
}

/**
 * Reusable Attijariwafa bank GED-AWB pagination footer.
 *
 * Renders: page-size selector · Précédent · numbered pages with ellipses · Suivant
 * plus the "Affichage de X à Y sur Z" summary text.
 *
 * Nothing is rendered when totalElements === 0.
 */
export const Pagination: React.FC<PaginationProps> = ({
  page,
  pageSize,
  totalElements,
  totalPages,
  isFirst,
  isLast,
  isLoading = false,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 20, 50],
  label = 'éléments',
}) => {
  if (totalElements === 0) return null;

  const startItem = page * pageSize + 1;
  const endItem = Math.min((page + 1) * pageSize, totalElements);

  /** Generates an array of page indices (numbers) or ellipsis markers (strings). */
  const getPageNumbers = (): (number | string)[] => {
    const pages: (number | string)[] = [];

    if (totalPages <= 7) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else {
      pages.push(0);
      if (page > 2) pages.push('...');

      const start = Math.max(1, page - 1);
      const end = Math.min(totalPages - 2, page + 1);
      for (let i = start; i <= end; i++) pages.push(i);

      if (page < totalPages - 3) pages.push('...');
      pages.push(totalPages - 1);
    }

    return pages;
  };

  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onPageSizeChange(Number(e.target.value));
  };

  const prevDisabled = isFirst || isLoading;
  const nextDisabled = isLast || isLoading;

  const btnBase =
    'flex items-center gap-1 px-2.5 py-1 rounded border text-xs font-medium transition-colors';
  const btnEnabled =
    'bg-white text-brand-text border-brand-border hover:bg-slate-100 hover:text-brand-primary';
  const btnDisabled =
    'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed';

  return (
    <div className="p-4 border-t border-brand-border bg-slate-50/50 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-brand-muted">
      {/* ── Summary text ── */}
      <div className="flex items-center gap-4">
        <span>
          Affichage de <strong className="text-brand-text">{startItem}</strong> à{' '}
          <strong className="text-brand-text">{endItem}</strong> sur{' '}
          <strong className="text-brand-text">{totalElements}</strong> {label}
        </span>
        <span className="hidden sm:inline text-slate-300">|</span>
        <span>
          Page <strong className="text-brand-text">{page + 1}</strong> sur{' '}
          <strong className="text-brand-text">{totalPages}</strong>
        </span>
      </div>

      {/* ── Controls ── */}
      <div className="flex items-center gap-4">
        {/* Page-size selector */}
        <div className="flex items-center gap-2">
          <span>Par page :</span>
          <select
            value={pageSize}
            onChange={handlePageSizeChange}
            disabled={isLoading}
            className="bg-white border border-brand-border rounded px-2 py-1 text-xs font-medium text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary disabled:opacity-50"
          >
            {pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </select>
        </div>

        {/* Navigation buttons */}
        <div className="flex items-center gap-1">
          {/* Previous */}
          <button
            onClick={() => onPageChange(Math.max(0, page - 1))}
            disabled={prevDisabled}
            className={`${btnBase} ${prevDisabled ? btnDisabled : btnEnabled}`}
          >
            <ChevronLeft className="w-3.5 h-3.5" />
            <span>Précédent</span>
          </button>

          {/* Page numbers */}
          <div className="flex items-center gap-1">
            {getPageNumbers().map((pNum, idx) =>
              typeof pNum === 'number' ? (
                <button
                  key={idx}
                  onClick={() => onPageChange(pNum)}
                  disabled={isLoading}
                  className={`w-7 h-7 rounded text-xs font-semibold flex items-center justify-center transition-colors ${
                    page === pNum
                      ? 'bg-brand-primary text-white shadow-sm'
                      : 'bg-white text-brand-text border border-brand-border hover:bg-red-50 hover:text-brand-primary'
                  }`}
                >
                  {pNum + 1}
                </button>
              ) : (
                <span key={idx} className="px-1 text-slate-400">
                  {pNum}
                </span>
              )
            )}
          </div>

          {/* Next */}
          <button
            onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
            disabled={nextDisabled}
            className={`${btnBase} ${nextDisabled ? btnDisabled : btnEnabled}`}
          >
            <span>Suivant</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
