import React, { useState } from 'react';
import { AlertCircle } from 'lucide-react';
import { Header } from './components/Header';
import { DashboardHeader } from './components/DashboardHeader';
import { ActionCard } from './components/ActionCard';
import { useCorporateActions } from './hooks/useCorporateActions';

const App: React.FC = () => {
  const { actions, loading, error, pendingDecisionById, approveAction, rejectAction } =
    useCorporateActions();
  const [actionError, setActionError] = useState<string | null>(null);

  const handleApprove = async (id: number) => {
    setActionError(null);

    try {
      await approveAction(id);
    } catch {
      setActionError('Failed to approve action. Please try again.');
    }
  };

  const handleReject = async (id: number) => {
    setActionError(null);

    try {
      await rejectAction(id);
    } catch {
      setActionError('Failed to reject action. Please try again.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <Header />

      <main className="max-w-7xl mx-auto p-8">
        <DashboardHeader totalActions={actions.length} />

        {error && (
          <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-xl flex items-center gap-3 text-amber-800">
            <AlertCircle className="w-5 h-5" />
            <span className="font-medium">{error}</span>
          </div>
        )}

        {actionError && (
          <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800">
            <AlertCircle className="w-5 h-5" />
            <span className="font-medium">{actionError}</span>
          </div>
        )}

        {loading ? (
          <div className="flex justify-center p-20">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
          </div>
        ) : actions.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-sm">
            <p className="text-lg font-semibold text-slate-700">No corporate actions awaiting review</p>
            <p className="text-slate-500 mt-1">New AI-analyzed notifications will appear here automatically.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-8">
            {actions.map((action) => (
              <ActionCard
                key={action.id}
                action={action}
                pendingDecision={pendingDecisionById[action.id]}
                onApprove={handleApprove}
                onReject={handleReject}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default App;
