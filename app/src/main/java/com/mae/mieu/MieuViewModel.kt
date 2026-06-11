package com.mae.mieu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MieuViewModel(
    private val aphorisms: List<String>,
    val total: Int
) : ViewModel() {

    companion object {
        val FALLBACK = listOf(
            "A Void doesn't fill the existential void... it purrs in it. Somehow, that's enough.",
            "Owning a Void isn't owning a cat; it's adopting a tiny black hole that graciously allows you to exist in its orbit.",
            "With a Void, every night is an adventure in darkness and delight."
        )
    }

    private val _current = MutableStateFlow("")
    val current: StateFlow<String> = _current

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index

    private val _enabled = MutableStateFlow(aphorisms.size >= 2)
    val navigationEnabled: StateFlow<Boolean> = _enabled

    var isTransitioning = false
        private set

    private var shuffledList: List<String> = fisherYates(aphorisms)
    private var idx: Int = 0

    init {
        if (shuffledList.isNotEmpty()) {
            _current.value = shuffledList[0]
            _index.value = 1
        }
    }

    fun advance(delta: Int) {
        if (isTransitioning) return
        val list = shuffledList
        if (list.isEmpty()) return

        isTransitioning = true
        viewModelScope.launch {
            idx += delta
            if (idx >= list.size) {
                shuffledList = fisherYates(list)
                idx = 0
            } else if (idx < 0) {
                idx = list.size - 1
            }
            _current.value = shuffledList[idx]
            _index.value = idx + 1
            delay(300)
            isTransitioning = false
        }
    }

    private fun fisherYates(input: List<String>): List<String> {
        val list = input.toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = (Math.random() * (i + 1)).toInt()
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
        return list
    }
}
