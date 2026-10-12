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
]);
