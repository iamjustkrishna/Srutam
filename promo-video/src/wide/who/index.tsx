// Placeholder for the native 16:9 cut (replaced by the landscape build).
import { AbsoluteFill } from "remotion";
import { TOTAL as T } from "../../who/timeline";

/** Length in timeline frames (30 fps); rendered at 60 fps. */
export const TOTAL = T;
export const RENDER_K = 2;

export const WhoLandscape: React.FC = () => <AbsoluteFill style={{ background: "#111" }} />;
