export interface SecurityRecord {
  id: string;
  name: string;
  isin: string;
  price: number;
  currency: string;
}

export interface PositionRecord {
  accountId: string;
  securityId: string;
  quantity: number;
}

export interface BankingDataGateway {
  findPositionsBySecurityId(securityId: string): readonly PositionRecord[];
  findSecurityByIdentifier(identifier: string): SecurityRecord | undefined;
}
