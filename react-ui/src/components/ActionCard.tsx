import React from 'react';
import { Activity, AlertTriangle, CheckCircle, XCircle } from 'lucide-react';
import { CorporateAction } from '../types';
import { DecisionType } from '../hooks/useCorporateActions';

const STATUS_BADGE_CLASSES: Record<CorporateAction['status'], string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  ANALYZED: 'bg-amber-100 text-amber-700',
  APPROVED: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-rose-100 text-rose-700',
  EXECUTED: 'bg-indigo-100 text-indigo-700',
};

interface ActionCardProps {
  action: CorporateAction;
  pendingDecision?: DecisionType;
  onApprove: (id: number) => Promise<void>;
  onReject: (id: number) => Promise<void>;
}

export const ActionCard: React.FC<ActionCardProps> = ({
  action,
  pendingDecision,
  onApprove,
  onReject,
}) => {
  const isDecisionPending = pendingDecision !== undefined;

  return (
    <div className="bg-white rounded-2xl shadow-xl overflow-hidden border border-slate-200 hover:border-indigo-300 transition-colors">
      <div className="p-8">
        <div className="flex justify-between items-start mb-6">
          <div className="flex items-center gap-4">
            <div className="bg-indigo-100 p-4 rounded-xl">
              <Activity className="w-8 h-8 text-indigo-600" />
            </div>
            <div>
              <h3 className="text-2xl font-bold text-slate-900">{action.securityId} - {action.actionType}</h3>
              <p className="text-slate-500 font-medium">Notification ID: CA-{action.id.toString().padStart(4, '0')}</p>
            </div>
          </div>
          <div className="flex flex-col items-end gap-2">
            <span className={`px-4 py-1.5 rounded-full text-sm font-bold tracking-wide ${STATUS_BADGE_CLASSES[action.status]}`}>
              {action.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-indigo-50 rounded-xl p-6 border border-indigo-100">
              <div className="flex items-center gap-2 mb-4">
                <AlertTriangle className="w-5 h-5 text-indigo-600" />
                <h4 className="font-bold text-indigo-900 uppercase tracking-wider text-sm">AI Analysis Engine</h4>
              </div>
              <p className="text-indigo-900/80 leading-relaxed text-lg italic">
                "{action.aiAnalysis}"
              </p>
              <div className="mt-4">
                <div className="bg-white/60 inline-flex px-3 py-1 rounded-lg text-xs font-bold text-indigo-700">
                  DECLARED RATIO: {action.ratio}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                <span className="text-xs font-bold text-slate-400 uppercase">Record Date</span>
                <div className="font-bold text-slate-700">{action.recordDate}</div>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                <span className="text-xs font-bold text-slate-400 uppercase">Ex-Date</span>
                <div className="font-bold text-slate-700">{action.exDate}</div>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-100">
                <span className="text-xs font-bold text-slate-400 uppercase">Pay Date</span>
                <div className="font-bold text-slate-700">{action.payDate}</div>
              </div>
            </div>
          </div>

          <div className="bg-slate-50 rounded-xl p-6 border border-slate-100">
            <h4 className="font-bold text-slate-800 mb-4 uppercase tracking-wider text-sm">Position Impact</h4>
            <div className="space-y-4">
              {action.impacts.map((impact) => (
                <div key={impact.id} className="flex justify-between items-center bg-white p-3 rounded-lg shadow-sm border border-slate-100">
                  <div>
                    <div className="text-xs font-bold text-slate-400 uppercase">{impact.accountId}</div>
                    <div className="font-bold text-slate-700">{impact.originalQuantity} Shares</div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs font-bold text-indigo-400 uppercase">NEW QTY</div>
                    <div className="font-bold text-indigo-600">{impact.newQuantity} Shares</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {action.status === 'ANALYZED' && (
          <div className="mt-8 flex justify-end gap-4 border-t border-slate-100 pt-6">
            <button
              type="button"
              disabled={isDecisionPending}
              onClick={() => {
                void onReject(action.id);
              }}
              className="flex items-center gap-2 px-6 py-2.5 rounded-xl font-bold text-slate-600 hover:bg-slate-100 transition-colors disabled:text-slate-300 disabled:hover:bg-transparent disabled:cursor-not-allowed"
            >
              <XCircle className="w-5 h-5" />
              {pendingDecision === 'reject' ? 'Rejecting...' : 'Reject'}
            </button>
            <button
              type="button"
              disabled={isDecisionPending}
              onClick={() => {
                void onApprove(action.id);
              }}
              className="flex items-center gap-2 px-8 py-2.5 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 shadow-lg shadow-indigo-200 transition-all active:scale-95 disabled:bg-indigo-300 disabled:shadow-none disabled:cursor-not-allowed disabled:active:scale-100"
            >
              <CheckCircle className="w-5 h-5" />
              {pendingDecision === 'approve' ? 'Approving...' : 'Approve & Execute'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
