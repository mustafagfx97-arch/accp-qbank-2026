package com.mustafanabeel.antibioticencyclopedia2026

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mustafanabeel.antibioticencyclopedia2026.data.ContentFilter
import com.mustafanabeel.antibioticencyclopedia2026.data.DrugRecord
import com.mustafanabeel.antibioticencyclopedia2026.data.EncyclopediaDataset
import com.mustafanabeel.antibioticencyclopedia2026.data.EncyclopediaRepository
import com.mustafanabeel.antibioticencyclopedia2026.data.ReferenceEntry
import com.mustafanabeel.antibioticencyclopedia2026.data.SearchItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoadState(
    val loading: Boolean = true,
    val dataset: EncyclopediaDataset? = null,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class EncyclopediaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EncyclopediaRepository(application)
    private val preferences = application.getSharedPreferences("encyclopedia_preferences", 0)

    private val _loadState = MutableStateFlow(LoadState())
    val loadState: StateFlow<LoadState> = _loadState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(ContentFilter.ALL)
    val filter: StateFlow<ContentFilter> = _filter

    private val _bookmarks = MutableStateFlow(
        preferences.getStringSet(BOOKMARK_KEY, emptySet())?.toSet().orEmpty()
    )
    val bookmarks: StateFlow<Set<String>> = _bookmarks

    val results: StateFlow<List<SearchItem>> = combine(
        _loadState,
        _query.debounce(100),
        _filter,
    ) { state, query, filter ->
        state.dataset?.let { repository.search(it, query, filter) }.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loadState.value = LoadState(loading = true)
            runCatching { repository.load() }
                .onSuccess { dataset -> _loadState.value = LoadState(loading = false, dataset = dataset) }
                .onFailure { error ->
                    _loadState.value = LoadState(
                        loading = false,
                        error = error.message ?: "تعذر تحميل الموسوعة.",
                    )
                }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: ContentFilter) {
        _filter.value = value
    }

    fun beginSearch(value: String = "", filter: ContentFilter = ContentFilter.ALL) {
        _filter.value = filter
        _query.value = value
    }

    fun drug(id: String): DrugRecord? = _loadState.value.dataset?.drugs?.firstOrNull { it.id == id }

    fun entry(id: String): ReferenceEntry? =
        _loadState.value.dataset?.entries?.firstOrNull { it.id == id }

    fun relatedEntries(drug: DrugRecord): List<ReferenceEntry> = repository.entriesFor(drug)

    fun toggleBookmark(id: String) {
        val changed = _bookmarks.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }.toSet()
        _bookmarks.value = changed
        preferences.edit().putStringSet(BOOKMARK_KEY, changed).apply()
    }

    fun bookmarkedItems(): List<SearchItem> {
        val data = _loadState.value.dataset ?: return emptyList()
        val ids = _bookmarks.value
        val drugs = data.drugs.filter { it.id in ids }.map { SearchItem.Drug(it, 0) }
        val entries = data.entries.filter { it.id in ids }.map { SearchItem.Entry(it, 0) }
        return drugs + entries
    }

    companion object {
        private const val BOOKMARK_KEY = "bookmarked_ids"
    }
}
