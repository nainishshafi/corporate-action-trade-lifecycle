import type {
  BankingDataGateway,
  PositionRecord,
  SecurityRecord,
} from "../../domain/bankingDataGateway.js";

const MOCK_SECURITIES: readonly SecurityRecord[] = [
  { id: "AAPL", name: "Apple Inc.", isin: "US0378331005", price: 180.5, currency: "USD" },
  { id: "MSFT", name: "Microsoft Corp.", isin: "US5949181045", price: 400.25, currency: "USD" },
  { id: "GOOGL", name: "Alphabet Inc.", isin: "US02079K3059", price: 140.1, currency: "USD" },
];

const MOCK_POSITIONS: readonly PositionRecord[] = [
  { accountId: "ACC-001", securityId: "AAPL", quantity: 100 },
  { accountId: "ACC-002", securityId: "AAPL", quantity: 50 },
  { accountId: "ACC-003", securityId: "MSFT", quantity: 200 },
  { accountId: "ACC-004", securityId: "AAPL", quantity: 75 },
];

/**
 * Demo-only banking adapter backed by in-memory fixtures.
 */
export function createMockBankingDataGateway(): BankingDataGateway {
  return {
    findPositionsBySecurityId(securityId: string): readonly PositionRecord[] {
      return MOCK_POSITIONS.filter((position) => position.securityId === securityId);
    },

    findSecurityByIdentifier(identifier: string): SecurityRecord | undefined {
      return MOCK_SECURITIES.find(
        (security) => security.id === identifier || security.isin === identifier
      );
    },
  };
}
