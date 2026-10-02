package space.iamjustkrishna.srutam.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import space.iamjustkrishna.srutam.data.InsightEntity
import space.iamjustkrishna.srutam.data.InsightKind
import space.iamjustkrishna.srutam.data.Recording
import space.iamjustkrishna.srutam.data.ReminderEntity
import space.iamjustkrishna.srutam.utils.AppPreferences

/**
 * Regression tests for the silent-upload-failure bug.
 *
 * The three child upserts (action_items, note_insights, reminders) used to end in
 * `.execute().close()`, discarding the response. A missing table or column
 * returned 404/400 and `uploadNote` still answered Result.success, so the note
 * was marked SYNCED with none of its children attached - which is how reminders
 * and decisions came to be invisible to agents while the app showed everything
 * as synced.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ChildUploadFailureTest {

    private lateinit var server: MockWebServer
    private lateinit var client: SupabaseCloudClient
    private lateinit var context: Context
    private val now = 1_800_000_000_000L

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferences.saveCloudSession(
            context,
            userId = "user-1",
            email = "k@example.com",
            accessToken = "test-token",
            refreshToken = null
        )
        server = MockWebServer()
        server.start()
        client = SupabaseCloudClient(context, baseUrlOverride = server.url("/").toString())
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun recording() = Recording(
        id = 1, audioFilePath = "a.m4a", timestamp = now, name = "Roadmap", cloudId = null
    )

    private fun action() = InsightEntity(
        id = "ins-1", recordingId = 1, kind = InsightKind.ACTION, text = "Send proposal", createdAt = now
    )

    private fun idea() = InsightEntity(
        id = "ins-2", recordingId = 1, kind = InsightKind.IDEA, text = "Family plan", createdAt = now
    )

    private fun reminder() = ReminderEntity(
        id = "rem-1", recordingId = 1, title = "Review", eventTimeMs = now, originalText = "review tomorrow"
    )

    /** The note POST must succeed first, otherwise we would not reach the children. */
    private fun enqueueNoteCreated() {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"id":"11111111-2222-4333-8444-555555555555"}]""")
        )
    }

    @Test
    fun aMissingActionItemsRelationFailsTheWholeNoteSync() = runBlocking {
        enqueueNoteCreated()
        // Exactly what PostgREST answers when migration 07/08 has not been applied.
        server.enqueue(
            MockResponse().setResponseCode(404)
                .setBody("""{"message":"relation \"public.action_items\" does not exist"}""")
        )

        val result = client.uploadNote(recording(), listOf(action()))

        assertTrue("a 404 on the child upsert must fail the note", result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun theFailureMessageNamesTheMissingRelationSoItCanBeDiagnosed() = runBlocking {
        enqueueNoteCreated()
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"message":"column \"client_insight_id\" does not exist"}""")
        )

        val error = client.uploadNote(recording(), listOf(action())).exceptionOrNull()

        // The body is the only thing that says WHICH migration is missing; losing it
        // is what made this bug take so long to find.
        assertTrue("message should name the column: ${error?.message}", error?.message?.contains("client_insight_id") == true)
        assertTrue("message should carry the status: ${error?.message}", error?.message?.contains("400") == true)
    }

    @Test
    fun aFailedRemindersUpsertFailsTheNoteEvenThoughEarlierChildrenSucceeded() = runBlocking {
        enqueueNoteCreated()
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) // action_items ok
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) // note_insights ok
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"no reminders table"}""")) // reminders fails

        val result = client.uploadNote(recording(), listOf(action(), idea()), listOf(reminder()))

        assertTrue("reminders failing must not be swallowed", result.isFailure)
    }

    @Test
    fun aFullySuccessfulUploadStillReportsTheCloudNoteId() = runBlocking {
        enqueueNoteCreated()
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]"))

        val result = client.uploadNote(recording(), listOf(action(), idea()), listOf(reminder()))

        assertTrue(result.isSuccess)
        assertEquals("11111111-2222-4333-8444-555555555555", result.getOrNull())
    }

    @Test
    fun theNewParityFieldsAreActuallySentForRemindersAndInsights() = runBlocking {
        enqueueNoteCreated()
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) // action_items
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) // note_insights
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]")) // reminders

        client.uploadNote(
            recording(),
            listOf(action(), idea()),
            listOf(reminder().copy(needsReview = true, timePrecision = "UNKNOWN", localDate = "2026-11-20"))
        )

        server.takeRequest() // notes
        server.takeRequest() // action_items
        val insightsBody = server.takeRequest().body.readUtf8()
        val remindersBody = server.takeRequest().body.readUtf8()

        // Without these the cloud stays a lossy projection and agents cannot tell
        // an archived idea from an open one, or a guess from a confirmed time.
        assertTrue("insights must carry status", insightsBody.contains("\"status\""))
        assertTrue("reminders must carry needs_review", remindersBody.contains("\"needs_review\":true"))
        assertTrue("reminders must carry time_precision", remindersBody.contains("\"time_precision\":\"UNKNOWN\""))
        assertTrue("reminders must carry local_date", remindersBody.contains("\"local_date\":\"2026-11-20\""))
    }
}
