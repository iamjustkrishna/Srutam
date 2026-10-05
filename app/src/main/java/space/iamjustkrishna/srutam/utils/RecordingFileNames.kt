package space.iamjustkrishna.srutam.utils

import java.io.File

/**
 * A note's title is its file name without ".m4a", so renaming means finding a name no other note uses.
 * The rules live here so the rename dialog (a hint while typing) and the rename itself (the real check)
 * can never disagree.
 */
object RecordingFileNames {
    private const val EXTENSION = ".m4a"
    private const val MAX_TITLE_LENGTH = 120
    private val illegalCharacters = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    /** What the user typed, without surrounding spaces or a typed-in ".m4a". */
    fun titleOf(input: String): String = input.trim().removeSuffix(EXTENSION).removeSuffix(EXTENSION.uppercase()).trim()

    fun fileNameFor(input: String): String = titleOf(input) + EXTENSION

    /** The .m4a names in [folder], or none if it cannot be read. */
    fun namesIn(folder: File?): List<String> =
        folder?.list { _, name -> name.endsWith(EXTENSION, ignoreCase = true) }?.toList().orEmpty()

    /**
     * Returns why [input] cannot be used as a note's name, or null if it can.
     * [existingFileNames] are the files already in the folder; [ownFileName] is the note's current file,
     * which is allowed (keeping or re-casing your own name is not a clash).
     */
    fun validate(input: String, existingFileNames: Collection<String>, ownFileName: String?): String? {
        val title = titleOf(input)
        if (title.isEmpty() || title.all { it == '.' }) return "Enter a name"
        if (title.length > MAX_TITLE_LENGTH) return "Use $MAX_TITLE_LENGTH characters or fewer"
        if (title.any { it in illegalCharacters }) {
            return "These characters can't be used: / \\ : * ? \" < > |"
        }
        val candidate = fileNameFor(title)
        val taken = existingFileNames.any {
            it.equals(candidate, ignoreCase = true) && !it.equals(ownFileName, ignoreCase = true)
        }
        return if (taken) "A note named \"$title\" already exists" else null
    }
}
