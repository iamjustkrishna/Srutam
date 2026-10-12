import { Composition, Folder, Still } from "remotion";
import { fontsReady } from "./fonts";
import { Catch } from "./scenes/Catch";
import { Connect } from "./scenes/Connect";
import { End } from "./scenes/End";
import { Hook } from "./scenes/Hook";
import { Insights } from "./scenes/Insights";
import { Lost } from "./scenes/Lost";
import { SparkStory } from "./SparkStory";
import { StyleBoard } from "./styleboard/StyleBoard";
import { SpiritBoard } from "./spirit/SpiritBoard";
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
    <Still
      id="AnimeStyleBoard"
      component={StyleBoard}
      width={2200}
      height={2660}
      calculateMetadata={calculateMetadata}
    />
    <Still
      id="ShruBoard"
      component={SpiritBoard}
      width={2480}
      height={2800}
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
