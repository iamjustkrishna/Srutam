// Placeholder for the native 16:9 cut (replaced by the landscape build).
import { AbsoluteFill } from "remotion";
import { TOTAL as T } from "../../timeline";

/** Length in timeline frames (30 fps); rendered at 30 fps. */
export const TOTAL = T;
export const RENDER_K = 1;

export const SparkLandscape: React.FC = () => <AbsoluteFill style={{ background: "#111" }} />;
