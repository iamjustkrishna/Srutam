# Srutam stories: captions and posting plan

Six animated stories, each in two cuts: **portrait 9:16** for Reels, Shorts and other
vertical feeds, and **landscape 16:9** for YouTube, LinkedIn, X, the website and the
Play Store listing. This file has, for each video, the post captions for every
platform, the cover frame, the key moments (for Story teasers and chapters) and when to
post.

- **Instagram comes first.** Its setup, rules and calendar are in the next section. Every
  other platform follows the Instagram post (see [Other platforms](#other-platforms)).
- **Times are IST (UTC+5:30).** The audience is India-first. The times are starting points:
  after two weeks, move each slot to the "Most active times" in Instagram Insights.
- **Links:** website [srutam.space](https://srutam.space). The Play Store listing is
  `https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam` (confirm the
  listing is live before linking it). Captions say "link in bio" where a platform has no
  clickable links.

## Before you post anything

1. **Voice licence (blocking).** All six videos use ElevenLabs voices: the Spark narrator,
   Meera, and the English character voices in the 16:9 cuts. They were made on the free
   plan, which allows non-commercial use with attribution only. A brand account is
   commercial use. Subscribe to a paid plan (Starter is the cheapest with a commercial
   licence). Then regenerate the lines while subscribed: ElevenLabs states no retroactive
   licence for free-plan audio. Regenerating and re-rendering is scripted
   (`scripts/generate-voiceover-elevenlabs.mjs`, then the renders).
2. **Hindi in the portrait cuts.** In the portrait **Who Said That?**, **Before the Exam**
   and **Nani's Laddoos**, the characters speak Hindi and Srutam answers from those
   recordings. That implies Hindi transcription, which the app doesn't have yet (it
   transcribes English on the phone). Either post English portrait cuts (these can be made
   from the 16:9 English voice takes), or accept the implication until Hindi ships. Spark,
   The 2 a.m. Idea and Knock Knock are fine as they are (Knock's Hindi line is Ma on a
   call, which Srutam doesn't transcribe).
3. **Claims to keep honest in every caption:**
   - Recording is quick from any app, and transcription happens on the phone (English).
   - Insights pull out next steps, ideas, decisions and reminders. You can ask questions
     across your notes.
   - Don't promise Hindi, speaker identification, or offline AI. Ask and Insights use the
     network.
4. **Accessibility is built in:** every video has burned-in English captions. Turn off
   auto-captions where a platform adds them, and fill in alt text where offered.

## The six videos

The portrait files are in `out/`, the landscape files in `out/wide/`. These are render
outputs, not committed; rebuild them with the commands in `../README.md`. Each video has
a cover frame (the time to pick it from), a tagline and a best-fit theme.

| # | Video | Portrait file | Landscape file |
|---|---|---|---|
| 1 | Spark | `out/spark-story-30s.mp4` (30 s) | `out/wide/spark-story-30s-16x9.mp4` (30 s) |
| 2 | The 2 a.m. Idea | `out/two-am-idea.mp4` (40 s) | `out/wide/two-am-idea-16x9.mp4` (40 s) |
| 3 | Knock Knock | `out/knock-knock.mp4` (31.6 s) | `out/wide/knock-knock-16x9.mp4` (32 s) |
| 4 | Before the Exam | `out/before-the-exam-v4.mp4` (43.2 s) | `out/wide/before-the-exam-16x9.mp4` (45.5 s) |
| 5 | Who Said That? | `out/who-said-that-v4.mp4` (48.5 s) | `out/wide/who-said-that-16x9.mp4` (53.5 s) |
| 6 | Nani's Laddoos | `out/nanis-laddoos.mp4` (41.6 s) | `out/wide/nanis-laddoos-16x9.mp4` (42.3 s) |

| # | Video | Cover frame (portrait / landscape) | Tagline | Best fit |
|---|---|---|---|---|
| 1 | Spark | 0:02.4 / 0:02.4 | Catch it before it's gone. | Everyone; product intro |
| 2 | The 2 a.m. Idea | 0:04 / 0:05 | Catch it before it's gone. | Founders, creators |
| 3 | Knock Knock | 0:21.5 / 0:22 | Say it once. Srutam knocks. | Family; Sunday |
| 4 | Before the Exam | 0:03 / 0:04 | Listen once. Ask anytime. | Students; exam season |
| 5 | Who Said That? | 0:02.5 / 0:02 | Every meeting, minuted. | Office, teams |
| 6 | Nani's Laddoos | 0:37 / 0:37 | Keep the recipe. Keep the voice. | Family; Diwali |

---

## Instagram

### Set up once

- **Account:** a professional account (Creator or Business) named **Srutam**.
  Category: App page.
- **Bio:**
  ```text
  Voice notes that remember for you 🎙️
  Tap, talk, done. Transcribed on your phone.
  Next steps, ideas and reminders, pulled out for you.
  Android ↓
  ```
- **Link in bio:** the Play Store listing, plus [srutam.space](https://srutam.space).
- **Story highlights:** "Stories" (each reel's Story frames), "How it works" (tap, talk,
  ask), and "Reminders".
- **Pinned reels** (top of the grid): Spark, Who Said That?, Knock Knock. Swap in Nani's
  Laddoos during Diwali week.

### Rules for every reel

- **Video:** upload the portrait file as it is (1080×1920, H.264/AAC; 30 or 60 fps).
  Don't add Instagram's auto-captions; the captions are already burned in.
- **Cover:** choose the frame from the table above. The profile grid crops covers to 3:4,
  so the centre 1080×1440 must hold the face or the phone.
- **Audio name:** rename the original audio to "Srutam – <title>" so it's credited when
  others reuse it.
- **Caption:**
  - The first line is the hook. About 125 characters show before "more".
  - Then a question that invites comments, then "Srutam for Android: link in bio".
- **Hashtags:** at most **5**, the cap since December 2025. They are already in each
  caption below.
- **Alt text:** use Advanced settings → Accessibility.
- **Facebook:** turn on "Share to Facebook" (Accounts Center). This covers Facebook Reels
  automatically.
- **Collabs:** when a creator partners on a reel, add them as a Collab, so the reel shows
  on both profiles. Good fits: study creators for Before the Exam, food and family creators
  for Nani's Laddoos.

### Calendar

One reel about twice a week, timed to its theme. Diwali 2026 is Sunday 8 November
(Dhanteras is 6 November), so Nani's Laddoos goes out three days before.

| Date | Time (IST) | Reel | Why then |
|---|---|---|---|
| Thu 15 Oct | 7:30 pm | 1 · Spark | Product intro opens the grid; weekday evening |
| Sun 18 Oct | 6:30 pm | 3 · Knock Knock | Lands on a Sunday half an hour before the story's 7 pm call: "It's Sunday. Call her." |
| Wed 21 Oct | 9:30 pm | 2 · The 2 a.m. Idea | Late-night thinking crowd |
| Sun 25 Oct | 9:00 pm | 4 · Before the Exam | Sunday-night study session, ahead of the end-semester season |
| Wed 28 Oct | 7:30 pm | 5 · Who Said That? | Midweek, after office hours |
| Thu 5 Nov | 7:30 pm | 6 · Nani's Laddoos | Diwali week, while families are making sweets |
| Sun 8 Nov | 10:00 am | Story: reshare Nani's Laddoos | Diwali morning |
| Mon 23 Nov | 10:00 pm | Story: reshare Before the Exam | End-semester exams |

### On each reel day

- **Stories, about 30 minutes after the reel:**
  1. A teaser: the first seconds of the video (see "Story teaser" under each video), with a
     poll sticker.
  2. The reel shared to your Story.
  3. A link sticker to the Play Store, with the video's tagline.
- **First hour:**
  - Reply to every comment.
  - Pin the best comment.
  - Post the reel in the broadcast channel, if you run one.
- **After 48 hours:**
  - Look at reach, the 3-second hold, average watch time, shares and saves.
  - If the 3-second hold is weak, test a new first frame or cover as a Trial Reel.
  - If shares are high, put a small boost behind the reel. This needs the paid voice
    licence first.

---

## Other platforms

Each platform takes the cut it plays best, posted after Instagram.

| Platform | Cut | When (IST) | Notes |
|---|---|---|---|
| Facebook Reels | Portrait | Same time as Instagram | Automatic via "Share to Facebook". On the Facebook Page, also post the landscape cut natively for desktop viewers. |
| YouTube Shorts | Portrait | Same day, 8:30 pm | Set the Short's "Related video" to the landscape version. Up to 3 hashtags in the description appear above the title. |
| YouTube (landscape) | Landscape | Same day, 8:30 pm | Put all six in a playlist "Srutam Stories". Add the chapter timestamps given below; YouTube needs at least three, each 10 s or longer, and these qualify. Turn off ads on the one used for Play. |
| Google Play listing | Landscape Spark | Once, before 15 Oct | Use a YouTube URL, public or unlisted. Ads and monetisation off, not age-restricted, embeddable, no timecode in the URL. |
| srutam.space | Landscape | Once, then as each one launches | Spark in the hero section, autoplaying muted (the captions are burned in). The others go in a "Stories" section. |
| Snapchat Spotlight | Portrait | Same evening, 9:00 pm | Large Indian user base. Keep the caption short, plus one topic. |
| WhatsApp Channel and Status | Portrait | Same evening, 8:30 pm | Post the video with a one-line caption and the Play link. Ask the team to put it on their Status. |
| Threads | Portrait | Next day, 1:00 pm | Text-first; one topic tag per post. |
| X | Landscape | Next day, 9:30 am | Plays well on desktop. One or two hashtags at most. |
| LinkedIn | Landscape | Tue–Thu, 9:00 am | Only Spark, The 2 a.m. Idea, Who Said That? and Before the Exam. Put the link in the first comment, not the post. |
| Pinterest | Portrait | Weekend evening, 9:00 pm | Boards: "Productivity", "Study tips", "Family recipes", "Diwali". Nani's Laddoos and Before the Exam fit best. |
| Reddit | Landscape | Once, not per video | One genuine post (for example, the 2 a.m. Idea story as a "made this for my app" post) in subreddits that allow self-promotion. Read each subreddit's rules first. |
| Product Hunt | Landscape Spark | Launch day | Use the YouTube link as the gallery video. |
| TikTok | Portrait | Only for audiences abroad | TikTok is still blocked in India. Skip it unless you target the diaspora. |
| Moj, Josh, ShareChat | — | Later | Hindi-first audiences. Hold these until Hindi transcription ships. |

---

## 1 · Spark ("This idea has 4 seconds to live")

**Files:**
- Portrait: `out/spark-story-30s.mp4`
- Landscape: `out/wide/spark-story-30s-16x9.mp4`

**Instagram:** Thu 15 Oct, 7:30 pm.

**Key moments (both cuts):**
- 0:00 hook, "This idea has 4 seconds to live"
- 0:03.7 ideas lost (shower, driving, mid-meeting)
- 0:08.2 "Tap, tap. From any app."
- 0:09.7 transcribed on the phone
- 0:15.7 Insights
- 0:21.3 every thought connects, just ask
- 0:26.3 end card

**Story teaser:** 0:00–0:08, with a poll sticker: "Lost an idea this week?" Yes / Every day.

**Instagram:**
```text
Your best idea has about four seconds before it's gone. ⏳

Tap, tap from any app and just say it. Srutam records it, transcribes it right on your phone, and pulls out your next steps, ideas and decisions. Later, just ask.

What's the last idea you lost? 👇

Srutam for Android: link in bio.

#Srutam #VoiceNotes #ProductivityTips #AndroidApps #IdeasToAction
```

**YouTube Shorts:**
```text
Title: This idea has 4 seconds to live ⏳ #Shorts
Description: Tap, tap from any app. Srutam records your idea, transcribes it on your phone, and pulls out the next steps. Catch it before it's gone.
Get it on Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
#Srutam #VoiceNotes #Productivity
```

**YouTube (landscape):**
```text
Title: Catch it before it's gone | Srutam (30-second animated short)
Description: Most ideas never make it past the shower, the drive or the meeting. Srutam catches them: tap from any app, talk, and it's transcribed on your phone, with next steps, ideas and decisions pulled out for you. Then just ask.

0:00 Four seconds to live
0:10 Caught and transcribed
0:20 Insights, and every thought connects

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
Website: https://srutam.space
```

**LinkedIn:**
```text
The best ideas at work rarely arrive at your desk. They show up in the shower, on the drive, halfway through someone else's meeting, and they're gone in seconds.

We made Srutam for that moment: tap from any app, say it, and it's transcribed on your phone. Next steps, ideas and decisions get pulled out for you, and later you can just ask.

Where do your best ideas show up?

#Productivity #VoiceNotes #Android
```

**X:**
```text
This idea has 4 seconds to live. ⏳

Tap, tap from any app, say it, and Srutam catches it: transcribed on your phone, next steps pulled out.

Catch it before it's gone. srutam.space
```

**Threads:**
```text
Your best idea has about four seconds before it's gone. Srutam catches it: tap from any app, talk, and it's transcribed on your phone. What's the last idea you lost?
```
Tag: `Productivity`

**Pinterest:**
```text
Title: Catch your ideas before they're gone
Description: A 30-second animated short about the ideas we lose in the shower, on the drive and in meetings, and an app that catches them. Tap from any app, talk, and Srutam transcribes on your phone and pulls out next steps. Board: Productivity.
```

**Snapchat and WhatsApp:**
```text
This idea has 4 seconds to live. Catch it with Srutam ⏳ (Play link)
```

---

## 2 · The 2 a.m. Idea

**Files:**
- Portrait: `out/two-am-idea.mp4`
- Landscape: `out/wide/two-am-idea-16x9.mp4`

**Instagram:** Wed 21 Oct, 9:30 pm.

**Key moments (both cuts):**
- 0:00 "2:13 a.m. The best idea I've ever had."
- 0:08.5 "I did not remember it in the morning."
- 0:11.5 rewind
- 0:14 tap and whisper it to Srutam
- 0:24 saved, back to sleep
- 0:27.3 "And in the morning, it's all there."
- 0:33.3 three months later: Chai & Chapters
- 0:36.2 end card

**Story teaser:** 0:00–0:11, with a poll sticker: "Ever lost a 2 a.m. idea?" Yes / Every week.

**Instagram:**
```text
2:13 a.m. Best idea ever. 7:02 a.m. Gone. 😩

Next time: tap, whisper it, back to sleep. In the morning it's all there: the idea, the next step, even a reminder.

Three months later, that idea had a name. ☕📚

Tag the friend who has 2 a.m. ideas.

Srutam for Android: link in bio.

#Srutam #2AMThoughts #StartupIdeas #VoiceNotes #AndroidApps
```

**YouTube Shorts:**
```text
Title: The 2 a.m. idea I almost lost 💡 #Shorts
Description: Tap. Say it. Back to sleep. In the morning Srutam has the idea, the next step and a reminder. Catch it before it's gone.
#Srutam #StartupIdeas #VoiceNotes
```

**YouTube (landscape):**
```text
Title: The 2 a.m. Idea | Srutam (animated short)
Description: The best idea you've ever had arrives at 2:13 a.m., and it's gone by morning. Unless you tap, say it, and go back to sleep. Srutam keeps the idea, finds the next step and sets the reminder.

0:00 2:13 a.m.
0:14 Tap, say it, back to sleep
0:27 It's all there

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
```

**LinkedIn:**
```text
Every founder has lost a 2 a.m. idea. Ours was a rooftop café for book lovers: swap a book, get a chai.

The fix turned out to be simple: tap, say it, go back to sleep. By morning the idea was written down, the next step was clear ("call Priya about her terrace") and there was a reminder for Saturday.

What's an idea you're glad you wrote down?

#Founders #Startups #Productivity
```

**X:**
```text
2:13 a.m.: best idea ever.
7:02 a.m.: gone.

Now: tap, whisper it, back to sleep. Srutam has the idea, the next step and a reminder by morning. 💡
```

**Threads:**
```text
2:13 a.m. Best idea ever. 7:02 a.m. Gone. Next time: tap, say it, back to sleep. In the morning the idea, the next step and a reminder are waiting.
```
Tag: `Startups`

**Pinterest:**
```text
Title: Never lose a 2 a.m. idea again
Description: A 40-second animated short: whisper the idea, go back to sleep, and wake up to the idea, the next step and a reminder. Board: Productivity.
```

**Snapchat and WhatsApp:**
```text
2 a.m. ideas, saved. Tap, say it, sleep 💡 (Play link)
```

---

## 3 · Knock Knock

**Files:**
- Portrait: `out/knock-knock.mp4`
- Landscape: `out/wide/knock-knock-16x9.mp4`

**Instagram:** Sun 18 Oct, 6:30 pm.

**Key moments:**
- 0:00 "knock knock!"
- 0:03.5 "Okay Ma, I'll call you on Sunday. Promise!"
- 0:08.5 Sunday came, and she got busy
- 0:13.5 "Call Ma, Sunday at 7" said once to Srutam
- 0:20 the reminder knocks at 6:45
- 0:25 the call: "Oh! You remembered!"
- 0:27.3 end card (landscape: 0:27.7)

**Story teaser:** 0:00–0:08, with a question sticker: "When did you last call home?"

**Instagram:**
```text
"Okay Ma, I'll call you on Sunday. Promise!" 🙈

Sunday came… and I got busy. Now I say it once, "Call Ma, Sunday at 7", and Srutam knocks at 6:45.

It's Sunday. This is your sign. Call her. 📞

Srutam for Android: link in bio.

#Srutam #CallYourMom #Reminders #SundayVibes #AndroidApps
```

**YouTube Shorts:**
```text
Title: Knock knock. It's Sunday. Call Ma 📞 #Shorts
Description: Say it once and Srutam knocks when it's time. Say it once. Srutam knocks.
#Srutam #Reminders #Family
```

**YouTube (landscape):**
```text
Title: Knock Knock | Srutam (animated short)
Description: We all promise to call on Sunday. Say it once, "Call Ma, Sunday at 7", and Srutam knocks at 6:45.

0:00 The promise
0:10 Say it once
0:20 Srutam knocks

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
```

**X:**
```text
"Okay Ma, I'll call you on Sunday. Promise!"

Say it once. Srutam knocks at 6:45. 📞
```

**Threads:**
```text
"I'll call you on Sunday, Ma. Promise." We all say it. Say it once to Srutam and it knocks when it's time. It's Sunday. Call her.
```
Tag: `Family`

**Pinterest:**
```text
Title: The Sunday call reminder that actually works
Description: A 30-second animated short about a promise to call Mom, and a reminder that knocks at the right time. Board: Family.
```

**Snapchat and WhatsApp:**
```text
It's Sunday. Call Ma 📞 Say it once, Srutam knocks. (Play link)
```

---

## 4 · Before the Exam

**Files:**
- Portrait: `out/before-the-exam-v4.mp4`
- Landscape: `out/wide/before-the-exam-16x9.mp4`

**Instagram:** Sun 25 Oct, 9:00 pm. Story reshare on Mon 23 Nov, 10:00 pm.

**Key moments (portrait / landscape):**

| Moment | Portrait | Landscape |
|---|---|---|
| "Exam at nine. And I forgot what Prof said." | 0:00 | 0:00 |
| The group chat | 0:04.4 | 0:04.4 |
| The flashback lecture | 0:08.5 | 0:08.5 |
| She asks Srutam | 0:14 | 0:14.7 |
| The lecture plays the answer | 0:19.2 | 0:19.9 |
| Shared with the group | 0:25.6 | 0:26.6 |
| Chai arrives | 0:28.3 | 0:29.3 |
| Time-lapse | 0:31.9 | 0:34.1 |
| The reminder | 0:35.5 | 0:37.8 |
| The exam hall | 0:36.5 | 0:38.8 |
| End card | 0:39.5 | 0:41.8 |

**Story teaser:** 0:00–0:08, with a poll sticker: "Ever forgotten what the Prof said?" Yes / Every exam.

**Instagram:**
```text
2:07 a.m. Exam at nine. And I forgot what Prof said. 😵

Half the class was asleep in that lecture. Srutam wasn't. One question, and the answer comes straight from the recording: chapters 4 and 7, the metro case study, 20 marks.

Then chai, Key Points, and a reminder 15 minutes before the exam.

Send this to your study group. 📚

Srutam for Android: link in bio.

#Srutam #ExamSeason #StudyTips #StudentLife #LectureNotes
```

**YouTube Shorts:**
```text
Title: 2 a.m., exam at 9, forgot what Prof said 😵 #Shorts
Description: Listen once. Ask anytime. Srutam answers from the lecture recording and cites it.
#Srutam #StudyTips #ExamSeason
```

**YouTube (landscape):**
```text
Title: 2 a.m. Before the Exam | Srutam (animated short)
Description: Exam at nine, and nobody remembers what Prof said. Meera asks Srutam, and the answer comes from Monday's lecture, with the moment ready to play.

0:00 2:07 a.m.
0:14 Ask Srutam
0:26 Chai, Key Points and the exam

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
```

**LinkedIn:**
```text
Students don't need more notes. They need the one sentence the professor said about the exam.

In our latest short, Meera asks Srutam at 2 a.m. what's coming in tomorrow's Econ exam. The answer comes straight from the lecture recording, with the moment ready to replay.

Listen once. Ask anytime.

#EdTech #Students #Productivity
```

**X:**
```text
2:07 a.m. Exam at 9. Nobody remembers what Prof said.

Srutam does: chapters 4 and 7, the metro case study, 20 marks. Straight from the lecture. 📚
```

**Threads:**
```text
2:07 a.m., exam at nine, and the whole group chat forgot what Prof said. One question to Srutam and the lecture answers. Listen once. Ask anytime.
```
Tag: `StudyTips`

**Pinterest:**
```text
Title: The study hack for "what did the professor say?"
Description: A 45-second animated short: record the lecture once, then ask what's on the exam and hear the exact moment. Board: Study tips.
```

**Snapchat and WhatsApp:**
```text
Exam at 9, forgot what Prof said? Ask Srutam 📚 (Play link)
```

---

## 5 · Who Said That?

**Files:**
- Portrait: `out/who-said-that-v4.mp4`
- Landscape: `out/wide/who-said-that-16x9.mp4`

**Instagram:** Wed 28 Oct, 7:30 pm. LinkedIn on Tue 27 Oct, 9:00 am.

**Key moments (portrait / landscape):**

| Moment | Portrait | Landscape |
|---|---|---|
| "The 14th? Who said yes to the 14th?!" | 0:00 | 0:00 |
| One week earlier | 0:03.5 | 0:03.9 |
| Recording starts | 0:04.7 | 0:05.1 |
| "No, no. The 14th. Final." | 0:12 | 0:14.7 |
| "Someone note it down!" | 0:17.1 | 0:20.7 |
| The Insights cards | 0:24 | 0:27.9 |
| The blame game | 0:30.6 | 0:34.5 |
| "Let's ask Srutam." | 0:33.7 | 0:37.8 |
| The replay | 0:39.9 | 0:43.9 |
| The payoff | 0:42.5 | 0:47 |
| End card | 0:45.3 | 0:50.3 |

**Story teaser:** 0:00–0:04, with a poll sticker: "Has your boss ever forgotten their own decision?" Yes / Every week.

**Instagram:**
```text
"The 14th? Who said yes to the 14th?!" 😤

A week ago, in Tuesday's sync, he did. Nobody wrote it down. Except Srutam.

One question, one replay, and the room goes quiet.

Tag the colleague who needs this before Monday's meeting. 👀

Srutam for Android: link in bio.

#Srutam #OfficeHumor #MeetingNotes #WorkLife #ProductivityApp
```

**YouTube Shorts:**
```text
Title: "Who said yes to the 14th?!" 😤 #Shorts
Description: Nobody wrote it down. Except Srutam. Every meeting, minuted.
#Srutam #OfficeHumor #MeetingNotes
```

**YouTube (landscape):**
```text
Title: Who Said That? | Srutam (animated short)
Description: The launch date was decided last Tuesday, and now nobody agrees on who said yes. Srutam was listening: one question, the exact moment, and the room hears it for itself.

0:00 Who said yes to the 14th?
0:14 Tuesday's sync
0:28 Nobody wrote it down. Except Srutam.
0:38 Let's ask Srutam

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
```

**LinkedIn:**
```text
"Who agreed to the 14th?"

Every team has had this meeting about the last meeting. The decision was made out loud, nobody wrote it down, and a week later it's anyone's guess.

Srutam records the meeting, pulls out the decision, the next step and the reminder before lunch, and when the question comes up, you can play back the exact moment.

Every meeting, minuted.

#Meetings #Teamwork #Productivity
```

**X:**
```text
"Who said yes to the 14th?!"

Nobody wrote it down. Except Srutam. One question, one replay, and the meeting settles itself. 😤
```

**Threads:**
```text
The meeting about the last meeting: "Who said yes to the 14th?!" Nobody wrote it down, except Srutam. Every meeting, minuted.
```
Tag: `WorkLife`

**Pinterest:**
```text
Title: Never argue about what was decided in a meeting again
Description: A 50-second animated office comedy: the decision was made out loud, nobody wrote it down, and the recording settles it. Board: Productivity.
```

**Snapchat and WhatsApp:**
```text
"Who said yes to the 14th?!" Srutam knows 😤 (Play link)
```

---

## 6 · Nani's Laddoos

**Files:**
- Portrait: `out/nanis-laddoos.mp4`
- Landscape: `out/wide/nanis-laddoos-16x9.mp4`

**Instagram:** Thu 5 Nov, 7:30 pm. Story reshare on Sun 8 Nov, 10:00 am (Diwali).

**Key moments (portrait / landscape):**

| Moment | Portrait | Landscape |
|---|---|---|
| "My Nani never measured anything." | 0:00 | 0:00 |
| She lets Srutam listen | 0:08 | 0:08.8 |
| Diwali, 1,400 km away | 0:12.7 | 0:13.5 |
| She asks Srutam | 0:20 | 0:20.8 |
| Nani's voice plays | 0:25.7 | 0:26.5 |
| The video call | 0:33.7 | 0:34.5 |
| End card | 0:37.3 | 0:38.1 |

**Story teaser:** 0:00–0:08, with a question sticker: "Which family recipe lives only in someone's head?"

**Instagram:**
```text
Nani never measured anything. "You go by the smell!" 🪔

One Sunday, I let Srutam listen while she cooked. This Diwali I'm 1,400 km away, so I asked: how much gur goes in?

"One fistful of gur for every bowl of besan… and a little love." In her own voice.

Record the recipes before they're only memories. Who's the Nani in your family? 👇

Srutam for Android: link in bio.

#Srutam #Diwali #FamilyRecipes #Laddoo #DiwaliSweets
```

**YouTube Shorts:**
```text
Title: Nani never measured anything 🪔 #Shorts
Description: Keep the recipe. Keep the voice. Happy Diwali from Srutam.
#Srutam #Diwali #FamilyRecipes
```

**YouTube (landscape):**
```text
Title: Nani's Laddoos | A Diwali story from Srutam
Description: Nani never measured anything, she went by the smell. One Sunday her granddaughter let Srutam listen. This Diwali, 1,400 km away, the recipe comes back in Nani's own voice.

0:00 Nani's kitchen
0:13 Diwali, far from home
0:26 In her own voice

Android: https://play.google.com/store/apps/details?id=space.iamjustkrishna.srutam
```

**X:**
```text
Nani never measured anything. "You go by the smell!"

This Diwali, 1,400 km away, her laddoo recipe came back in her own voice. 🪔 Keep the recipe. Keep the voice.
```

**Threads:**
```text
Nani never measured anything. One Sunday I let Srutam listen while she cooked. This Diwali, far from home, her recipe came back in her own voice. Who's the Nani in your family?
```
Tag: `Diwali`

**Pinterest:**
```text
Title: Save Nani's recipes in her own voice
Description: A Diwali short about besan laddoos, a grandmother who never measured, and keeping her recipe and her voice. One fistful of gur for every bowl of besan, and a little love. Boards: Family recipes, Diwali.
```

**Snapchat and WhatsApp:**
```text
Happy Diwali 🪔 Keep Nani's recipe, in her voice. (Play link)
```

---

## Sources

- Instagram's 5-hashtag cap (December 2025):
  [Later](https://later.com/blog/instagram-hashtags/amp),
  [Inro](https://www.inro.social/tools/instagram-hashtag-generator)
- Grid crop to 3:4:
  [Oktopost](https://www.oktopost.com/blog/instagram-grid-size-guide/),
  [Hopper HQ](https://www.hopperhq.com/blog/instagram-reel-size/)
- Instagram times for India:
  [WsCube Tech](https://www.wscubetech.com/blog/instagram-post-time/),
  [timetopost.co](https://timetopost.co/best-time-to-post/instagram/india/)
- Google Play promo video rules:
  [AppLaunchFlow](https://www.applaunchflow.com/blog/google-play-promo-video-requirements-2026),
  [Gummicube](https://www.gummicube.com/blog/google-play-store-preview-video-guidelines/)
- ElevenLabs commercial use:
  [ElevenLabs billing docs](https://elevenlabs.io/docs/overview/administration/billing.md),
  [Oakgen](https://oakgen.ai/blog/elevenlabs-free-commercial-use)
- Diwali 2026 dates:
  [SmartPuja](https://www.smartpuja.com/blog/diwali-2026-date-lakshmi-puja-muhurat-vidhi/),
  [Kissht](https://www.kissht.com/blog/diwali-2026-date-and-lakshmi-puja-timing)
- TikTok in India:
  [Storyboard18](https://www.storyboard18.com/amp/brand-makers/tiktok-ban-in-india-still-stands-company-clarifies-after-brief-website-access-79505.htm)
- LinkedIn video specs:
  [Ligo Social](https://ligosocial.com/blog/linkedin-video-post-best-practices)

Platform rules change often. Check each platform's help pages before a big launch.
