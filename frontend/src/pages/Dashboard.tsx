import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getWorkItems } from '../api';
import { WorkItem } from '../types';

const priorityColors: Record<string, string> = {
  P0: '#ff4444',
  P1: '#ff8800',
  P2: '#ffcc00',
  P3: '#44bb44',
};

const statusColors: Record<string, string> = {
  OPEN: '#ff4444',
  INVESTIGATING: '#ff8800',
  RESOLVED: '#44bb44',
  CLOSED: '#888888',
};

export default function Dashboard() {
  const [workItems, setWorkItems] = useState<WorkItem[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchWorkItems = async () => {
    try {
      const data = await getWorkItems();
      setWorkItems(data);
    } catch (err) {
      console.error('Failed to fetch work items', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWorkItems();
    // Poll every 5 seconds
    const interval = setInterval(fetchWorkItems, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'monospace' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ color: '#fff', margin: 0 }}>🚨 Incident Management System</h1>
        <span style={{ color: '#888', fontSize: '12px' }}>Auto-refreshes every 5s</span>
      </div>

      <div style={{ marginTop: '8px', marginBottom: '24px', display: 'flex', gap: '16px' }}>
        {['P0', 'P1', 'P2', 'P3'].map(p => (
          <span key={p} style={{
            background: priorityColors[p],
            color: '#000',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: 'bold'
          }}>
            {p}: {workItems.filter(w => w.priority === p && w.status !== 'CLOSED').length} active
          </span>
        ))}
      </div>

      {loading ? (
        <p style={{ color: '#888' }}>Loading incidents...</p>
      ) : workItems.length === 0 ? (
        <p style={{ color: '#888' }}>No incidents. System healthy ✅</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #333' }}>
              {['Priority', 'Component', 'Type', 'Title', 'Status', 'Signals', 'Created'].map(h => (
                <th key={h} style={{ color: '#888', textAlign: 'left', padding: '8px', fontSize: '12px' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {workItems
              .sort((a, b) => a.priority.localeCompare(b.priority))
              .map(wi => (
                <tr
                  key={wi.id}
                  onClick={() => navigate(`/incident/${wi.id}`)}
                  style={{
                    borderBottom: '1px solid #222',
                    cursor: 'pointer',
                    transition: 'background 0.2s'
                  }}
                  onMouseEnter={e => (e.currentTarget.style.background = '#1a1a1a')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <td style={{ padding: '12px 8px' }}>
                    <span style={{
                      background: priorityColors[wi.priority],
                      color: '#000',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontWeight: 'bold',
                      fontSize: '12px'
                    }}>{wi.priority}</span>
                  </td>
                  <td style={{ color: '#fff', padding: '12px 8px' }}>{wi.componentId}</td>
                  <td style={{ color: '#aaa', padding: '12px 8px', fontSize: '12px' }}>{wi.componentType}</td>
                  <td style={{ color: '#fff', padding: '12px 8px' }}>{wi.title}</td>
                  <td style={{ padding: '12px 8px' }}>
                    <span style={{
                      background: statusColors[wi.status],
                      color: wi.status === 'CLOSED' ? '#fff' : '#000',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '12px'
                    }}>{wi.status}</span>
                  </td>
                  <td style={{ color: '#aaa', padding: '12px 8px' }}>{wi.signalCount}</td>
                  <td style={{ color: '#666', padding: '12px 8px', fontSize: '11px' }}>
                    {new Date(wi.createdAt).toLocaleString()}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
