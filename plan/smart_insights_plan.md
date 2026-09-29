# Srutam Insights Screen: Modern Architecture & Design Plan

> **Status:** ✅ Implemented, Verified & Live on Connected Device (`192.168.31.163:39103`)  
> **Aesthetic Philosophy:** Grounded in **Dieter Rams' Functional Minimalism ("Less, but better")** & **Apple Human Interface Guidelines (Tactile Squircle Geometry, Edge Gradient Masks, Fluid Snapping)**.

---

## 1. Visual Hierarchy & Architecture

The top date control on the **Insights** screen has been restructured into a balanced, single `44dp` three-part instrument row:

```
Row (height = 44.dp, verticalAlignment = CenterVertically, padding = 16.dp horizontal)
┌───────────────────────────┬─────────────────────────────────────────────────────────┬──────────────────────┐
│  [Month & Year] (Left)    │              [Horizontal Date Scroller] (Center)        │  ['All'] (Right)     │
│                           │                                                         │                      │
│  Sep                      │  ▕░░                                               ░░▏  │  ┌───────────────┐   │
│  2026                     │  ▕░░  24    25    26   ┌───────┐   28    29    30  ░░▏  │  │      All      │   │
│                           │  ▕░░                   │  27   │                   ░░▏  │  │  (36dp height) │   │
│  (Pure Serif Typography;  │  ▕░░                   │   •   │                   ░░▏  │  └───────────────┘   │
│   Vertical Snap Scroll    │  ▕░░                   └───────┘                   ░░▏  │  (Active: Filled;    │
│   bounded by earliest)    │  ▕░░   Edge fade mask               Edge fade mask ░░▏  │   Inactive: Outline) │
└───────────────────────────┴─────────────────────────────────────────────────────────┴──────────────────────┘
```

---

## 2. Core Pillars of the Modern Implementation

### A. Dedicated Tactile 'All' Button
- **Geometry & Alignment**: Positioned on the far right of the row as a dedicated squircle pill (`height = 36dp`, `width = 48dp`, `RoundedCornerShape(12dp)`), perfectly matching the height and corner radius of the active date cube.
- **Visual Equivalence**:
  - **All Mode (Active)**: Solid filled in `CobaltBlue` (`#1E40AF`) / `CosmicGlowBlue` with bold white text. Indicates all insights across the entire workspace are shown.
  - **Date Filtered (Inactive)**: Subtly outlined ceramic surface (`Border: 1dp Color(0xFFE2E8F0)` / `CosmicVoidCardBorder`). Tapping 'All' instantly clears the date filter back to `null` and centers the scroller on today.

### B. Pure Month & Year with Bounded Vertical Snapping
- **Typographic Distinction**: Removed the awkward inline "All" text from underneath the month. Month is rendered in elegant `PlayfairDisplayFontFamily` (14.5sp, bold serif) with tabular sans year (10sp, muted slate).
- **Bounded Vertical Wheel**: Automatically inspects the earliest captured note or insight in Room DB (`earliestCapturedNoteMonth`). The snap-wheel scrolls backward from the current month strictly down to the earliest note month and halts cleanly. It cannot scroll into nonexistent past months or future months.

### C. Horizontal Date Scroller with Edge Gradient Masks
- **Fluid Layout**: Dynamically occupies all remaining width (`Modifier.weight(1f)`).
- **Edge Fades**: Features soft horizontal gradient masks (`16dp` left and right) using `Brush.horizontalGradient`, allowing day numbers to smoothly emerge and dissolve into the background rather than abruptly clipping.
- **Drag Isolation**: Uses `collectIsDraggedAsState()` so programmatic recentering or switching months never accidentally selects dates during settling.

---

## 3. Live Physical Device Screen Captures

### State 1: All Insights Mode (Default)
The dedicated **'All'** pill is highlighted in solid blue, the scroller is positioned at the current date with subtle activity indicators, and Month & Year is cleanly displayed on the left.

![Insights Screen - All Mode](./insights_all_mode.png)

---

### State 2: Date Filtered Mode
Tapping a date (e.g., September 27) centers that date inside the active blue squircle cube. The **'All'** button dims to a subtle outlined ceramic squircle pill. Tapping **'All'** instantly restores full unconstrained view.

![Insights Screen - Date 27 Selected](./insights_date_selected.png)

---

### State 3: Ideas Stream with Categorical Badges
Clean card surfaces (`20dp` rounded corners) with soft pill badges (`💡 Idea`, `🎯 Target Date`, `⚖️ Decision`), relative timestamps, and readable origin note source chips.

![Insights Screen - Ideas Stream](./insights_ideas_view.png)

---

## 4. Code & Architecture Map

| Layer | File | Primary Responsibility |
| :--- | :--- | :--- |
| **State / View Model** | [`InsightsViewModel.kt`](file:///c:/Users/krish/AndroidStudioProjects/Srutam/app/src/main/java/space/iamjustkrishna/srutam/viewmodel/InsightsViewModel.kt) | Computes `earliestMonth: YearMonth` across recordings, insights, and reminders. |
| **UI Component** | [`InsightsDateScroller.kt`](file:///c:/Users/krish/AndroidStudioProjects/Srutam/app/src/main/java/space/iamjustkrishna/srutam/ui/screens/InsightsDateScroller.kt) | Implements `VerticalMonthPicker`, `HorizontalDaysScroller` (with gradient masks), and `AllFilterButton`. |
| **Container Screen** | [`InsightsContent.kt`](file:///c:/Users/krish/AndroidStudioProjects/Srutam/app/src/main/java/space/iamjustkrishna/srutam/ui/screens/InsightsContent.kt) | Wires `state.earliestMonth` into date scroller; eliminated old search textfield. |
| **Matrix Tests** | [`InsightsUiTest.kt`](file:///c:/Users/krish/AndroidStudioProjects/Srutam/app/src/test/java/space/iamjustkrishna/srutam/matrix/InsightsUiTest.kt) | JVM unit tests asserting `insights_all_button` visibility, click behavior, and date scroller strip. |

---

## 5. Verification Matrix

- ✅ **Robolectric JVM Unit Tests**: `InsightsUiTest` passing 100% (header assertion, date strip assertion, all-button clear date).
- ✅ **Roborazzi Headless Snapshot Tests**: `PhoneInsightsMatrixTest` passing 100% across light and cosmic void dark themes.
- ✅ **Clean Compilation**: `./gradlew assembleDebug` succeeded with zero errors.
- ✅ **Physical Hardware Validation**: Installed on physical device (`192.168.31.163:39103`). Verified live tapping, smooth snapping, and state toggling.
