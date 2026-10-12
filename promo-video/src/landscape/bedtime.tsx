// "The 2 a.m. Idea" in 16:9: Meera's face beside what she is thinking of (the bulb,
// the thought bubbles), then beside the phone that catches the idea; the café as
// two panels; the brand card re-laid side by side on its cream paper.
import { interpolate } from "remotion";
import { clamp } from "../anim";
import { BedtimeVideo, CAPTIONS } from "../bedtime/BedtimeVideo";
import { BEAT, SCENES, TOTAL } from "../bedtime/timeline";
import { Wide, type Shot } from "./Wide";

/** The 2:13 a.m. clock, top left as in the vertical cut. */
const Clock: React.FC<{ f: number }> = ({ f }) => (
  <svg viewBox="0 0 1920 1080" style={{ position: "absolute", inset: 0 }}>
    <text
      x={80}
      y={200}
      fontFamily="Inter"
      fontWeight={200}
      fontSize={150}
      fill="#E9E4FF"
      opacity={interpolate(f, [14, 24], [0, 1], clamp)}
      style={{ filter: "drop-shadow(0 0 16px rgba(167,139,250,0.8))" }}
    >
      2:13
    </text>
    <text x={88} y={262} fontFamily="Inter" fontWeight={600} fontSize={44} letterSpacing={8} fill="#C4B5FD" opacity={interpolate(f, [18, 28], [0, 1], clamp)}>
      A.M.
    </text>
  </svg>
);

/** The VHS rewind HUD across the top (same clock run-back as the vertical cut). */
const RewindHud: React.FC<{ f: number }> = ({ f }) => {
  const t = f - SCENES.rewind.from;
  const mins = Math.round(interpolate(t / SCENES.rewind.duration, [0, 0.9], [7 * 60 + 30, 2 * 60 + 13], clamp));
  return (
    <svg viewBox="0 0 1920 1080" style={{ position: "absolute", inset: 0 }}>
      <g
        fontFamily="Inter"
        fontWeight={800}
        fill="#FFFFFF"
        stroke="#0B0718"
        strokeWidth={8}
        paintOrder="stroke"
        style={{ filter: "drop-shadow(0 0 6px rgba(255,255,255,0.7))" }}
      >
        <text x={80} y={140} fontSize={76} opacity={t % 16 < 11 ? 1 : 0.2}>
          ◀◀ REWIND
        </text>
        <text x={1840} y={140} textAnchor="end" fontSize={76}>
          {`${Math.floor(mins / 60)}:${String(mins % 60).padStart(2, "0")}`}
        </text>
      </g>
    </svg>
  );
};

const FACE: [number, number, number] = [540, 820, 760];

const SHOTS: Shot[] = [
  // 2:13 a.m.: eyes snap open, then the bulb lights up beside her.
  { at: 0, crop: [540, 770, 1080], to: [540, 760, 1000], over: Clock },
  { at: BEAT.bulbOn - 2, split: [FACE, [540, 230, 560]], over: Clock },
  // "I'll remember it in the morning": the thought bubble beside her smug face.
  { at: SCENES.remember.from, split: [FACE, [445, 300, 700]] },
  // Morning: the 7:30 alarm, then the empty bubble.
  { at: SCENES.morning1.from, crop: [540, 780, 1080] },
  { at: BEAT.bulbPop - 2, split: [FACE, [540, 300, 700]] },
  { at: SCENES.rewind.from, crop: [540, 770, 1080], over: RewindHud },
  // The second night: her face beside the phone that is listening.
  { at: SCENES.night2.from, split: [[540, 760, 760], [540, 1500, 760]] },
  { at: SCENES.morning2.from, split: [[540, 610, 720], [540, 1340, 760]], cap: [40, 1000] },
  // Three months later: the café sign, then Meera with her chai.
  { at: SCENES.later.from, split: [[540, 500, 900], [540, 1400, 900]] },
  // The bulb and logo beside the name, tagline and badge, on the same cream paper.
  {
    at: SCENES.end.from,
    bg: "#F4EBDD",
    panels: [
      { box: [60, 90, 900, 900], win: [540, 640, 880] },
      { box: [980, 300, 880, 420], win: [540, 1300, 1000] },
    ],
  },
];

export const BedtimeWide: React.FC = () => <Wide V={BedtimeVideo} shots={SHOTS} cues={CAPTIONS} total={TOTAL} />;
