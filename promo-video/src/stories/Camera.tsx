// Camera and light for the story videos: a camera group (zoom about a focus,
// handheld drift, shake), a depth-of-field background layer, a colour grade
// and a light leak for transitions. All SVG, inside a 1080 x 1920 Frame.
import { useId } from "react";
import { handheld } from "./physics";
import { useCanvas } from "./canvas";

export const Camera: React.FC<{
  f: number;
  zoom?: number;
  focus?: [number, number];
  /** Where the focus point lands on screen (defaults to where it is: zoom in place). */
  center?: [number, number];
  shake?: [number, number];
  drift?: number;
  seed?: number;
  children: React.ReactNode;
}> = ({ f, zoom = 1, focus, center, shake = [0, 0], drift = 5, seed = 1, children }) => {
  const cv = useCanvas();
  const fo = focus ?? [cv.w / 2, cv.h / 2];
  const h = handheld(f, drift, seed);
  const c = center ?? fo;
  return (
    <g transform={`translate(${h.x + shake[0]} ${h.y + shake[1]}) rotate(${h.rot} ${cv.w / 2} ${cv.h / 2}) translate(${c[0]} ${c[1]}) scale(${zoom}) translate(${-fo[0]} ${-fo[1]})`}>
      {children}
    </g>
  );
};

/** A background layer: blurred (depth of field) and moved less than the camera (parallax). */
export const Depth: React.FC<{ blur?: number; children: React.ReactNode }> = ({ blur = 3, children }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g>
      <defs>
        <filter id={`dof${id}`} x="-5%" y="-5%" width="110%" height="110%">
          <feGaussianBlur stdDeviation={blur} />
        </filter>
      </defs>
      <g filter={`url(#dof${id})`}>{children}</g>
    </g>
  );
};

/** Colour grade: a warm key from one side, cool shadows on the other. */
export const Grade: React.FC<{ warm?: string; cool?: string; from?: "left" | "right" | "top"; amount?: number }> = ({
  warm = "#FDBA74",
  cool = "#312E81",
  from = "left",
  amount = 0.22,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const cv = useCanvas();
  const [x1, y1, x2, y2] = from === "left" ? [0, 0, 1, 0] : from === "right" ? [1, 0, 0, 0] : [0, 0, 0, 1];
  return (
    <g style={{ mixBlendMode: "soft-light" }} opacity={amount * 3}>
      <defs>
        <linearGradient id={`gr${id}`} x1={x1} y1={y1} x2={x2} y2={y2}>
          <stop offset="0%" stopColor={warm} />
          <stop offset="100%" stopColor={cool} />
        </linearGradient>
      </defs>
      <rect width={cv.w} height={cv.h} fill={`url(#gr${id})`} />
    </g>
  );
};

/** A warm light leak sweeping across, for transitions (k: 0 -> 1). */
export const LightLeak: React.FC<{ k: number; color?: string }> = ({ k, color = "#FB923C" }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const cv = useCanvas();
  if (k <= 0 || k >= 1) return null;
  const a = Math.sin(k * Math.PI);
  return (
    <g style={{ mixBlendMode: "screen" }} opacity={a * 0.85}>
      <defs>
        <radialGradient id={`lk${id}`}>
          <stop offset="0%" stopColor="#FFF7ED" />
          <stop offset="35%" stopColor={color} />
          <stop offset="100%" stopColor={color} stopOpacity={0} />
        </radialGradient>
      </defs>
      <ellipse cx={-200 + k * (cv.w + 420)} cy={(500 + k * 600) * (cv.h / 1920)} rx={900} ry={700} fill={`url(#lk${id})`} />
    </g>
  );
};
