import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";

const INTER_WEIGHTS = [
  ["Regular", "400"],
  ["Medium", "500"],
  ["SemiBold", "600"],
  ["Bold", "700"],
  ["ExtraBold", "800"],
] as const;

export const fontsReady = Promise.all([
  ...INTER_WEIGHTS.map(([name, weight]) =>
    loadFont({
      family: "Inter",
      url: staticFile(`fonts/Inter-${name}.otf`),
      weight,
    }),
  ),
  // The app's own display face (app/src/main/res/font/playfair_display.ttf)
  loadFont({
    family: "Playfair Display",
    url: staticFile("fonts/playfair_display.ttf"),
  }),
  // Tiro Devanagari Sanskrit (SIL OFL) for the name in its own script
  loadFont({
    family: "Tiro Devanagari Sanskrit",
    url: staticFile("fonts/TiroDevanagariSanskrit-Regular.woff2"),
  }),
  // Kalam (SIL OFL), a handwritten face from the Indian Type Foundry
  loadFont({
    family: "Kalam",
    url: staticFile("fonts/Kalam-Regular.woff2"),
    weight: "400",
  }),
  loadFont({
    family: "Kalam",
    url: staticFile("fonts/Kalam-Bold.woff2"),
    weight: "700",
  }),
]);
