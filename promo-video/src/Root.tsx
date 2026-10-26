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
import { CastSheet } from "./cast/CastSheet";
import { BedtimeVideo } from "./bedtime/BedtimeVideo";
import { KnockVideo } from "./knock/KnockVideo";
import { TOTAL as KNOCK_TOTAL } from "./knock/timeline";
import { NaniVideo } from "./nani/NaniVideo";
import { WhoVideo } from "./who/WhoVideo";
import { K as STORY_K, RENDER_FPS } from "./stories/time";
import { ExamVideo } from "./exam/ExamVideo";
import { TOTAL as EXAM_TOTAL } from "./exam/timeline";
import { SlidesVideo } from "./slides/SlidesVideo";
import { DrawnSlides, FULL as DRAWN_TOTAL } from "./drawn/DrawnSlides";
import { BrainCourt, TOTAL as COURT_TOTAL } from "./court/Court";
import { TestFrame } from "./drawn/TestFrame";
import { TOTAL as SLIDES_TOTAL } from "./slides/timeline";
import { TOTAL as WHO_TOTAL } from "./who/timeline";
import { MeetingTest, TEST_TOTAL } from "./meeting/MeetingTest";
import { TOTAL as NANI_TOTAL } from "./nani/timeline";
import { TOTAL as BEDTIME_TOTAL } from "./bedtime/timeline";
import { PANELS } from "./painted/Panels";
import { SpiritBoard } from "./spirit/SpiritBoard";
import { SHEET_H, SHEET_W } from "./storyboards/Board";
import { IDEA_BOARDS, PRO_BOARDS } from "./storyboards/Ideas";
import { PRO_H, PRO_W } from "./storyboards/ProBoard";
import { FPS, HEIGHT, SCENES, TOTAL, WIDTH } from "./timeline";
import { SparkLandscape, TOTAL as SPARK_L, RENDER_K as SPARK_LK } from "./wide/spark";
import { BedtimeLandscape, TOTAL as BEDTIME_L, RENDER_K as BEDTIME_LK } from "./wide/bedtime";
import { NaniLandscape, TOTAL as NANI_L, RENDER_K as NANI_LK } from "./wide/nani";
import { KnockLandscape, TOTAL as KNOCK_L, RENDER_K as KNOCK_LK } from "./wide/knock";
import { WhoLandscape, TOTAL as WHO_L, RENDER_K as WHO_LK } from "./wide/who";
import { ExamLandscape, TOTAL as EXAM_L, RENDER_K as EXAM_LK } from "./wide/exam";
import { LH, LW } from "./wide/kit";

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
    <Folder name="Landscape-16x9">
      <Composition id="SparkStory-16x9" component={SparkLandscape} durationInFrames={SPARK_L * SPARK_LK} fps={30 * SPARK_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
      <Composition id="TwoAmIdea-16x9" component={BedtimeLandscape} durationInFrames={BEDTIME_L * BEDTIME_LK} fps={30 * BEDTIME_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
      <Composition id="NanisLaddoos-16x9" component={NaniLandscape} durationInFrames={NANI_L * NANI_LK} fps={30 * NANI_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
      <Composition id="KnockKnock-16x9" component={KnockLandscape} durationInFrames={KNOCK_L * KNOCK_LK} fps={30 * KNOCK_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
      <Composition id="WhoSaidThat-16x9" component={WhoLandscape} durationInFrames={WHO_L * WHO_LK} fps={30 * WHO_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
      <Composition id="BeforeTheExam-16x9" component={ExamLandscape} durationInFrames={EXAM_L * EXAM_LK} fps={30 * EXAM_LK} width={LW} height={LH} calculateMetadata={calculateMetadata} />
    </Folder>
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
      id="KnockKnock"
      component={KnockVideo}
      durationInFrames={KNOCK_TOTAL}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="NanisLaddoos"
      component={NaniVideo}
      durationInFrames={NANI_TOTAL}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="WhoSaidThat"
      component={WhoVideo}
      durationInFrames={WHO_TOTAL * STORY_K}
      fps={RENDER_FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Still id="DrawnTest" component={TestFrame} width={WIDTH} height={HEIGHT} calculateMetadata={calculateMetadata} />
    <Composition
      id="WhoDidSlides"
      component={SlidesVideo}
      durationInFrames={SLIDES_TOTAL * STORY_K}
      fps={RENDER_FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="WhoDidSlidesDrawn"
      component={DrawnSlides}
      durationInFrames={DRAWN_TOTAL * STORY_K}
      fps={RENDER_FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="BrainCourt"
      component={BrainCourt}
      durationInFrames={COURT_TOTAL * STORY_K}
      fps={RENDER_FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="BeforeTheExam"
      component={ExamVideo}
      durationInFrames={EXAM_TOTAL * STORY_K}
      fps={RENDER_FPS}
      width={WIDTH}
      height={HEIGHT}
      calculateMetadata={calculateMetadata}
    />
    <Composition
      id="MeetingAnimationTest"
      component={MeetingTest}
      durationInFrames={TEST_TOTAL}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
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
    <Still
      id="CastSheet"
      component={CastSheet}
      width={2900}
      height={1640}
      calculateMetadata={calculateMetadata}
    />
    <Folder name="Ideas">
      {IDEA_BOARDS.map(([id, component]) => (
        <Still
          key={id}
          id={id}
          component={component}
          width={SHEET_W}
          height={SHEET_H}
          calculateMetadata={calculateMetadata}
        />
      ))}
      {PRO_BOARDS.map(([id, component]) => (
        <Still
          key={id}
          id={id}
          component={component}
          width={PRO_W}
          height={PRO_H}
          calculateMetadata={calculateMetadata}
        />
      ))}
    </Folder>
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
