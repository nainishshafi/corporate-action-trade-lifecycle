export interface PositionImpact {
  id: number;
  accountId: string;
  originalQuantity: number;
  newQuantity: number;
}

export interface CorporateAction {
  id: number;
  securityId: string;
  actionType: string;
  ratio: string;
  recordDate: string;
  exDate: string;
  payDate: string;
  status: 'PENDING' | 'ANALYZED' | 'APPROVED' | 'REJECTED' | 'EXECUTED';
  aiAnalysis: string;
  impacts: PositionImpact[];
}
