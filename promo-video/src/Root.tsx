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
import { OptionsBoard } from "./options/OptionsBoard";
import { PaintedBoard } from "./painted/PaintedBoard";
import { SketchSheet } from "./sketch/SketchSheet";
import { BedtimeVideo } from "./bedtime/BedtimeVideo";
import { TOTAL as BEDTIME_TOTAL } from "./bedtime/timeline";
import { PANELS } from "./painted/Panels";
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
    <Still
      id="HeroOptions"
      component={OptionsBoard}
      width={2500}
      height={2560}
      calculateMetadata={calculateMetadata}
    />
    <Still
      id="PaintedStoryboard"
      component={PaintedBoard}
      width={3376}
      height={1590}
      calculateMetadata={calculateMetadata}
    />
    <Folder name="Painted-Panels">
      {PANELS.map((Panel, i) => (
        <Still
          key={i}
          id={`Painted${String(i + 1).padStart(2, "0")}`}
          component={() => (
            <svg viewBox="0 0 1600 900" width={1600} height={900}>
              <Panel />
            </svg>
          )}
          width={1600}
          height={900}
          calculateMetadata={calculateMetadata}
        />
      ))}
    </Folder>
    <Still
      id="MeeraSketchSheet"
      component={SketchSheet}
      width={1800}
      height={2400}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="TwoAmIdea"
      component={BedtimeVideo}
      durationInFrames={BEDTIME_TOTAL}
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
