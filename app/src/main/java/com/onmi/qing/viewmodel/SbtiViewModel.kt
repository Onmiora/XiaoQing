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

// SBTI测试 ViewModel
@HiltViewModel
class SbtiViewModel @Inject constructor() : ViewModel() {

// 测试流程步骤
    enum class SbtiStep {
        GATE_QUESTIONS,     // 筛选门问题
        FORMAL_QUESTIONS,   // 正式测试
        RESULT             // 结果
    }

// UI状态
    data class UiState(
        val currentStep: SbtiStep = SbtiStep.GATE_QUESTIONS,
        val gateQuestionIndex: Int = 0,        // 当前筛选门问题索引
        val currentQuestionIndex: Int = 0,     // 当前正式问题索引
        val gateQuestions: List<SbtiQuestion> = emptyList(),  // 筛选门问题
        val formalQuestions: List<SbtiQuestion> = emptyList(), // 正式问题
        val answers: Map<Int, Int> = emptyMap(), // 所有答案 (questionId -> selectedOptionIndex)
        val gateQuestion1AnswerIndex: Int? = null,  // drink_gate_q1的答案索引
        val gateQuestion1AnswerWeight: Int? = null,   // drink_gate_q1的答案分值
        val gateQuestion2AnswerIndex: Int? = null,  // drink_gate_q2的答案索引
        val gateQuestion2AnswerWeight: Int? = null,   // drink_gate_q2的答案分值
        val showDrinkGate2: Boolean = false,   // 是否显示第二个筛选门问题
        val result: PersonalityType? = null,   // 测试结果
        val similarity: Int = 0,               // 相似度
        val isComplete: Boolean = false,       // 是否完成
        val isCalculating: Boolean = false     // 是否正在计算结果
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // 初始化问题
        _uiState.update {
            it.copy(
                gateQuestions = getSbtiGateQuestions(),
                formalQuestions = getSbtiQuestions()
            )
        }
    }

// 获取当前问题
    fun getCurrentQuestion(): SbtiQuestion? {
        val state = _uiState.value
        return when (state.currentStep) {
            SbtiStep.GATE_QUESTIONS -> {
                state.gateQuestions.getOrNull(state.gateQuestionIndex)
            }
            SbtiStep.FORMAL_QUESTIONS -> {
                state.formalQuestions.getOrNull(state.currentQuestionIndex)
            }
            SbtiStep.RESULT -> null
        }
    }

// 获取正式问题的当前题目
    fun getCurrentFormalQuestion(): SbtiQuestion? {
        return _uiState.value.formalQuestions.getOrNull(_uiState.value.currentQuestionIndex)
    }

// 获取正式问题的当前答案
    fun getCurrentFormalAnswer(): Int? {
        val question = getCurrentFormalQuestion() ?: return null
        return _uiState.value.answers[question.id]
    }

// 获取正式问题进度
    fun getFormalProgress(): Float {
        val state = _uiState.value
        if (state.formalQuestions.isEmpty()) return 0f
        return (state.currentQuestionIndex + 1).toFloat() / state.formalQuestions.size
    }

// 获取正式问题进度百分比
    fun getFormalProgressPercent(): Int {
        return (getFormalProgress() * 100).toInt()
    }

    // 回答筛选门问题
    fun answerGateQuestion(optionIndex: Int) {
        viewModelScope.launch {
            _uiState.update { state ->
                val question = state.gateQuestions.getOrNull(state.gateQuestionIndex) ?: return@update state
                val questionId = question.id
                val selectedWeight = question.weights.getOrNull(optionIndex) ?: return@update state

                val newAnswers = state.answers.toMutableMap()
                newAnswers[questionId] = optionIndex

                // 根据题目ID记录答案的索引和分值
                val (newGate1Index, newGate1Weight) = if (questionId == -2) {
                    Pair(optionIndex, selectedWeight)
                } else {
                    Pair(state.gateQuestion1AnswerIndex, state.gateQuestion1AnswerWeight)
                }

                val (newGate2Index, newGate2Weight) = if (questionId == -1) {
                    Pair(optionIndex, selectedWeight)
                } else {
                    Pair(state.gateQuestion2AnswerIndex, state.gateQuestion2AnswerWeight)
                }

                // drink_gate_q1 (id=-2): 选择"饮酒"(weight=3)时触发drink_gate_q2
                val isDrinkSelected = questionId == -2 && selectedWeight == 3

                // drink_gate_q2 (id=-1): 选择B(weight=2)时触发DRUNK
                val isDrunkTriggered = questionId == -1 && selectedWeight == 2

                // 如果触发了DRUNK，直接显示结果
                if (isDrunkTriggered) {
                    return@update state.copy(
                        answers = newAnswers,
                        gateQuestion1AnswerIndex = newGate1Index,
                        gateQuestion1AnswerWeight = newGate1Weight,
                        gateQuestion2AnswerIndex = newGate2Index,
                        gateQuestion2AnswerWeight = newGate2Weight,
                        currentStep = SbtiStep.RESULT,
                        result = getPersonalityTypes().find { it.code == "DRUNK" },
                        similarity = 100,
                        isComplete = true
                    )
                }

                // 如果选择了饮酒，进入第二个筛选门
                if (questionId == -2 && isDrinkSelected) {
                    return@update state.copy(
                        answers = newAnswers,
                        gateQuestion1AnswerIndex = newGate1Index,
                        gateQuestion1AnswerWeight = newGate1Weight,
                        showDrinkGate2 = true
                    )
                }

                // 如果是第一个筛选门问题回答完毕但没选饮酒，进入正式测试
                if (questionId == -2 && !isDrinkSelected) {
                    return@update state.copy(
                        answers = newAnswers,
                        gateQuestion1AnswerIndex = newGate1Index,
                        gateQuestion1AnswerWeight = newGate1Weight,
                        showDrinkGate2 = false,
                        currentStep = SbtiStep.FORMAL_QUESTIONS
                    )
                }

                // 否则（第二个筛选门问题回答完毕但没触发DRUNK），进入正式测试
                state.copy(
                    answers = newAnswers,
                    gateQuestion2AnswerIndex = newGate2Index,
                    gateQuestion2AnswerWeight = newGate2Weight,
                    currentStep = SbtiStep.FORMAL_QUESTIONS
                )
            }
        }
    }

// 回答正式问题
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
            val nextIndex = minOf(state.currentQuestionIndex + 1, state.formalQuestions.size - 1)
            state.copy(currentQuestionIndex = nextIndex)
        }
    }

// 是否是最后一题
    fun isLastQuestion(): Boolean {
        val state = _uiState.value
        return state.currentQuestionIndex == state.formalQuestions.size - 1
    }

    // 是否是第一题
    fun isFirstQuestion(): Boolean {
        return _uiState.value.currentQuestionIndex == 0
    }

// 上一题
    fun previousQuestion() {
        _uiState.update { state ->
            val prevIndex = maxOf(state.currentQuestionIndex - 1, 0)
            state.copy(currentQuestionIndex = prevIndex)
        }
    }

// 完成测试，计算结果
    fun completeTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }

            val state = _uiState.value
            val result = calculatePersonalityResult(
                answers = state.answers,
                gateQuestion1Answer = state.gateQuestion1AnswerIndex,
                gateQuestion1Weight = state.gateQuestion1AnswerWeight,
                gateQuestion2Answer = state.gateQuestion2AnswerIndex,
                gateQuestion2Weight = state.gateQuestion2AnswerWeight
            )

            _uiState.update {
                it.copy(
                    currentStep = SbtiStep.RESULT,
                    result = result.first,
                    similarity = result.second,
                    isComplete = true,
                    isCalculating = false
                )
            }
        }
    }

// 重置测试
    fun reset() {
        _uiState.value = UiState(
            gateQuestions = getSbtiGateQuestions(),
            formalQuestions = getSbtiQuestions()
        )
    }

    // 退出测试到发现页
    fun exitTest() {
        reset()
    }

}
