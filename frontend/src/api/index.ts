import axios from 'axios';
import { WorkItem, Signal, Rca, StatusHistory } from '../types';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

export const getWorkItems = () =>
  api.get<WorkItem[]>('/api/workitems').then(r => r.data);

export const getWorkItemById = (id: string) =>
  api.get<WorkItem>(`/api/workitems/${id}`).then(r => r.data);

export const updateStatus = (id: string, status: string, changedBy: string) =>
  api.patch<WorkItem>(`/api/workitems/${id}/status`, { status, changedBy }).then(r => r.data);

export const getStatusHistory = (id: string) =>
  api.get<StatusHistory[]>(`/api/workitems/${id}/history`).then(r => r.data);

export const getSignalsByWorkItem = (workItemId: string) =>
  api.get<Signal[]>(`/api/signals/workitem/${workItemId}`).then(r => r.data);

export const submitRca = (workItemId: string, rca: Partial<Rca>) =>
  api.post<Rca>(`/api/rca/${workItemId}`, rca).then(r => r.data);

export const getRca = (workItemId: string) =>
  api.get<Rca>(`/api/rca/${workItemId}`).then(r => r.data);
