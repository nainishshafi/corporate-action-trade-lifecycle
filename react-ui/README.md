# Corporate Action Trade Lifecycle - React UI

This repository contains the React-based frontend for the Corporate Action Trade Lifecycle system. It provides the operator-facing Human-in-the-Loop (HITL) dashboard used to review AI-analyzed corporate actions, inspect projected position impacts, and approve eligible actions for backend execution.

## **Technical Stack**
- **React 18**: Component-based UI for the operator workflow.
- **TypeScript 5**: Strict typing for API contracts and UI state.
- **Vite 5**: Fast local development server and production bundling.
- **Tailwind CSS 3**: Utility-first styling for the dashboard experience.
- **Axios**: HTTP client for backend API communication.
- **Vitest + Testing Library**: Component and behavior-driven frontend test coverage.
- **ESLint**: Static analysis for TypeScript and React hook correctness.

## **Frontend Responsibilities**
- Present a clear operator dashboard for pending corporate actions.
- Display AI analysis text and declared corporate action ratios.
- Show projected account-level position impacts before approval.
- Enforce a safe approval UX by preventing duplicate submissions while an approval request is in flight.
- Poll the backend for pending actions without overlapping requests or applying stale responses out of order.

## **Architecture Overview**
The frontend is intentionally lightweight and organized around a few clear layers:

- **UI Components** (`src/components`): Presentational building blocks such as the header, dashboard summary, and action cards.
- **Application Shell** (`src/App.tsx`): Composes the dashboard, loading state, and operator-facing error banners.
- **Custom Hook** (`src/hooks/useCorporateActions.ts`): Encapsulates polling, approval orchestration, stale request protection, and in-flight button state.
- **API Layer** (`src/api/caService.ts`): Centralizes HTTP calls to the Java backend.
- **Shared Types** (`src/types.ts`): Defines the canonical frontend contract for corporate actions and position impacts.

## **Key Features**
- **HITL Review Dashboard**: Highlights that human validation is active before trade execution.
- **Safe Approval Flow**: Disables the approval button during submission to avoid duplicate requests.
- **Resilient Polling**: Waits for the current request to finish before scheduling the next poll.
- **Inline Error Handling**: Surfaces fetch and approval failures directly in the UI instead of relying on browser alerts.
- **Contract-Backed Rendering**: Displays only values backed by the shared API contract rather than invented metadata.

## **Backend Integration**
During local development, Vite proxies frontend API calls from `/api/*` to the Java backend at `http://localhost:8080`.

Current frontend API usage:
- `GET /api/ca/pending`: Fetch pending or reviewable corporate actions for the dashboard.
- `POST /api/ca/{id}/approve`: Approve an action for execution after operator review.

The UI expects a `CorporateAction` payload with:
- action identity and security metadata
- lifecycle status
- AI analysis text
- declared ratio and key dates
- account-level `impacts` showing original and projected quantities

## **Development**

### **Prerequisites**
- Node.js 20+ recommended
- npm 10+ recommended
- Java backend running locally on `http://localhost:8080`

### **Install Dependencies**
```bash
npm install
```

### **Run the Frontend**
```bash
npm run dev
```

The application will be available at `http://localhost:3000`.

### **Build for Production**
```bash
npm run build
```

### **Preview the Production Build**
```bash
npm run preview
```

### **Testing**
```bash
npm test
```

The test suite validates:
- dashboard rendering for analyzed actions
- empty-state behavior
- approval API invocation
- duplicate-submit prevention while approval is in flight
- error handling for failed fetches
- non-overlapping polling behavior

### **Linting**
```bash
npm run lint
```

## **Project Structure**
- `src/App.tsx`: Top-level dashboard composition and error presentation.
- `src/components/Header.tsx`: HITL status banner and product header.
- `src/components/DashboardHeader.tsx`: Summary header and total action count.
- `src/components/ActionCard.tsx`: Corporate action detail and approval UI.
- `src/hooks/useCorporateActions.ts`: Polling, refresh, approval, and request safety logic.
- `src/api/caService.ts`: Axios-based API boundary.
- `src/types.ts`: Shared TypeScript interfaces for backend payloads.
- `src/test/setup.ts`: Vitest and Testing Library setup.
- `vite.config.ts`: Vite dev server, proxy, and Vitest configuration.

## **Current Scope**
- The dashboard currently focuses on review and approval of corporate actions.
- The `Reject` button is present in the UI but is not yet wired to a backend workflow.
- This frontend assumes the backend is the source of truth for lifecycle transitions and execution rules.
