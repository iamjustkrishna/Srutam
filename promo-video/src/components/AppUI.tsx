// Faithful replicas of real Srutam UI, built from the app's layouts and
// strings (floating_record_button.xml, QuickRecordNotification.kt,
// InsightCards.kt, GlobalCopilotScreen.kt). For the final cut, swap these for
// screen recordings of the app with this same demo data.
import { Img, interpolate, staticFile } from "remotion";
import { clamp, easeOut } from "../anim";
import { C, FONT } from "../theme";
import {
  BulbIcon,
  CheckIcon,
  CloseIcon,
  LockIcon,
  MicIcon,
  PauseIcon,
  SparkleIcon,
  StopIcon,
  WaveIcon,
} from "./Icons";

export type DockState = "collapsed" | "idle" | "recording";

/** The floating dock, embedded on the left screen edge. `dp` = px per dp. */
export const FloatingDock: React.FC<{
  state: DockState;
  dp: number;
  seconds?: number;
  glow?: number;
  open?: number;
}> = ({ state, dp, seconds = 0, glow = 0, open = 1 }) => {
  const btn = 38 * dp;
  const circle = (bg: string, child: React.ReactNode, border = true) => (
    <div
      style={{
        width: btn,
        height: btn,
        borderRadius: 999,
        background: bg,
        border: border ? `${1.2 * dp}px solid #475569` : undefined,
        boxSizing: "border-box",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {child}
    </div>
  );

  const halo =
    glow > 0
      ? `0 0 ${40 * glow * dp}px ${12 * glow * dp}px rgba(59,130,246,${0.65 * glow})`
      : "none";

  if (state === "collapsed") {
    return (
      <div
        style={{
          width: 52 * dp,
          height: 52 * dp,
          borderRadius: `0 ${22 * dp}px ${22 * dp}px 0`,
          background: C.slate900,
          border: `${1.2 * dp}px solid rgba(255,255,255,0.3)`,
          borderLeft: "none",
          boxSizing: "border-box",
          padding: 8 * dp,
          boxShadow: halo,
        }}
      >
        <Img
          src={staticFile("img/logo.png")}
          style={{ width: "100%", height: "100%" }}
        />
      </div>
    );
  }

  const mm = Math.floor(seconds / 60);
  const ss = String(Math.floor(seconds % 60)).padStart(2, "0");
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 8 * dp,
        padding: `${6 * dp}px ${8 * dp}px`,
        borderRadius: 22 * dp,
        background: C.slate900,
        border: `${1.2 * dp}px solid #475569`,
        boxShadow:
          state === "recording"
            ? `0 0 ${30 * dp}px rgba(239,68,68,0.45)`
            : halo,
        transformOrigin: "left center",
        scale: `${open} 1`,
        opacity: Math.min(1, open * 1.5),
      }}
    >
      {state === "idle" ? (
        <>
          {circle(C.recordCircle, <MicIcon size={18 * dp} />, false)}
          {circle(
            "#1E293B",
            <Img
              src={staticFile("img/logo.png")}
              style={{ width: 26 * dp, height: 26 * dp }}
            />,
          )}
          {circle("#1E293B", <CloseIcon size={16 * dp} />)}
        </>
      ) : (
        <>
          <div
            style={{
              fontFamily: FONT.sans,
              fontWeight: 700,
              fontSize: 13 * dp * 1.25,
              color: C.crimson,
              padding: `0 ${6 * dp}px`,
              minWidth: 34 * dp,
            }}
          >
            {mm}:{ss}
          </div>
          {circle("#1E293B", <PauseIcon size={16 * dp} />)}
          {circle(C.recordCircle, <StopIcon size={16 * dp} />, false)}
          {circle("#1E293B", <CloseIcon size={16 * dp} />)}
        </>
      )}
    </div>
  );
};

/** The single recording notification (QuickRecordNotification). */
export const SavedNotification: React.FC<{
  width: number;
  finishing: boolean;
  seconds: number;
}> = ({ width, finishing, seconds }) => {
  const s = width / 640;
  return (
    <div
      style={{
        width,
        borderRadius: 34 * s,
        background: "#FFFFFF",
        boxShadow: "0 20px 50px rgba(15,23,42,0.25)",
        padding: `${26 * s}px ${30 * s}px`,
        boxSizing: "border-box",
        fontFamily: FONT.sans,
        display: "flex",
        gap: 22 * s,
        alignItems: "center",
      }}
    >
      <Img
        src={staticFile("img/logo.png")}
        style={{ width: 72 * s, height: 72 * s }}
      />
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 22 * s, color: C.slate500, fontWeight: 500 }}>
          Srutam · now
        </div>
        <div
          style={{
            fontSize: 34 * s,
            color: C.slate900,
            fontWeight: 700,
            marginTop: 4 * s,
          }}
        >
          Saved
        </div>
        <div style={{ fontSize: 26 * s, color: C.slate500, marginTop: 2 * s }}>
          {finishing
            ? "Finishing transcript..."
            : `Your note is saved (0:${String(seconds).padStart(2, "0")})`}
        </div>
      </div>
      <div
        style={{
          width: 54 * s,
          height: 54 * s,
          borderRadius: 99,
          background: C.emerald,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          opacity: finishing ? 0.25 : 1,
        }}
      >
        <CheckIcon size={32 * s} />
      </div>
    </div>
  );
};

/** "Srutam <Word>" header in the app's Playfair + cobalt italic style. */
export const SrutamHeader: React.FC<{
  word: string;
  size: number;
  dark?: boolean;
  sub?: React.ReactNode;
}> = ({ word, size, dark = false, sub }) => (
  <div>
    <div
      style={{
        fontFamily: FONT.serif,
        fontSize: size,
        color: dark ? "#F8FAFC" : C.slate900,
        lineHeight: 1.1,
      }}
    >
      Srutam{" "}
      <span
        style={{
          fontStyle: "italic",
          fontWeight: 700,
          color: dark ? C.glowBlue : C.cobalt,
        }}
      >
        {word}
      </span>
    </div>
    {sub}
  </div>
);

export const OnDeviceBadge: React.FC<{ size: number; color?: string }> = ({
  size,
  color = C.teal,
}) => (
  <div
    style={{
      display: "flex",
      alignItems: "center",
      gap: size * 0.3,
      fontFamily: FONT.sans,
      fontSize: size,
      fontWeight: 600,
      color,
    }}
  >
    <LockIcon size={size * 1.05} color={color} />
    Transcribed on this device
  </div>
);

export type Kind = "next" | "idea" | "decision";

export const KIND = {
  next: {
    label: "Next Steps",
    chip: "NEXT STEP",
    color: C.cobalt,
    bg: C.cobaltContainer,
  },
  idea: { label: "Ideas", chip: "IDEA", color: C.amber, bg: C.amberContainer },
  decision: {
    label: "Decisions",
    chip: "DECISION",
    color: C.teal,
    bg: "#CCFBF1",
  },
} as const;

/** Category tabs row from the Insights screen. */
export const InsightTabs: React.FC<{
  s: number;
  selected: Kind;
  counts: Record<Kind, number>;
}> = ({ s, selected, counts }) => (
  <div
    style={{
      display: "flex",
      gap: 6 * s,
      padding: 8 * s,
      borderRadius: 999,
      background: C.slate100,
      border: `${1.5 * s}px solid ${C.slate200}`,
      fontFamily: FONT.sans,
    }}
  >
    {(Object.keys(KIND) as Kind[]).map((k) => (
      <div
        key={k}
        style={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          gap: 8 * s,
          padding: `${14 * s}px 0`,
          borderRadius: 999,
          background: k === selected ? "#FFFFFF" : "transparent",
          boxShadow: k === selected ? "0 2px 8px rgba(15,23,42,0.08)" : "none",
          fontSize: 22 * s,
          fontWeight: k === selected ? 700 : 500,
          color: k === selected ? C.slate900 : C.slate700,
          whiteSpace: "nowrap",
        }}
      >
        <div
          style={{
            width: 11 * s,
            height: 11 * s,
            borderRadius: 99,
            background: KIND[k].color,
          }}
        />
        {KIND[k].label} · {counts[k]}
      </div>
    ))}
  </div>
);

/** An insight card with its source note row. */
export const InsightCard: React.FC<{ s: number; kind: Kind; text: string }> = ({
  s,
  kind,
  text,
}) => {
  const k = KIND[kind];
  return (
    <div
      style={{
        borderRadius: 36 * s,
        background: "#FFFFFF",
        border: `${1.5 * s}px solid ${C.slate200}`,
        padding: 34 * s,
        fontFamily: FONT.sans,
        boxShadow: "0 10px 30px rgba(15,23,42,0.06)",
      }}
    >
      <div style={{ display: "flex", gap: 12 * s }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8 * s,
            padding: `${8 * s}px ${18 * s}px`,
            borderRadius: 999,
            background: k.bg,
            color: k.color,
            fontWeight: 700,
            fontSize: 20 * s,
            letterSpacing: 1,
          }}
        >
          {kind === "idea" ? (
            <BulbIcon size={22 * s} color={k.color} />
          ) : (
            <div
              style={{
                width: 12 * s,
                height: 12 * s,
                borderRadius: 99,
                background: k.color,
              }}
            />
          )}
          {k.chip}
        </div>
        <div
          style={{
            padding: `${8 * s}px ${18 * s}px`,
            borderRadius: 999,
            background: C.slate100,
            color: C.slate500,
            fontWeight: 700,
            fontSize: 20 * s,
          }}
        >
          OPEN
        </div>
      </div>
      <div
        style={{
          fontSize: 36 * s,
          fontWeight: 600,
          color: C.slate900,
          marginTop: 22 * s,
          lineHeight: 1.3,
        }}
      >
        {text}
      </div>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 16 * s,
          marginTop: 24 * s,
          padding: 18 * s,
          borderRadius: 24 * s,
          background: C.slate50,
        }}
      >
        <div
          style={{
            width: 52 * s,
            height: 52 * s,
            borderRadius: 99,
            background: C.cobaltContainer,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <WaveIcon size={30 * s} />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 24 * s, fontWeight: 700, color: C.slate900 }}>
            Bus ride idea
          </div>
          <div style={{ fontSize: 20 * s, color: C.slate500 }}>
            Today, 8:42 am
          </div>
        </div>
        <div style={{ fontSize: 22 * s, fontWeight: 700, color: C.cobalt }}>
          Open Note ›
        </div>
      </div>
    </div>
  );
};

/** Bottom bar: Notes / Insights / AI pill plus the red record button. */
export const BottomBar: React.FC<{
  s: number;
  active: "Notes" | "Insights" | "AI";
  dark?: boolean;
}> = ({ s, active, dark = false }) => (
  <div
    style={{
      display: "flex",
      alignItems: "center",
      gap: 20 * s,
      fontFamily: FONT.sans,
    }}
  >
    <div
      style={{
        flex: 1,
        display: "flex",
        padding: 10 * s,
        borderRadius: 999,
        background: dark ? C.voidCard : "#EEF1F6",
        border: `${1.5 * s}px solid ${dark ? C.voidBorder : C.slate200}`,
      }}
    >
      {(["Notes", "Insights", "AI"] as const).map((t) => (
        <div
          key={t}
          style={{
            flex: 1,
            textAlign: "center",
            padding: `${18 * s}px 0`,
            borderRadius: 999,
            background:
              t === active ? (dark ? "#243357" : "#FFFFFF") : "transparent",
            fontSize: 24 * s,
            fontWeight: t === active ? 700 : 500,
            color: dark ? "#E2E8F0" : t === active ? C.slate900 : C.slate500,
          }}
        >
          {t === "AI" ? "✦ AI" : t}
        </div>
      ))}
    </div>
    <div
      style={{
        width: 104 * s,
        height: 104 * s,
        borderRadius: 999,
        background: C.crimson,
        boxShadow: `0 0 0 ${9 * s}px rgba(239,68,68,0.2)`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <div
        style={{
          width: 46 * s,
          height: 46 * s,
          borderRadius: 99,
          background: "#991B1B",
        }}
      />
    </div>
  </div>
);

/** Typewriter helper: how much of `text` is visible at `frame`. */
export const typed = (text: string, frame: number, start: number, cps = 1.6) =>
  text.slice(0, Math.max(0, Math.floor((frame - start) * cps)));

/** The AI tab answering across notes (GlobalCopilotScreen, dark theme). */
export const AiChat: React.FC<{
  s: number;
  frame: number;
  askAt: number;
  answerAt: number;
}> = ({ s, frame, askAt, answerAt }) => {
  const question = "What did I decide about the launch?";
  const answer =
    "You moved the launch to Friday. The email goes out at 9 AM, and beta users can each invite one friend.";
  const q = typed(question, frame, askAt, 1.8);
  const a = typed(answer, frame, answerAt, 3.2);
  const refIn = interpolate(frame, [answerAt + 30, answerAt + 42], [0, 1], {
    ...clamp,
    easing: easeOut,
  });
  return (
    <div
      style={{
        fontFamily: FONT.sans,
        display: "flex",
        flexDirection: "column",
        gap: 26 * s,
      }}
    >
      {q.length > 0 && (
        <div
          style={{
            alignSelf: "flex-end",
            maxWidth: "82%",
            padding: `${22 * s}px ${28 * s}px`,
            borderRadius: `${34 * s}px ${34 * s}px ${8 * s}px ${34 * s}px`,
            background: C.cobalt,
            color: "#FFFFFF",
            fontSize: 32 * s,
            fontWeight: 600,
          }}
        >
          {q}
        </div>
      )}
      {frame >= answerAt - 8 && (
        <div
          style={{
            padding: 30 * s,
            borderRadius: 34 * s,
            background: C.voidCard,
            border: `${1.5 * s}px solid ${C.voidBorder}`,
            color: "#E2E8F0",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 10 * s,
              fontSize: 22 * s,
              fontWeight: 700,
              color: C.glowBlue,
            }}
          >
            <SparkleIcon size={24 * s} color={C.glowBlue} /> Srutam AI
          </div>
          <div
            style={{
              fontSize: 32 * s,
              lineHeight: 1.4,
              marginTop: 14 * s,
              minHeight: 90 * s,
            }}
          >
            {a.length > 0 ? a : "···"}
          </div>
          <div style={{ opacity: refIn, marginTop: 20 * s }}>
            <div
              style={{
                fontSize: 20 * s,
                fontWeight: 700,
                color: "#94A3B8",
                letterSpacing: 1,
              }}
            >
              Referenced Notes:
            </div>
            <div
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 10 * s,
                marginTop: 10 * s,
                padding: `${10 * s}px ${20 * s}px`,
                borderRadius: 999,
                background: "#1B2540",
                fontSize: 24 * s,
                fontWeight: 600,
              }}
            >
              <WaveIcon size={26 * s} color={C.glowBlue} /> Bus ride idea ·
              Today
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
