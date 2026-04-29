package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.onmi.qing.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// MBTI测试 ViewModel
@HiltViewModel
class MbtiViewModel @Inject constructor() : ViewModel() {

// UI状态
    data class UiState(
        val currentQuestionIndex: Int = 0,
        val questions: List<MbtiQuestion> = emptyList(),
        val answers: Map<Int, Int> = emptyMap(),  // questionId -> selectedOptionIndex (0=E, 1=I)
        val result: String? = null,               // MBTI结果，如 "ENFP"
        val resultDescription: String = "",        // 人格描述
        val isComplete: Boolean = false,
        val isCalculating: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(questions = getMbtiQuestions())
        }
    }

// 获取当前题目
    fun getCurrentQuestion(): MbtiQuestion? {
        return _uiState.value.questions.getOrNull(_uiState.value.currentQuestionIndex)
    }

// 获取当前题目的答案
    fun getCurrentAnswer(): Int? {
        val question = getCurrentQuestion() ?: return null
        return _uiState.value.answers[question.id]
    }

    // 获取当前题目维度
    fun getCurrentDimension(): String? {
        val question = getCurrentQuestion() ?: return null
        return getQuestionDimension(question.id)
    }

// 获取测试进度
    fun getProgress(): Float {
        val state = _uiState.value
        if (state.questions.isEmpty()) return 0f
        return (state.currentQuestionIndex + 1).toFloat() / state.questions.size
    }

    // 获取进度百分比
    fun getProgressPercent(): Int {
        return (getProgress() * 100).toInt()
    }

    // 回答题目
    fun answerQuestion(questionId: Int, optionIndex: Int) {
        _uiState.update { state ->
            val newAnswers = state.answers.toMutableMap()
            newAnswers[questionId] = optionIndex
            state.copy(answers = newAnswers)
        }
    }

// 下一题
    fun nextQuestion() {
        _uiState.update { state ->
            val nextIndex = minOf(state.currentQuestionIndex + 1, state.questions.size - 1)
            state.copy(currentQuestionIndex = nextIndex)
        }
    }

    // 上一题
    fun previousQuestion() {
        _uiState.update { state ->
            val prevIndex = maxOf(state.currentQuestionIndex - 1, 0)
            state.copy(currentQuestionIndex = prevIndex)
        }
    }

    // 是否是最后一题
    fun isLastQuestion(): Boolean {
        val state = _uiState.value
        return state.currentQuestionIndex == state.questions.size - 1
    }

    // 是否是第一题
    fun isFirstQuestion(): Boolean {
        return _uiState.value.currentQuestionIndex == 0
    }

    // 完成测试，计算结果
    fun completeTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }

            val state = _uiState.value
            val result = calculateMbtiResult(state.answers, state.questions)
            val description = getMbtiTypeDescription(result)

            _uiState.update {
                it.copy(
                    result = result,
                    resultDescription = description,
                    isComplete = true,
                    isCalculating = false
                )
            }
        }
    }

    // 重置测试
    fun reset() {
        _uiState.value = UiState(questions = getMbtiQuestions())
    }

    // 退出测试
    fun exitTest() {
        reset()
    }

}
