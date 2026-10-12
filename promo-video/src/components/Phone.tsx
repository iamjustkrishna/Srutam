import { C, FONT } from "../theme";

type Props = {
  x: number;
  y: number;
  width: number;
  dark?: boolean;
  children?: React.ReactNode;
  style?: React.CSSProperties;
};

/** A plain Android phone (1080x2400 screen ratio) with a status bar. */
export const Phone: React.FC<Props> = ({
  x,
  y,
  width,
  dark = false,
  children,
  style,
}) => {
  const height = width * (2400 / 1080);
  const bezel = width * 0.03;
  const fg = dark ? "#E2E8F0" : C.slate900;
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        width,
        height,
        borderRadius: width * 0.11,
        background: "#0B0F1A",
        padding: bezel,
        boxSizing: "border-box",
        boxShadow: "0 40px 80px rgba(15,23,42,0.35), inset 0 0 0 3px #2A3142",
        ...style,
      }}
    >
      <div
        style={{
          position: "relative",
          width: "100%",
          height: "100%",
          borderRadius: width * 0.085,
          overflow: "hidden",
          background: dark ? C.voidBg : "#F4F5F8",
        }}
      >
        {children}
        {/* Status bar */}
        <div
          style={{
            position: "absolute",
            left: 0,
            right: 0,
            top: 0,
            height: width * 0.075,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: `0 ${width * 0.07}px`,
            fontFamily: FONT.sans,
            fontWeight: 600,
            fontSize: width * 0.035,
            color: fg,
          }}
        >
          <span>8:42</span>
          <div
            style={{
              display: "flex",
              gap: width * 0.012,
              alignItems: "center",
            }}
          >
            {[0.4, 0.6, 0.8, 1].map((h, i) => (
              <div
                key={i}
                style={{
                  width: width * 0.008,
                  height: width * 0.03 * h,
                  background: fg,
                  borderRadius: 2,
                }}
              />
            ))}
            <div
              style={{
                width: width * 0.055,
                height: width * 0.026,
                border: `2px solid ${fg}`,
                borderRadius: 5,
                marginLeft: width * 0.015,
              }}
            >
              <div style={{ width: "80%", height: "100%", background: fg }} />
            </div>
          </div>
        </div>
        {/* Punch-hole camera */}
        <div
          style={{
            position: "absolute",
            left: "50%",
            top: width * 0.025,
            width: width * 0.035,
            height: width * 0.035,
            marginLeft: -width * 0.0175,
            borderRadius: 99,
            background: "#05070C",
          }}
        />
      </div>
    </div>
  );
};

/** The thumb that taps. Same flat slate language as the Host. */
export const Thumb: React.FC<{ x: number; y: number; press?: number }> = ({
  x,
  y,
  press = 0,
}) => (
  <svg
    viewBox="0 0 260 520"
    width={260}
    height={520}
    style={{
      position: "absolute",
      left: x - 70,
      top: y - 40,
      overflow: "visible",
      transformOrigin: "70px 40px",
      scale: `${(1 - press * 0.06) * 0.72}`,
    }}
  >
    <g transform="rotate(-32 80 40)">
      <rect x={-10} y={0} width={170} height={330} rx={85} fill="#475569" />
      <rect x={-60} y={250} width={300} height={400} rx={120} fill="#475569" />
      <rect x={22} y={18} width={106} height={120} rx={52} fill="#64748B" />
      <path
        d="M 10 210 Q 75 235 140 210"
        stroke="#334155"
        strokeWidth={6}
        fill="none"
        strokeLinecap="round"
      />
    </g>
  </svg>
);

/** Expanding tap ripple. */
export const Ripple: React.FC<{
  x: number;
  y: number;
  t: number;
  color?: string;
}> = ({ x, y, t, color = C.white }) =>
  t <= 0 || t >= 1 ? null : (
    <div
      style={{
        position: "absolute",
        left: x - 90 * t,
        top: y - 90 * t,
        width: 180 * t,
        height: 180 * t,
        borderRadius: 999,
        border: `6px solid ${color}`,
        opacity: 1 - t,
      }}
    />
  );
