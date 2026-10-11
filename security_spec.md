# Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **Authentication Gate**: Unauthenticated callers (`request.auth == null`) cannot read or write any document in `/staff_accounts/{accountId}` or `/admins/{adminId}`.
2. **Strict Schema & Key Whitelisting**: `StaffAccount` must contain exact `hasAll` and `hasOnly` keys (22 keys) with bounded string lengths (`fullName` 1..100, `email` 3..150, `phone` 0..30) and boolean permission flags.
3. **PII & Cross-User Isolation**: A regular worker (`!isAdmin()`) can only `get` or `list` their own `/staff_accounts/{uid}` document (`request.auth.uid == accountId` or `resource.data.userId == request.auth.uid`). Only verified store admins (`isAdmin()`) can list all store staff accounts (`storeId == 'af_store'`).
4. **No Self-Approval / Privilege Escalation**:
   - On `create`: A regular user can only create their own `/staff_accounts/{uid}` with `approvalStatus == 'PENDING'`, `role == 'worker'`, `approvedByUid == ''`, and `approvedByName == ''`. Only `isAdmin()` can create an already-approved account.
   - On `update`: A regular user can only update `['fullName', 'phone', 'requestedRole', 'updatedAt']` on their own document while `userId`, `storeId`, `email`, `createdAt`, `role`, `approvalStatus`, and all permission booleans remain unchanged.
5. **Verified Admin Approval**: Only `isAdmin()` (either verified store owner `request.auth.token.email_verified == true && request.auth.token.email == 'antar7theman@gmail.com'` or an existing `/admins/$(request.auth.uid)` document) can update `approvalStatus`, `role`, and permission flags, or manage `/admins/{adminId}`.

## 2. Attack Payloads (The Dirty Dozen)
1. **The Impersonator**: User `workerA` creating `/staff_accounts/workerB` with `userId: 'workerB'` -> Denied.
2. **Privilege Escalation on Create**: User `workerA` creating `/staff_accounts/workerA` with `approvalStatus: 'APPROVED'` or `role: 'manager'` -> Denied.
3. **Privilege Escalation on Update**: User `workerA` updating `/staff_accounts/workerA` to set `approvalStatus: 'APPROVED'` or `canViewReports: true` -> Denied.
4. **PII Harvester**: User `workerA` reading `/staff_accounts/workerB` or listing all `/staff_accounts` without admin rights -> Denied.
5. **Shadow Field Injection**: User `workerA` creating `/staff_accounts/workerA` with an extra field `isSuperAdmin: true` -> Denied (`hasOnly` check).
6. **Email Spoofing**: Unverified user (`email_verified == false`) with `email == 'antar7theman@gmail.com'` attempting admin actions -> Denied.
