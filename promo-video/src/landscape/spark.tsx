// "Spark" in 16:9: the countdown title set beside the dying idea, the lost-ideas
// board re-laid as a big counter with the three vignettes stacked beside it, then
// close framing on the phone, the insights, the mesh and the brand.
import { interpolate } from "remotion";
import { clamp, easeOut } from "../anim";
import { SPARK_CAPTIONS, SparkStory } from "../SparkStory";
import { C, FONT } from "../theme";
import { TOTAL } from "../timeline";
import { Panel, Wide, WIDE_H, WIDE_W, type Shot } from "./Wide";

/** The hook title, set on the left in landscape (same beats as the vertical one). */
const HookTitle: React.FC<{ f: number; V: React.FC }> = ({ f, V }) => {
  const push = interpolate(f, [0, 110], [0, 1], clamp);
  return (
    <>
      <Panel V={V} win={[450 + push * 20, 900, 980 - push * 70]} left={0} top={0} width={WIDE_W} height={WIDE_H} />
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: `linear-gradient(90deg, ${C.slate200} 0%, ${C.slate200}EE 30%, ${C.slate200}00 52%)`,
        }}
      />
      <div style={{ position: "absolute", left: 110, top: 300, fontFamily: FONT.sans }}>
        <div
          style={{
            fontSize: 84,
            fontWeight: 700,
            color: C.slate900,
            opacity: interpolate(f, [4, 14], [0, 1], clamp),
            translate: `0 ${interpolate(f, [4, 16], [24, 0], { ...clamp, easing: easeOut })}px`,
          }}
        >
          This idea has
        </div>
        <div
          style={{
            fontSize: 128,
            fontWeight: 800,
            lineHeight: 1.08,
            color: C.slate900,
            transformOrigin: "0 50%",
            opacity: interpolate(f, [44, 52], [0, 1], clamp),
            scale: `${interpolate(f, [44, 56], [1.25, 1], { ...clamp, easing: easeOut })}`,
          }}
        >
          <span style={{ background: C.slate900, padding: "0 20px", borderRadius: 20, color: C.gold }}>4 seconds</span>
          <br />
          to live.
        </div>
      </div>
    </>
  );
};

const SHOTS: Shot[] = [
  { at: 0, card: HookTitle },
  // Big counter on the left, the three vignettes (each panel 1000 x 440) stacked on the right.
  {
    at: 110,
    bg: C.slate200,
    cap: [60, 900],
    panels: [
      { box: [60, 290, 960, 420], win: [540, 185, 480] },
      { box: [1100, 40, 760, 1000], win: [540, 1050, 1040] },
    ],
  },
  // The phone, the dock tab, then the tap and the live transcript.
  { at: 200, crop: [540, 880, 1080], to: [540, 920, 1040] },
  { at: 240, blend: 10, crop: [540, 1080, 1080], to: [520, 1100, 1000] },
  { at: 300, blend: 14, crop: [540, 370, 1080] },
  // Insights fill the list: start on the spark, follow the phone up, ride down the cards.
  { at: 470, crop: [540, 600, 1080] },
  { at: 530, blend: 16, crop: [540, 1000, 1080], to: [540, 1170, 1080] },
  // The mesh builds, then the question and its answer.
  { at: 640, crop: [540, 580, 1080], to: [540, 800, 1080] },
  { at: 710, blend: 12, crop: [540, 1230, 1080] },
  // Brand: stacked and centred, the light background carried past the edges.
  { at: 790, bg: C.slate50, crop: [540, 1000, 1600] },
];

export const SparkWide: React.FC = () => <Wide V={SparkStory} shots={SHOTS} cues={SPARK_CAPTIONS} total={TOTAL} />;
