// App.js
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import IncidentDetail from './pages/IncidentDetail';
import RcaForm from './pages/RcaForm';

export default function App() {
  return ( 
   
    <BrowserRouter>
      {/* Wrapper to ensure the background is seamless */}
      <div style={{   minHeight: '100vh',
  background: '#0f172a'}}>
  <div style={{ width: '100%', maxWidth: '1200px', padding: '0px' }}></div>
       
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/incident/:id" element={<IncidentDetail />} />
          <Route path="/rca/:id" element={<RcaForm />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}