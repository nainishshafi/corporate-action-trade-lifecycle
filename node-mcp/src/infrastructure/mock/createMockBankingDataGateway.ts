import { readFileSync } from "node:fs";
import type {
  BankingDataGateway,
  PositionRecord,
  SecurityRecord,
} from "../../domain/bankingDataGateway.js";

interface MockBankingFixture {
  securities: readonly SecurityRecord[];
  positions: readonly PositionRecord[];
}

const FIXTURE_URL = new URL("../../../../shared/mock-banking-data.json", import.meta.url);

function loadFixtureData(): MockBankingFixture {
  const raw = readFileSync(FIXTURE_URL, "utf8");
  return JSON.parse(raw) as MockBankingFixture;
}

/**
 * Demo-only banking adapter backed by the shared in-memory fixture file.
 */
export function createMockBankingDataGateway(): BankingDataGateway {
  const { securities, positions } = loadFixtureData();

  return {
    findPositionsBySecurityId(securityId: string): readonly PositionRecord[] {
      const normalized = securityId.trim().toUpperCase();
      return positions.filter((position) => position.securityId.toUpperCase() === normalized);
    },

    findSecurityByIdentifier(identifier: string): SecurityRecord | undefined {
      const normalized = identifier.trim().toUpperCase();
      return securities.find(
        (security) =>
          security.id.toUpperCase() === normalized || security.isin.toUpperCase() === normalized
      );
    },
  };
}
