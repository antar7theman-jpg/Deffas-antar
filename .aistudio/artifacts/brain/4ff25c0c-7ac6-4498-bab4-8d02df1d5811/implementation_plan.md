# Implementation Plan - Comprehensive App Performance & Frame Rate Optimization

Goal: Optimize frame rates, UI responsiveness, scrolling smoothness, and background synchronization across the entire application (POS, inventory, live dashboard, reports, debts, offline sync).

## User Review Required
> [!IMPORTANT]
> This plan covers a comprehensive performance overhaul across all screens, including LazyColumn list stabilization, Coil memory caching, recomposition reduction using `derivedStateOf` and `remember`, and Firestore offline query optimization.

## Proposed Changes

### 1. Jetpack Compose UI & List Optimization
- **LazyColumn / LazyRow Stabilization**:
  - Add explicit `key` parameters to all list items across POS, Inventory, Debts, Customers, and Live Dashboard.
  - Ensure items use stable data classes or stable IDs so Compose skips unnecessary recompositions.
- **Recomposition Reduction (`derivedStateOf` & `remember`)**:
  - Wrap complex filtering, search queries, and scroll-position computations in `derivedStateOf` and `remember`.
  - Extract heavy calculations out of composable scopes into ViewModels or memoized states.
- **Modifier Optimization**:
  - Avoid lambda allocations inside loops and frequently changing modifiers where possible.

### 2. Image Loading & Memory Performance (Coil)
- Configure optimal memory cache size and disk cache policies for Coil image loaders (product images, avatars).
- Enable crossfade and bitmap config optimizations to prevent UI jank during rapid list scrolling.

### 3. Database & Offline Sync Performance (Firestore & Local Caching)
- Optimize Firestore offline persistence query batching and snapshot listeners to avoid redundant network reads and main-thread blocking.
- Streamline local state updates so offline sales, inventory changes, and manager/worker notifications process instantly without frame drops.

## Verification Plan
- Run `compile_applet` to verify compilation and syntax correctness.
- Test app navigation and scrolling responsiveness.
