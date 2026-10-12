import { AbsoluteFill } from "remotion";
import { MeeraSketch } from "./MeeraSketch";

/** Expression sheet in the warm, hand-drawn reference style. */
export const SketchSheet: React.FC = () => (
  <AbsoluteFill style={{ background: "#8B7B71" }}>
    <svg viewBox="0 0 1800 2400" width={1800} height={2400}>
      <defs>
        <filter id="paper">
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.8}
            numOctaves={2}
            seed={9}
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
      </defs>
      <rect width={1800} height={2400} fill="#8B7B71" />
      <rect
        width={1800}
        height={2400}
        filter="url(#paper)"
        opacity={0.09}
        style={{ mixBlendMode: "overlay" }}
      />
      <MeeraSketch
        x={40}
        y={20}
        s={1.3}
        expr="surprised"
        turn={0.35}
        gaze={[0.7, 0]}
      />
      <MeeraSketch
        x={880}
        y={400}
        s={1.45}
        expr="smug"
        turn={0.3}
        gaze={[0.8, 0.1]}
        hand
      />
      <MeeraSketch
        x={40}
        y={1290}
        s={1.3}
        expr="worried"
        turn={-0.15}
        gaze={[-0.3, -0.4]}
      />
      <MeeraSketch x={1110} y={1650} s={0.95} expr="laugh" turn={0} />
    </svg>
  </AbsoluteFill>
);
