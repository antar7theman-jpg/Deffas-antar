# Hide Google Sign-In Button

Remove or hide the Google Sign-In button from the staff/owner authentication screen as requested, preserving all other app features and behavior.

## User Review & Critical Decisions

> [!IMPORTANT]
> The Google Sign-In button on the login screen will be removed cleanly.

- **Confirmed Decision**: Remove the Google Sign-In button block entirely from `FirebaseStaffAuthComponents.kt`.

## 1. Overview & Core Concept

- **What It Does**: Hides/removes the Google sign-in option from the login screen.
- **Key Value**: Streamlines the login interface to match user preferences without touching other functionalities.

## 2. User Experience & Visual Design

- **Login Screen**: The green "Sign in with Google" button is removed, leaving standard email/password login and direct login options intact with consistent spacing.

## 3. Key Product Decisions & Trade-Offs

- **Decision**: Completely remove the button and associated callback invocation from the UI layout.
- **Why**: Directly satisfies the user request while keeping credential manager code intact if needed, or safely cleaning up the specific view block.

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────┐
│      FirebaseStaffSignInScreen           │
│  - Direct login / Email login            │
│  - [Google Sign-In Button REMOVED]       │
│  - Register new account link             │
└──────────────────────────────────────────┘
```
