package space.iamjustkrishna.srutam.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptPostProcessorTest {
    @Test fun plainSpeechIsUnchanged() {
        assertEquals(
            "Call Priya tomorrow at 3pm, then send the notes.",
            TranscriptPostProcessor.clean("Call Priya tomorrow at 3pm, then send the notes.")
        )
    }

    @Test fun nonSpeechTagsAreRemovedAndSpacingRepaired() {
        assertEquals(
            "Yeah, that works.",
            TranscriptPostProcessor.clean("Yeah, (cough) that works [BLANK_AUDIO] .")
        )
        assertEquals("", TranscriptPostProcessor.clean("[MUSIC]"))
        assertEquals("", TranscriptPostProcessor.clean("*music* ♪♪"))
    }

    @Test fun blankInputStaysBlank() {
        assertEquals("", TranscriptPostProcessor.clean("   \n  "))
    }

    @Test fun whitespaceIsNormalized() {
        assertEquals("one two three", TranscriptPostProcessor.clean("  one \n two\t three  "))
    }

    @Test fun decoderLoopCollapsesToOneOccurrence() {
        val loop = "I think we should go. ".repeat(12).trim()
        assertEquals("Okay. I think we should go.", TranscriptPostProcessor.clean("Okay. $loop"))
        assertEquals("the", TranscriptPostProcessor.clean("the the the the the the the"))
    }

    @Test fun textAfterALoopIsKept() {
        assertEquals("so go home", TranscriptPostProcessor.clean("so so so so so so go home"))
    }

    @Test fun genuineShortRepetitionIsKept() {
        assertEquals("No, no, no, no. Stop.", TranscriptPostProcessor.clean("No, no, no, no. Stop."))
    }

    @Test fun loopComparisonIgnoresCaseAndPunctuation() {
        assertEquals("Go.", TranscriptPostProcessor.clean("Go. go, GO! go go."))
    }
}
