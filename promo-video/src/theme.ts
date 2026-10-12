// Colors come straight from the app's tokens (app/.../ui/theme/Color.kt,
// SemanticColors.kt and InsightCards.kt) so the video teaches the real UI.
export const C = {
  // Act 1: the grey world the idea dies in
  slate900: "#0F172A",
  slate700: "#334155",
  slate500: "#64748B",
  slate400: "#94A3B8",
  slate300: "#CBD5E1",
  slate200: "#E2E8F0",
  slate100: "#F1F5F9",
  slate50: "#F8FAFC",
  white: "#FFFFFF",

  // The spark (StardustGold) - the only warm thing on screen early on
  gold: "#FBBF24",
  goldLight: "#FDE68A",
  goldDeep: "#F59E0B",

  // Capture only (StudioCrimson / floating record circle)
  crimson: "#EF4444",
  recordCircle: "#E11D48",

  // Insight categories
  cobalt: "#2563EB",
  cobaltContainer: "#EFF6FF",
  amber: "#D97706",
  amberContainer: "#FEF3C7",
  teal: "#0D9488",
  emerald: "#10B981",
  emeraldContainer: "#ECFDF5",

  // Cosmic Void (payoff)
  voidBg: "#050714",
  voidCard: "#0C1225",
  voidBorder: "#1A1F3A",
  glowPurple: "#7C3AED",
  glowBlue: "#3B82F6",
  nebulaPink: "#EC4899",
} as const;

export const FONT = {
  sans: "Inter, sans-serif",
  serif: "Playfair Display, serif",
} as const;
