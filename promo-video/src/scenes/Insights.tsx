import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { clamp, easeInOut, easeOut, springy } from "../anim";
import {
  InsightCard,
  InsightTabs,
  KIND,
  OnDeviceBadge,
  SrutamHeader,
  type Kind,
} from "../components/AppUI";
import { BellIcon } from "../components/Icons";
import { Phone } from "../components/Phone";
import { Spark } from "../components/Spark";
import { C, FONT } from "../theme";

const PHONE = { x: 90, y: 560, w: 900 };
const SCREEN_W = PHONE.w * 0.94;
const S = SCREEN_W / 640;

// Landings are synced to the VO: "next steps", "ideas", "decisions".
const KIDS: {
  kind: Kind;
  launch: number;
  land: number;
  text: string;
  orbit: number;
}[] = [
  {
    kind: "next",
    launch: 38,
    land: 56,
    text: "Draft the launch email by Thursday",
    orbit: 0,
  },
  {
    kind: "idea",
    launch: 68,
    land: 85,
    text: "Let beta users invite one friend each",
    orbit: 2.1,
  },
  {
    kind: "decision",
    launch: 89,
    land: 106,
    text: "The launch moves to Friday",
    orbit: 4.2,
  },
];
const HERO = { x: 540, y: 420 };
const CARD = { x: 540, y: 1250 };

/** 0:15.7 - The spark splits into the app's real insight colors. */
export const Insights: React.FC = () => {
  const frame = useCurrentFrame();
  const landed = KIDS.filter((k) => frame >= k.land);
  const current = landed.length ? landed[landed.length - 1] : KIDS[0];
  const counts = { next: 0, idea: 0, decision: 0 } as Record<Kind, number>;
  landed.forEach((k) => (counts[k.kind] = 1));
  const cardIn = interpolate(frame, [current.land, current.land + 12], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  const phoneIn = interpolate(frame, [0, 18], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  const split = interpolate(frame, [14, 26], [0, 1], {
    ...clamp,
    easing: springy,
  });
  const remind = interpolate(frame, [124, 138], [0, 1], {
    ...clamp,
    easing: springy,
  });

  return (
    <AbsoluteFill style={{ background: "#F4F5F8", overflow: "hidden" }}>
      <Phone
        x={PHONE.x}
        y={interpolate(phoneIn, [0, 1], [900, PHONE.y])}
        width={PHONE.w}
      >
        <div
          style={{
            position: "absolute",
            left: 40 * S,
            right: 40 * S,
            top: 90 * S,
          }}
        >
          <SrutamHeader
            word="Insights"
            size={66 * S}
            sub={
              <div style={{ marginTop: 10 * S }}>
                <OnDeviceBadge size={24 * S} />
              </div>
            }
          />
          <div style={{ marginTop: 30 * S }}>
            <InsightTabs s={S} selected={current.kind} counts={counts} />
          </div>
          <div
            style={{
              marginTop: 30 * S,
              opacity: frame >= KIDS[0].land ? cardIn : 0,
              translate: `${interpolate(cardIn, [0, 1], [80, 0])}px 0`,
            }}
          >
            <InsightCard s={S} kind={current.kind} text={current.text} />
          </div>
        </div>
      </Phone>

      {/* Hero spark: happy bounce, then splits into three colored sparks */}
      <Spark
        size={180}
        life={1}
        talk={interpolate(frame, [0, 30], [1, 0.2], clamp)}
        lookY={0.6}
        stretch={1 + Math.sin(frame / 5) * 0.05 * (1 - split)}
        style={{
          position: "absolute",
          left: HERO.x - 90,
          top: HERO.y - 90 - Math.abs(Math.sin(frame / 7)) * 20,
        }}
      />
      {KIDS.map((k, i) => {
        if (frame >= k.land + 4) return null;
        const a = k.orbit + frame / 10;
        const ox = HERO.x + Math.cos(a) * 170 * split;
        const oy = HERO.y + Math.sin(a) * 90 * split;
        const f = interpolate(frame, [k.launch, k.land], [0, 1], {
          ...clamp,
          easing: easeInOut,
        });
        const x = ox + (CARD.x - ox) * f;
        const y = oy + (CARD.y - oy) * f - Math.sin(f * Math.PI) * 220;
        const size = 80 * (1 - 0.5 * f);
        return (
          <Spark
            key={k.kind}
            size={size}
            life={1}
            color={KIND[k.kind].color}
            seed={i + 3}
            stretch={1 + Math.sin(f * Math.PI) * 0.4}
            style={{
              position: "absolute",
              left: x - size / 2,
              top: y - size / 2,
              opacity: split,
            }}
          />
        );
      })}

      {/* Landing flash in the category color */}
      {KIDS.map((k) => {
        const t = interpolate(frame, [k.land - 2, k.land + 14], [0, 1], clamp);
        if (t <= 0 || t >= 1) return null;
        return (
          <div
            key={k.kind}
            style={{
              position: "absolute",
              left: CARD.x - 260,
              top: CARD.y - 260,
              width: 520,
              height: 520,
              borderRadius: 999,
              background: `radial-gradient(circle, ${KIND[k.kind].color}55 0%, transparent 65%)`,
              opacity: 1 - t,
              scale: `${0.5 + t}`,
            }}
          />
        );
      })}

      {/* A reminder lands too */}
      <div
        style={{
          position: "absolute",
          left: 120,
          right: 120,
          top: interpolate(remind, [0, 1], [1980, 1440]),
          padding: "26px 32px",
          borderRadius: 36,
          background: C.white,
          boxShadow: "0 24px 60px rgba(15,23,42,0.22)",
          display: "flex",
          alignItems: "center",
          gap: 24,
          fontFamily: FONT.sans,
        }}
      >
        <div
          style={{
            width: 84,
            height: 84,
            borderRadius: 24,
            background: C.cobaltContainer,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <BellIcon size={50} />
        </div>
        <div>
          <div
            style={{
              fontSize: 28,
              fontWeight: 700,
              color: C.cobalt,
              letterSpacing: 1,
            }}
          >
            REMINDER · FRI 9:00 AM
          </div>
          <div style={{ fontSize: 40, fontWeight: 700, color: C.slate900 }}>
            Send the launch email
          </div>
        </div>
      </div>
    </AbsoluteFill>
  );
};
