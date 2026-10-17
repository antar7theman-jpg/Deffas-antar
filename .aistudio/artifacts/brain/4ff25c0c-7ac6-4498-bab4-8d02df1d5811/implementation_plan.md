# Implementation Plan - Manager Sync Mode (Pro vs Offline/Lite) & Permission Error Fix

## Problem Statement
1. **Firestore Permission Error (`PERMISSION_DENIED`)**: When managers or workers attempt to sync data without proper authorization or when sync is disabled, Firestore throws permission denied exceptions.
2. **Manager Sync Mode Feature**: Users need the ability when creating or approving a manager (or independent manager) to assign a sync mode:
   - **Pro mode**: Full synchronization capabilities with workers (default).
   - **Just offline / Lite**: All app features except live synchronization with workers.
   - Ability to change this status in the future by the owner/admin.
   - When a manager in offline/lite mode tries to access sync features, show an informative upgrade prompt explaining that Pro mode is required.

---

## Proposed Changes

### 1. Data Models & Repository (`StaffAccountRepository.kt`, `Models.kt`)
- Add `syncMode` field to `StaffAccount` (values: `"pro"`, `"offline"`, `"lite"`, defaulting to `"pro"`).
- Update Firestore serialization/deserialization (`toFirestoreMap()`, `fromFirestoreMap()`, SharedPreferences caching) to include `syncMode`.
- Provide helper properties in `StaffAccount` (e.g., `isSyncEnabled: Boolean get() = syncMode == "pro"`).

### 2. Firestore Security Rules (`firestore.rules`)
- Update Firestore security rules for `/users/{userId}` and subcollections (`products`, `invoices`, `worker_locations`) to check if the staff/manager account has sync enabled (`syncMode == "pro"` or owner access). If a manager is in offline/lite mode, block live sync writes/reads on shared cloud collections, preventing `PERMISSION_DENIED` crashes and enforcing the tier on the server side.

### 3. Manager Creation & Approval UI (`StaffAccountRepository.kt`, `FirebaseStaffAuthComponents.kt`, `AntarSalesViewModel.kt`)
- Update `createWorkerAccountByManager`, registration, and approval functions to accept and persist `syncMode`.
- Update the Staff Management / Manager Approval dialogs and screens to include a dropdown or selector for `syncMode` (`Pro mode` vs `Just offline / Lite`), allowing admins/owners to set or change it at any time.

### 4. Sync Gate & Upgrade Prompt (`LiveInventorySyncManager.kt`, UI Components)
- In `LiveInventorySyncManager` and sales/sync actions, check `isSyncEnabled`.
- If sync is disabled (offline/lite mode) and the manager attempts to start sync or push data, catch or intercept and trigger an upgrade dialog / prompt ("ميزة المزامنة تتطلب حساب Pro. يمكنك ترقية الحساب من لوحة التحكم").
- Fix any unhandled Firestore permission exceptions gracefully with fallback local-only behavior and user feedback.

---

## Verification Plan
- Compile applet (`compile_applet`) to verify syntax and dependency correctness.
- Test that creating/approving managers with Pro vs Offline/Lite saves correctly.
- Test that offline/lite managers receive the upgrade prompt when sync is triggered.
