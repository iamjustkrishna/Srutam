// TEMPORARY (review stills only, removed before hand-off).
import { Composition, registerRoot } from "remotion";
import { fontsReady } from "../../fonts";
import { RENDER_K, SparkLandscape, TOTAL } from "./index";

const Root: React.FC = () => (
  <Composition
    id="SparkStory-16x9"
    component={SparkLandscape}
    durationInFrames={TOTAL * RENDER_K}
    fps={30 * RENDER_K}
    width={1920}
    height={1080}
    calculateMetadata={async () => {
      await fontsReady;
      return {};
    }}
  />
);
registerRoot(Root);
