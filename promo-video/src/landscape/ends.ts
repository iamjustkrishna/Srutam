// The story videos close on Kit's EndBand: a dark band (y 1560..1920) with the
// wordmark and tagline. In 16:9 the scene keeps the left and the band's lettering
// sits on the right, on the band's own colour.
import type { Shot, Win } from "./Wide";

export const BAND = "#050714";

/** Scene on the left (window `scene`, 1000 px wide), the band's lettering on the right. */
export const bandEnd = (at: number, scene: Win, bandW = 920): Shot => ({
  at,
  bg: BAND,
  panels: [
    { box: [0, 0, 1000, 1080], win: scene },
    { box: [1030, 381, 860, 318], win: [540, 1740, bandW] },
  ],
});
