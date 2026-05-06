import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getWorkItems } from '../api';
import { WorkItem } from '../types';

const priorityColors: Record<string, string> = {
  P0: '#cc3333',
  P1: '#cc7700',
  P2: '#ccaa00',
  P3: '#33aa33',
};

const statusColors: Record<string, string> = {
  OPEN: '#cc3333',
  INVESTIGATING: '#cc7700',
  RESOLVED: '#33aa33',
  CLOSED: '#555555',
};

const priorityLabels: Record<string, string> = {
  P0: 'CRITICAL',
  P1: 'HIGH',
  P2: 'MEDIUM',
  P3: 'LOW',
};

export default function Dashboard() {
  const [workItems, setWorkItems] = useState<WorkItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastRefresh, setLastRefresh] = useState(new Date());
  const [countdown, setCountdown] = useState(5);
  const navigate = useNavigate();

  const fetchWorkItems = async () => {
    try {
      const data = await getWorkItems();
      setWorkItems(data);
      setLastRefresh(new Date());
      setCountdown(5);
    } catch (err) {
      console.error('Failed to fetch work items', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWorkItems();
    const interval = setInterval(fetchWorkItems, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown(c => (c <= 1 ? 5 : c - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const activeCount = workItems.filter(w => w.status !== 'CLOSED').length;
  const totalSignals = workItems.reduce((sum, w) => sum + w.signalCount, 0);

  return (
    <div style={{
      minHeight: '100vh',
      background: '#0a0a0a',
      color: '#ccc',
      fontFamily: '"Courier New", monospace',
      padding: '0',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 24px',
        borderBottom: '1px solid #1a1a1a',
        background: '#0d0d0d',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{
            background: '#1a1a1a',
            color: '#33aa33',
            padding: '4px 12px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: 'bold',
            letterSpacing: '2px',
          }}>IMS</span>
          <span style={{ color: '#666', fontSize: '13px', letterSpacing: '2px' }}>
            INCIDENT MANAGEMENT SYSTEM
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <span style={{ color: '#cc3333', fontSize: '12px' }}>
            ● {activeCount} ACTIVE
          </span>
          <span style={{ color: '#444', fontSize: '12px' }}>
            REFRESH IN {countdown}s
          </span>
        </div>
      </div>

      <div style={{ padding: '24px' }}>
        {/* Priority Cards */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '16px' }}>
          {['P0', 'P1', 'P2', 'P3'].map(p => {
            const active = workItems.filter(w => w.priority === p && w.status !== 'CLOSED');
            return (
              <div key={p} style={{
                background: '#111',
                border: `1px solid ${active.length > 0 ? priorityColors[p] + '44' : '#1a1a1a'}`,
                borderRadius: '6px',
                padding: '16px',
              }}>
                <div style={{ color: '#555', fontSize: '11px', letterSpacing: '2px', marginBottom: '8px' }}>
                  {priorityLabels[p]}
                </div>
                <div style={{
                  fontSize: '36px',
                  fontWeight: 'bold',
                  color: active.length > 0 ? priorityColors[p] : '#333',
                  marginBottom: '8px',
                }}>
                  {active.length}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ color: '#444', fontSize: '11px' }}>
                    {active.length === 1 ? '1 active incident' : `${active.length} active incidents`}
                  </span>
                  <span style={{
                    background: priorityColors[p],
                    color: '#000',
                    padding: '2px 6px',
                    borderRadius: '3px',
                    fontSize: '10px',
                    fontWeight: 'bold',
                  }}>{p}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Stats Bar */}
        <div style={{
          background: '#111',
          border: '1px solid #1a1a1a',
          borderRadius: '6px',
          padding: '12px 20px',
          display: 'flex',
          gap: '32px',
          marginBottom: '16px',
          fontSize: '12px',
        }}>
          <span>TOTAL: <strong style={{ color: '#fff' }}>{workItems.length}</strong></span>
          <span>ACTIVE: <strong style={{ color: '#cc3333' }}>{activeCount}</strong></span>
          <span>SIGNALS: <strong style={{ color: '#cc7700' }}>{totalSignals}</strong></span>
          <span>LAST REFRESH: <strong style={{ color: '#33aa33' }}>
            {lastRefresh.toTimeString().slice(0, 8)}
          </strong></span>
        </div>

        {/* Incident Table */}
        <div style={{
          background: '#111',
          border: '1px solid #1a1a1a',
          borderRadius: '6px',
          overflow: 'hidden',
        }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #1a1a1a' }}>
                {['PRI', 'STATUS', 'TITLE / COMPONENT', 'TYPE', 'SIGS'].map(h => (
                  <th key={h} style={{
                    color: '#444',
                    textAlign: 'left',
                    padding: '10px 16px',
                    fontSize: '11px',
                    letterSpacing: '1px',
                    fontWeight: 'normal',
                  }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ padding: '24px', color: '#444', textAlign: 'center' }}>
                    Loading incidents...
                  </td>
                </tr>
              ) : workItems.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '24px', color: '#444', textAlign: 'center' }}>
                    No incidents. System healthy ✅
                  </td>
                </tr>
              ) : (
                workItems
                  .sort((a, b) => a.priority.localeCompare(b.priority))
                  .map(wi => (
                    <tr
                      key={wi.id}
                      onClick={() => navigate(`/incident/${wi.id}`)}
                      style={{
                        borderBottom: '1px solid #151515',
                        cursor: 'pointer',
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = '#161616')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{
                          background: priorityColors[wi.priority],
                          color: '#000',
                          padding: '2px 8px',
                          borderRadius: '3px',
                          fontSize: '11px',
                          fontWeight: 'bold',
                        }}>{wi.priority}</span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{
                          color: statusColors[wi.status],
                          fontSize: '12px',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                        }}>
                          <span>●</span>{wi.status}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <div style={{ color: '#ccc', fontSize: '13px' }}>{wi.title}</div>
                        <div style={{ color: '#444', fontSize: '11px', marginTop: '2px' }}>{wi.componentId}</div>
                      </td>
                      <td style={{ padding: '12px 16px', color: '#666', fontSize: '12px' }}>
                        {wi.componentType}
                      </td>
                      <td style={{ padding: '12px 16px', color: '#888', fontSize: '13px' }}>
                        {wi.signalCount}
                      </td>
                    </tr>
                  ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
