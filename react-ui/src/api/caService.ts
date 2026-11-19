import axios from 'axios';
import { CorporateAction } from '../types';

export const caService = {
  getPendingActions: async (signal?: AbortSignal): Promise<CorporateAction[]> => {
    const response = await axios.get('/api/ca/pending', { signal });
    return response.data;
  },

  approveAction: async (id: number): Promise<void> => {
    await axios.post(`/api/ca/${id}/approve`);
  }
};
