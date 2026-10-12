import { Composition, Folder } from "remotion";
import { fontsReady } from "./fonts";
import { Catch } from "./scenes/Catch";
import { Connect } from "./scenes/Connect";
import { End } from "./scenes/End";
import { Hook } from "./scenes/Hook";
import { Insights } from "./scenes/Insights";
import { Lost } from "./scenes/Lost";
import { SparkStory } from "./SparkStory";
import { FPS, HEIGHT, SCENES, TOTAL, WIDTH } from "./timeline";

const calculateMetadata = async () => {
  await fontsReady;
  return {};
};

const scenes = [
  ["Hook", Hook, SCENES.hook.duration],
  ["Lost", Lost, SCENES.lost.duration],
  ["Catch", Catch, SCENES.catch.duration],
  ["Insights", Insights, SCENES.insights.duration],
  ["Connect", Connect, SCENES.connect.duration],
  ["End", End, SCENES.end.duration],
] as const;

export const RemotionRoot: React.FC = () => (
  <>
    <Composition
      id="SparkStory30"
      component={SparkStory}
      durationInFrames={TOTAL}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Folder name="Scenes">
      {scenes.map(([id, component, duration]) => (
        <Composition
          key={id}
          id={id}
          component={component}
          durationInFrames={duration}
          fps={FPS}
          width={WIDTH}
          height={HEIGHT}
          calculateMetadata={calculateMetadata}
        />
      ))}
    </Folder>
  </>
);
