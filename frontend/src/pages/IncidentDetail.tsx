import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getWorkItemById, getSignalsByWorkItem, getStatusHistory, updateStatus, getRca } from '../api';
import { WorkItem, Signal, StatusHistory, Rca } from '../types';

const statusFlow = ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED'];

export default function IncidentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [workItem, setWorkItem] = useState<WorkItem | null>(null);
  const [signals, setSignals] = useState<Signal[]>([]);
  const [history, setHistory] = useState<StatusHistory[]>([]);
  const [rca, setRca] = useState<Rca | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    Promise.all([
      getWorkItemById(id),
      getSignalsByWorkItem(id),
      getStatusHistory(id),
      getRca(id).catch(() => null),
    ]).then(([wi, sigs, hist, rcaData]) => {
      setWorkItem(wi);
      setSignals(sigs);
      setHistory(hist);
      setRca(rcaData);
    }).finally(() => setLoading(false));
  }, [id]);

  const handleStatusUpdate = async (newStatus: string) => {
    if (!id) return;
    setUpdating(true);
    setError('');
    try {
      const updated = await updateStatus(id, newStatus, 'engineer');
      setWorkItem(updated);
      const hist = await getStatusHistory(id);
      setHistory(hist);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <p style={{ color: '#888', padding: '24px', fontFamily: 'monospace' }}>Loading...</p>;
  if (!workItem) return <p style={{ color: '#888', padding: '24px' }}>Not found</p>;

  const nextStatus = statusFlow[statusFlow.indexOf(workItem.status) + 1];

  return (
    <div style={{ padding: '24px', fontFamily: 'monospace', color: '#fff' }}>
      <button
        onClick={() => navigate('/')}
        style={{ background: 'none', border: '1px solid #444', color: '#888', padding: '4px 12px', cursor: 'pointer', borderRadius: '4px', marginBottom: '16px' }}
      >← Back</button>

      <h2>{workItem.title}</h2>
      <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', flexWrap: 'wrap' }}>
        <span>Priority: <strong style={{ color: workItem.priority === 'P0' ? '#ff4444' : '#fff' }}>{workItem.priority}</strong></span>
        <span>Status: <strong>{workItem.status}</strong></span>
        <span>Signals: <strong>{workItem.signalCount}</strong></span>
        <span>Component: <strong>{workItem.componentId}</strong></span>
      </div>

      {error && <p style={{ color: '#ff4444', background: '#2a0000', padding: '8px', borderRadius: '4px' }}>{error}</p>}

      {nextStatus && (
        <button
          onClick={() => nextStatus === 'CLOSED' ? navigate(`/rca/${id}`) : handleStatusUpdate(nextStatus)}
          disabled={updating}
          style={{
            background: '#1a6b1a', color: '#fff', border: 'none',
            padding: '8px 16px', borderRadius: '4px', cursor: 'pointer',
            marginBottom: '24px', fontSize: '14px'
          }}
        >
          {updating ? 'Updating...' : nextStatus === 'CLOSED' ? '📝 Submit RCA to Close' : `→ Move to ${nextStatus}`}
        </button>
      )}

      {rca && (
        <div style={{ background: '#0a2a0a', padding: '16px', borderRadius: '8px', marginBottom: '24px' }}>
          <h3 style={{ color: '#44bb44', margin: '0 0 8px' }}>✅ RCA Submitted</h3>
          <p><strong>Category:</strong> {rca.rootCauseCategory}</p>
          <p><strong>Root Cause:</strong> {rca.rootCauseDescription}</p>
          <p><strong>Fix Applied:</strong> {rca.fixApplied}</p>
          <p><strong>Prevention:</strong> {rca.preventionSteps}</p>
          <p><strong>MTTR:</strong> {rca.mttrMinutes} minutes</p>
        </div>
      )}

      <h3>Status Timeline</h3>
      <div style={{ marginBottom: '24px' }}>
        {history.map(h => (
          <div key={h.id} style={{ borderLeft: '2px solid #333', paddingLeft: '12px', marginBottom: '8px' }}>
            <span style={{ color: '#888', fontSize: '11px' }}>{new Date(h.changedAt).toLocaleString()}</span>
            <span style={{ marginLeft: '8px' }}>{h.fromStatus} → {h.toStatus}</span>
            <span style={{ color: '#666', marginLeft: '8px', fontSize: '12px' }}>by {h.changedBy}</span>
          </div>
        ))}
      </div>

      <h3>Raw Signals ({signals.length})</h3>
      <div style={{ maxHeight: '300px', overflowY: 'auto', background: '#111', padding: '12px', borderRadius: '8px' }}>
        {signals.length === 0 ? (
          <p style={{ color: '#666' }}>No signals yet</p>
        ) : signals.map(s => (
          <div key={s.id} style={{ borderBottom: '1px solid #222', padding: '8px 0', fontSize: '12px' }}>
            <span style={{ color: '#888' }}>{new Date(s.receivedAt).toLocaleString()}</span>
            <span style={{ color: '#ff8800', marginLeft: '8px' }}>[{s.severity}]</span>
            <span style={{ color: '#aaa', marginLeft: '8px' }}>{s.errorCode}: {s.errorMessage}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
