import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut, rand } from "../anim";
import { AiChat, SrutamHeader } from "../components/AppUI";
import { VoidSky } from "../components/Backdrops";
import { WaveIcon } from "../components/Icons";
import { Phone } from "../components/Phone";
import { Spark } from "../components/Spark";
import { C, FONT } from "../theme";

type Node = { x: number; y: number; color: string; note?: boolean };

// A Mesh-like graph: note nodes with their insights around them.
const NOTES: Node[] = [
  { x: 540, y: 820, color: C.white, note: true },
  { x: 250, y: 600, color: C.white, note: true },
  { x: 830, y: 620, color: C.white, note: true },
  { x: 300, y: 1080, color: C.white, note: true },
  { x: 800, y: 1090, color: C.white, note: true },
];
const PALETTE = [C.glowBlue, C.gold, C.emerald, C.nebulaPink, C.glowPurple];
const SATS: (Node & { parent: number })[] = NOTES.flatMap((n, ni) =>
  Array.from({ length: 3 }, (_, k) => {
    const a = ni * 1.7 + k * 2.1;
    return {
      x: n.x + Math.cos(a) * 125,
      y: n.y + Math.sin(a) * 105,
      color: PALETTE[(ni + k) % PALETTE.length],
      parent: ni,
    };
  }),
);
// Cross-links between different notes: "every thought connects".
const LINKS: [number, number][] = [
  [0, 4],
  [1, 9],
  [2, 13],
  [3, 7],
  [5, 12],
  [6, 10],
  [8, 14],
  [11, 2],
];

/** 0:21.3 - Night falls (Cosmic Void). Thoughts connect; then just ask. */
export const Connect: React.FC = () => {
  const frame = useCurrentFrame();
  const reveal = interpolate(frame, [0, 22], [0, 1], {
    ...clamp,
    easing: easeInOut,
  });
  const shrink = interpolate(frame, [62, 84], [0, 1], {
    ...clamp,
    easing: easeInOut,
  });
  const phoneY = interpolate(frame, [64, 86], [1950, 700], {
    ...clamp,
    easing: easeOut,
  });
  const draw = (start: number) =>
    interpolate(frame, [start, start + 18], [0, 1], {
      ...clamp,
      easing: easeOut,
    });

  return (
    <AbsoluteFill style={{ background: "#F4F5F8", overflow: "hidden" }}>
      <VoidSky reveal={reveal} />

      <AbsoluteFill
        style={{
          scale: `${interpolate(shrink, [0, 1], [1, 0.62])}`,
          translate: `0 ${interpolate(shrink, [0, 1], [0, -420])}px`,
          opacity: interpolate(shrink, [0, 1], [1, 0.55]),
        }}
      >
        <div
          style={{
            position: "absolute",
            left: 80,
            top: 230,
            opacity: draw(12),
          }}
        >
          <SrutamHeader word="Mesh" size={110} dark />
        </div>
        <svg
          width={1080}
          height={1920}
          style={{ position: "absolute", inset: 0 }}
        >
          {SATS.map((s, i) => {
            const p = NOTES[s.parent];
            const t = draw(10 + i * 2);
            return (
              <line
                key={`s${i}`}
                x1={p.x}
                y1={p.y}
                x2={p.x + (s.x - p.x) * t}
                y2={p.y + (s.y - p.y) * t}
                stroke="#3B4A7A"
                strokeWidth={4}
              />
            );
          })}
          {LINKS.map(([a, b], i) => {
            const t = draw(30 + i * 3);
            const A = SATS[a];
            const B = SATS[b];
            return (
              <line
                key={`l${i}`}
                x1={A.x}
                y1={A.y}
                x2={A.x + (B.x - A.x) * t}
                y2={A.y + (B.y - A.y) * t}
                stroke={C.glowPurple}
                strokeWidth={5}
                strokeOpacity={0.85}
                style={{ filter: `drop-shadow(0 0 8px ${C.glowPurple})` }}
              />
            );
          })}
        </svg>
        {NOTES.map((n, i) =>
          i === 0 ? null : (
            <div
              key={`n${i}`}
              style={{
                position: "absolute",
                left: n.x - 46,
                top: n.y - 46,
                width: 92,
                height: 92,
                borderRadius: 999,
                background: C.voidCard,
                border: `6px solid #94A3B8`,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                scale: `${draw(6 + i * 3)}`,
              }}
            >
              <WaveIcon size={50} color="#E2E8F0" />
            </div>
          ),
        )}
        {SATS.map((s, i) => (
          <Spark
            key={`p${i}`}
            size={44}
            life={1}
            color={s.color}
            faceless
            style={{
              position: "absolute",
              left: s.x - 22,
              top: s.y - 22,
              scale: `${draw(10 + i * 2) * (0.9 + 0.15 * Math.sin(frame / 6 + rand(i) * 6))}`,
            }}
          />
        ))}
        {/* Our spark is the bus-ride note at the center of it all */}
        <Spark
          size={150}
          life={1}
          lookY={interpolate(frame, [0, 40], [-0.5, 0.2], clamp)}
          lookX={Math.sin(frame / 20) * 0.6}
          style={{ position: "absolute", left: 540 - 75, top: 820 - 75 }}
        />
      </AbsoluteFill>

      {/* Just ask: the AI tab */}
      <Phone x={140} y={phoneY} width={800} dark>
        <div style={{ position: "absolute", left: 44, right: 44, top: 100 }}>
          <SrutamHeader
            word="AI"
            size={84}
            dark
            sub={
              <div
                style={{
                  fontFamily: FONT.sans,
                  fontSize: 30,
                  color: "#94A3B8",
                  marginTop: 6,
                }}
              >
                Ask across all your voice notes
              </div>
            }
          />
          <div style={{ marginTop: 44 }}>
            <AiChat s={1.05} frame={frame} askAt={76} answerAt={104} />
          </div>
        </div>
      </Phone>
    </AbsoluteFill>
  );
};
