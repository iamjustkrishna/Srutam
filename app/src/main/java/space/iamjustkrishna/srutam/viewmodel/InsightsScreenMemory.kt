package space.iamjustkrishna.srutam.viewmodel

import android.os.Bundle

data class InsightsScreenMemory(
    val selection: String? = null,
    val ideaSearch: String = "", val decisionSearch: String = "",
    val datesExpanded: Boolean = false, val themesExpanded: Boolean = false, val completedExpanded: Boolean = false,
    val taskIndex: Int = 0, val taskOffset: Int = 0,
    val ideaIndex: Int = 0, val ideaOffset: Int = 0,
    val decisionIndex: Int = 0, val decisionOffset: Int = 0
) {
    fun toBundle() = Bundle().apply {
        putString("selection", selection); putString("ideaSearch", ideaSearch); putString("decisionSearch", decisionSearch)
        putBoolean("datesExpanded", datesExpanded); putBoolean("themesExpanded", themesExpanded); putBoolean("completedExpanded", completedExpanded)
        putInt("taskIndex", taskIndex); putInt("taskOffset", taskOffset)
        putInt("ideaIndex", ideaIndex); putInt("ideaOffset", ideaOffset)
        putInt("decisionIndex", decisionIndex); putInt("decisionOffset", decisionOffset)
    }
    companion object {
        fun from(bundle: Bundle?) = bundle?.let {
            InsightsScreenMemory(it.getString("selection"), it.getString("ideaSearch").orEmpty(), it.getString("decisionSearch").orEmpty(),
                it.getBoolean("datesExpanded"), it.getBoolean("themesExpanded"), it.getBoolean("completedExpanded"),
                it.getInt("taskIndex"), it.getInt("taskOffset"), it.getInt("ideaIndex"), it.getInt("ideaOffset"),
                it.getInt("decisionIndex"), it.getInt("decisionOffset"))
        } ?: InsightsScreenMemory()
    }
}
