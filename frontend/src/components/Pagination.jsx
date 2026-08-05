export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages == null || totalPages <= 1) return null
  return (
    <div className="row" style={{ justifyContent: 'space-between', marginTop: '0.75rem' }}>
      <button className="btn btn-ghost" type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ← Prev
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn btn-ghost"
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next →
      </button>
    </div>
  )
}
