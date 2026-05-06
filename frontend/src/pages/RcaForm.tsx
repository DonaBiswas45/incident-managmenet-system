import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { submitRca, updateStatus } from '../api';

const ROOT_CAUSE_CATEGORIES = [
  'NETWORK_FAILURE',
  'CONFIG_ERROR',
  'HARDWARE_FAILURE',
  'SOFTWARE_BUG',
  'CAPACITY_EXHAUSTION',
  'HUMAN_ERROR',
  'THIRD_PARTY',
];

export default function RcaForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    rootCauseCategory: '',
    rootCauseDescription: '',
    fixApplied: '',
    preventionSteps: '',
    incidentStartTime: '',
    incidentEndTime: '',
    submittedBy: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async () => {
    if (!id) return;
    setSubmitting(true);
    setError('');
    try {
      await submitRca(id, {
        ...form,
        incidentStartTime: new Date(form.incidentStartTime).toISOString(),
        incidentEndTime: new Date(form.incidentEndTime).toISOString(),
      });
      await updateStatus(id, 'CLOSED', form.submittedBy || 'engineer');
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to submit RCA');
    } finally {
      setSubmitting(false);
    }
  };

  const inputStyle = {
    width: '100%', background: '#1a1a1a', border: '1px solid #333',
    color: '#fff', padding: '8px', borderRadius: '4px',
    fontFamily: 'monospace', fontSize: '13px', boxSizing: 'border-box' as const,
  };

  const labelStyle = { color: '#888', fontSize: '12px', display: 'block', marginBottom: '4px' };

  return (
    <div style={{ padding: '24px', fontFamily: 'monospace', color: '#fff', maxWidth: '700px' }}>
      <button
        onClick={() => navigate(`/incident/${id}`)}
        style={{ background: 'none', border: '1px solid #444', color: '#888', padding: '4px 12px', cursor: 'pointer', borderRadius: '4px', marginBottom: '16px' }}
      >← Back</button>

      <h2>📝 Root Cause Analysis</h2>
      <p style={{ color: '#666', fontSize: '12px' }}>All fields required to close this incident.</p>

      {error && <p style={{ color: '#ff4444', background: '#2a0000', padding: '8px', borderRadius: '4px' }}>{error}</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <label style={labelStyle}>Root Cause Category *</label>
          <select name="rootCauseCategory" value={form.rootCauseCategory} onChange={handleChange} style={inputStyle}>
            <option value="">Select category...</option>
            {ROOT_CAUSE_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        <div>
          <label style={labelStyle}>Root Cause Description *</label>
          <textarea name="rootCauseDescription" value={form.rootCauseDescription} onChange={handleChange}
            style={{ ...inputStyle, height: '80px', resize: 'vertical' }} placeholder="What caused this incident?" />
        </div>

        <div>
          <label style={labelStyle}>Fix Applied *</label>
          <textarea name="fixApplied" value={form.fixApplied} onChange={handleChange}
            style={{ ...inputStyle, height: '80px', resize: 'vertical' }} placeholder="What was done to fix it?" />
        </div>

        <div>
          <label style={labelStyle}>Prevention Steps *</label>
          <textarea name="preventionSteps" value={form.preventionSteps} onChange={handleChange}
            style={{ ...inputStyle, height: '80px', resize: 'vertical' }} placeholder="How to prevent recurrence?" />
        </div>

        <div style={{ display: 'flex', gap: '16px' }}>
          <div style={{ flex: 1 }}>
            <label style={labelStyle}>Incident Start Time *</label>
            <input type="datetime-local" name="incidentStartTime" value={form.incidentStartTime}
              onChange={handleChange} style={inputStyle} />
          </div>
          <div style={{ flex: 1 }}>
            <label style={labelStyle}>Incident End Time *</label>
            <input type="datetime-local" name="incidentEndTime" value={form.incidentEndTime}
              onChange={handleChange} style={inputStyle} />
          </div>
        </div>

        <div>
          <label style={labelStyle}>Submitted By *</label>
          <input type="text" name="submittedBy" value={form.submittedBy} onChange={handleChange}
            style={inputStyle} placeholder="Your name" />
        </div>

        <button
          onClick={handleSubmit}
          disabled={submitting}
          style={{
            background: '#1a6b1a', color: '#fff', border: 'none',
            padding: '12px', borderRadius: '4px', cursor: 'pointer',
            fontSize: '14px', fontFamily: 'monospace'
          }}
        >
          {submitting ? 'Submitting...' : '✅ Submit RCA & Close Incident'}
        </button>
      </div>
    </div>
  );
}
