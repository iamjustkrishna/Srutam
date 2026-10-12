// Real Srutam UI for "The 2 a.m. Idea", as HTML inside the phone.
// Strings come from QuickRecordNotification.kt and the Insights screen.
import { Img } from "remotion";
import { BellIcon, BulbIcon, LockIcon, WaveIcon } from "../components/Icons";
import { LOGO } from "../spirit/logoData";
import { C } from "../theme";

export type LockState = "idle" | "recording" | "saving" | "saved";

const Pill: React.FC<{
  label: string;
  primary?: boolean;
  pressed?: boolean;
  k: number;
}> = ({ label, primary, pressed, k }) => (
  <div
    style={{
      padding: `${12 * k}px ${30 * k}px`,
      borderRadius: 999,
      background: primary ? "#2563EB" : "#64748B",
      color: "#FFFFFF",
      fontWeight: 700,
      fontSize: 24 * k,
      transform: pressed ? "scale(0.92)" : undefined,
      boxShadow: pressed ? "0 0 0 8px rgba(37,99,235,0.35)" : undefined,
    }}
  >
    {label}
  </div>
);

/** The lock screen at 2:13 a.m. with Srutam's single recording notification. */
export const LockScreen: React.FC<{
  w: number;
  state: LockState;
  seconds: number;
  level: number;
  pressed?: "start" | "save" | null;
}> = ({ w, state, seconds, level, pressed = null }) => {
  const k = w / 640;
  const title =
    state === "idle" ? "Srutam" : state === "recording" ? "Recording" : "Saved";
  const text =
    state === "idle"
      ? "Ready to record"
      : state === "recording"
        ? `0:${String(Math.floor(seconds)).padStart(2, "0")}`
        : state === "saving"
          ? "Finishing transcript..."
          : "Your note is saved (0:08)";
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "linear-gradient(180deg,#1A2050 0%,#0B0F26 100%)",
        color: "#F8FAFC",
        padding: `${120 * k}px ${34 * k}px 0`,
      }}
    >
      <div
        style={{
          textAlign: "center",
          fontSize: 150 * k,
          fontWeight: 300,
          lineHeight: 1,
        }}
      >
        2:13
      </div>
      <div
        style={{
          textAlign: "center",
          fontSize: 26 * k,
          opacity: 0.75,
          marginTop: 10 * k,
        }}
      >
        Wednesday, 15 October
      </div>
      <div
        style={{
          marginTop: 60 * k,
          borderRadius: 34 * k,
          background: "rgba(255,255,255,0.95)",
          color: "#0F172A",
          padding: `${24 * k}px ${26 * k}px`,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10 * k,
            fontSize: 20 * k,
            color: "#64748B",
            fontWeight: 600,
          }}
        >
          <Img src={LOGO} style={{ width: 30 * k, height: 30 * k }} />
          Srutam · now
        </div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginTop: 12 * k,
          }}
        >
          <div>
            <div
              style={{
                fontSize: 32 * k,
                fontWeight: 800,
                display: "flex",
                alignItems: "center",
                gap: 10 * k,
              }}
            >
              {state === "recording" && (
                <span
                  style={{
                    width: 16 * k,
                    height: 16 * k,
                    borderRadius: 99,
                    background: C.crimson,
                    display: "inline-block",
                  }}
                />
              )}
              {title}
            </div>
            <div
              style={{ fontSize: 24 * k, color: "#475569", marginTop: 4 * k }}
            >
              {text}
            </div>
          </div>
          <div style={{ display: "flex", gap: 10 * k }}>
            {state === "idle" && (
              <Pill k={k} label="Start" primary pressed={pressed === "start"} />
            )}
            {state === "recording" && (
              <>
                <Pill k={k} label="Pause" primary />
                <Pill k={k} label="Save" pressed={pressed === "save"} />
              </>
            )}
            {(state === "saving" || state === "saved") && (
              <div
                style={{
                  width: 52 * k,
                  height: 52 * k,
                  borderRadius: 99,
                  background: state === "saved" ? C.emerald : "#CBD5E1",
                  color: "#FFF",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 30 * k,
                  fontWeight: 900,
                }}
              >
                ✓
              </div>
            )}
          </div>
        </div>
        {state === "recording" && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 6 * k,
              height: 70 * k,
              marginTop: 16 * k,
            }}
          >
            {Array.from({ length: 30 }, (_, i) => {
              const h =
                8 + level * 56 * Math.abs(Math.sin(i * 1.7 + seconds * 9));
              return (
                <div
                  key={i}
                  style={{
                    width: 8 * k,
                    height: h * k,
                    borderRadius: 4 * k,
                    background: i < 22 ? "#2563EB" : "#CBD5E1",
                  }}
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

const Chip: React.FC<{
  label: string;
  color: string;
  bg: string;
  k: number;
  icon?: React.ReactNode;
}> = ({ label, color, bg, k, icon }) => (
  <div
    style={{
      display: "inline-flex",
      alignItems: "center",
      gap: 8 * k,
      padding: `${7 * k}px ${16 * k}px`,
      borderRadius: 999,
      background: bg,
      color,
      fontWeight: 800,
      fontSize: 18 * k,
      letterSpacing: 1,
    }}
  >
    {icon}
    {label}
  </div>
);

const Card: React.FC<{
  k: number;
  show: number;
  children: React.ReactNode;
}> = ({ k, show, children }) => (
  <div
    style={{
      marginTop: 18 * k,
      padding: `${22 * k}px ${24 * k}px`,
      borderRadius: 28 * k,
      background: "#FFFFFF",
      border: "1.5px solid #E2E8F0",
      boxShadow: "0 8px 24px rgba(15,23,42,0.06)",
      opacity: show,
      transform: `translateY(${(1 - show) * 40 * k}px) scale(${0.96 + 0.04 * show})`,
    }}
  >
    {children}
  </div>
);

/** Morning: the note, transcribed and titled, and what Srutam pulled out of it. */
export const MorningScreen: React.FC<{
  w: number;
  note: number;
  idea: number;
  step: number;
  reminder: number;
}> = ({ w, note, idea, step, reminder }) => {
  const k = w / 640;
  const source = (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 8 * k,
        marginTop: 12 * k,
        fontSize: 18 * k,
        color: "#64748B",
      }}
    >
      <WaveIcon size={22 * k} /> Rooftop Book Café · 2:13 am
    </div>
  );
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "#F4F5F8",
        padding: `${70 * k}px ${30 * k}px 0`,
        color: "#0F172A",
      }}
    >
      <div style={{ fontFamily: "Playfair Display", fontSize: 54 * k }}>
        Srutam{" "}
        <span style={{ fontStyle: "italic", fontWeight: 700, color: C.cobalt }}>
          Insights
        </span>
      </div>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8 * k,
          color: C.teal,
          fontWeight: 600,
          fontSize: 20 * k,
          marginTop: 6 * k,
        }}
      >
        <LockIcon size={20 * k} color={C.teal} /> Transcribed on this device
      </div>
      <Card k={k} show={note}>
        <div style={{ fontSize: 26 * k, fontWeight: 800 }}>
          Rooftop Book Café
        </div>
        <div style={{ fontSize: 18 * k, color: "#64748B" }}>
          Today, 2:13 am · 0:08
        </div>
        <div
          style={{
            fontSize: 22 * k,
            lineHeight: 1.45,
            marginTop: 10 * k,
            color: "#334155",
          }}
        >
          Rooftop café, for book lovers... swap a book, get a chai. Call Priya
          about her terrace... Saturday.
        </div>
      </Card>
      <Card k={k} show={idea}>
        <Chip
          k={k}
          label="IDEA"
          color={C.amber}
          bg={C.amberContainer}
          icon={<BulbIcon size={18 * k} color={C.amber} />}
        />
        <div
          style={{
            fontSize: 28 * k,
            fontWeight: 700,
            marginTop: 12 * k,
            lineHeight: 1.3,
          }}
        >
          A rooftop café for book lovers: swap a book, get a chai
        </div>
        {source}
      </Card>
      <Card k={k} show={step}>
        <Chip k={k} label="NEXT STEP" color={C.cobalt} bg={C.cobaltContainer} />
        <div style={{ fontSize: 28 * k, fontWeight: 700, marginTop: 12 * k }}>
          Call Priya about her terrace
        </div>
        {source}
      </Card>
      <Card k={k} show={reminder}>
        <div style={{ display: "flex", alignItems: "center", gap: 16 * k }}>
          <div
            style={{
              width: 56 * k,
              height: 56 * k,
              borderRadius: 16 * k,
              background: C.cobaltContainer,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <BellIcon size={34 * k} />
          </div>
          <div>
            <div
              style={{
                fontSize: 18 * k,
                fontWeight: 800,
                color: C.cobalt,
                letterSpacing: 1,
              }}
            >
              REMINDER · SATURDAY
            </div>
            <div style={{ fontSize: 26 * k, fontWeight: 700 }}>
              Call Priya about her terrace
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
