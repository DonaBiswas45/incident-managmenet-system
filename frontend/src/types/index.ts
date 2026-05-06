export interface Signal {
  id: string;
  componentId: string;
  componentType: string;
  errorCode: string;
  errorMessage: string;
  severity: string;
  workItemId: string;
  receivedAt: string;
}

export interface WorkItem {
  id: string;
  componentId: string;
  componentType: string;
  title: string;
  priority: string;
  status: string;
  signalCount: number;
  assignedTo: string | null;
  firstSignalAt: string;
  lastSignalAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface Rca {
  id: string;
  workItemId: string;
  rootCauseCategory: string;
  rootCauseDescription: string;
  fixApplied: string;
  preventionSteps: string;
  incidentStartTime: string;
  incidentEndTime: string;
  mttrMinutes: number;
  submittedBy: string;
  isComplete: boolean;
}

export interface StatusHistory {
  id: string;
  workItemId: string;
  fromStatus: string;
  toStatus: string;
  changedBy: string;
  changedAt: string;
}
