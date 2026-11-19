import React from 'react';

interface DashboardHeaderProps {
  totalActions: number;
}

export const DashboardHeader: React.FC<DashboardHeaderProps> = ({ totalActions }) => {
  return (
    <div className="mb-8 flex justify-between items-end">
      <div>
        <h2 className="text-3xl font-bold text-slate-800">Operations Dashboard</h2>
        <p className="text-slate-500 mt-2 text-lg">Review AI-analyzed corporate actions and validate impacts.</p>
      </div>
      <div className="text-right">
        <span className="text-sm font-medium text-slate-400 uppercase tracking-widest">Total Actions</span>
        <div className="text-4xl font-black text-indigo-600">{totalActions}</div>
      </div>
    </div>
  );
};
