import React from 'react';
import { Activity, ShieldCheck } from 'lucide-react';

export const Header: React.FC = () => {
  return (
    <header className="bg-indigo-900 text-white p-6 shadow-lg">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
        <div className="flex items-center gap-3">
          <Activity className="w-8 h-8 text-indigo-300" />
          <h1 className="text-2xl font-bold tracking-tight">Corporate Action Lifecycle Demo</h1>
        </div>
        <div className="flex items-center gap-2 text-sm bg-indigo-800 px-4 py-2 rounded-full">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Human-in-the-Loop Active</span>
        </div>
      </div>
    </header>
  );
};
