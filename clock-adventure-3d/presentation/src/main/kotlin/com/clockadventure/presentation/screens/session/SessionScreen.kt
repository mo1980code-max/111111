package com.clockadventure.presentation.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.domain.engine.TimeFormatter
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.GateQuestion
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.MascotId
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.QuestionKind
import com.clockadventure.domain.model.SessionResult
import com.clockadventure.presentation.R
import com.clockadventure.presentation.clock.InteractiveClock
import com.clockadventure.presentation.clock.MiniClock
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.AnswerButton
import com.clockadventure.presentation.components.AnswerState
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.ArrowBackIcon
import com.clockadventure.presentation.components.CoinBadge
import com.clockadventure.presentation.components.ConfettiOverlay
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.MascotBubble
import com.clockadventure.presentation.components.MascotMood
import com.clockadventure.presentation.components.MascotView
import com.clockadventure.presentation.components.Pill
import com.clockadventure.presentation.components.ProgressBar
import com.clockadventure.presentation.components.RoundIconButton
import com.clockadventure.presentation.components.StepDots
import com.clockadventure.presentation.session.FeedbackKind
import com.clockadventure.presentation.session.MatchState
import com.clockadventure.presentation.session.SessionPhase
import com.clockadventure.presentation.session.SessionUiState
import com.clockadventure.presentation.session.SessionViewModel
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus

/** The clock never shrinks below this, whatever the window shape - it must stay readable and draggable. */
private val MIN_CLOCK_SIZE = 160.dp

/**
 * Sizes the interactive clock from *both* dimensions of the window instead of only its width.
 *
 * The two [BoxWithConstraints] below already cap the clock against [maxWidth] so it never outgrows
 * a phone turned sideways or a tablet's wide column - but width alone is not enough: a landscape
 * phone, a tablet split 50/50 with another app, or a small free-form multi-window all have plenty
 * of width and very little *height*, and the prompt card, hints and buttons around the clock still
 * need to fit above and below it without the phase collapsing into an awkward scroll. This mirrors
 * the width fraction against a fraction of [Configuration.screenHeightDp][android.content.res.Configuration.screenHeightDp]
 * from [LocalConfiguration] - which Compose already recomposes on every rotation, fold and
 * multi-window resize - and keeps whichever fraction is smaller.
 */
@Composable
private fun heightAwareClockSize(
    maxWidth: Dp,
    widthFraction: Float,
    heightFraction: Float,
    cap: Dp
): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return minOf(maxWidth * widthFraction, screenHeight * heightFraction, cap).coerceAtLeast(MIN_CLOCK_SIZE)
}

/** Main exercise screen: the ten lessons, the six mini games and the four challenges all run here. */
@Composable
fun SessionRoute(
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { viewModel.onQuit() } }

    SessionScreen(
        state = state,
        onBack = onBack,
        onStartQuestions = viewModel::startQuestions,
        onChoiceSelected = viewModel::onChoiceSelected,
        onClockChanged = viewModel::onClockChanged,
        onCheckClock = viewModel::onCheckClock,
        onRetry = viewModel::onRetry,
        onShowAnswer = viewModel::onShowAnswer,
        onContinue = viewModel::onContinue,
        onReplay = viewModel::onReplay,
        onNextLevel = onNextLevel,
        onHome = onHome,
        onMatchClockSelected = viewModel::onMatchClockSelected,
        onMatchTimeSelected = viewModel::onMatchTimeSelected,
        digitalText = viewModel::digitalText,
        hintText = viewModel.hintText(),
        onTakeBreak = viewModel::onTakeBreak,
        onAskGrownUp = viewModel::onAskGrownUp,
        onGateAnswer = viewModel::onGateAnswer,
        onCancelGate = viewModel::onCancelGate,
        modifier = modifier
    )
}

@Composable
internal fun SessionScreen(
    state: SessionUiState,
    onBack: () -> Unit,
    onStartQuestions: () -> Unit,
    onChoiceSelected: (String) -> Unit,
    onClockChanged: (ClockTime) -> Unit,
    onCheckClock: () -> Unit,
    onRetry: () -> Unit,
    onShowAnswer: () -> Unit,
    onContinue: () -> Unit,
    onReplay: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onHome: () -> Unit,
    onMatchClockSelected: (String) -> Unit,
    onMatchTimeSelected: (String) -> Unit,
    digitalText: (ClockTime) -> String,
    hintText: LocalizedText,
    onTakeBreak: () -> Unit,
    onAskGrownUp: () -> Unit,
    onGateAnswer: (Int) -> Unit,
    onCancelGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = state.settings
    val lang = settings.language
    val mood = when {
        state.phase == SessionPhase.RESULT && (state.result?.stars ?: 0) >= 2 -> MascotMood.EXCITED
        state.feedbackKind == FeedbackKind.CORRECT -> MascotMood.EXCITED
        state.feedbackKind == FeedbackKind.WRONG || state.feedbackKind == FeedbackKind.TIME_UP -> MascotMood.THINKING
        state.phase == SessionPhase.TEACH -> MascotMood.HAPPY
        else -> MascotMood.HAPPY
    }
    val bubble = when {
        state.phase == SessionPhase.RESULT -> stringResource(R.string.mascot_cheer)
        state.feedbackKind == FeedbackKind.CORRECT -> stringResource(R.string.mascot_cheer)
        state.feedbackKind == FeedbackKind.WRONG || state.feedbackKind == FeedbackKind.TIME_UP ->
            stringResource(R.string.mascot_think)
        state.phase == SessionPhase.TEACH -> stringResource(R.string.mascot_idle)
        else -> stringResource(R.string.mascot_idle)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SessionTopBar(state = state, onBack = onBack)

                when (state.phase) {
                    SessionPhase.TEACH -> TeachPhase(state = state, onStart = onStartQuestions, digitalText = digitalText)
                    SessionPhase.QUESTION ->
                        if (state.matchState != null) {
                            MatchPhase(
                                state = state,
                                matchState = state.matchState,
                                onClockSelected = onMatchClockSelected,
                                onTimeSelected = onMatchTimeSelected
                            )
                        } else {
                            QuestionPhase(
                                state = state,
                                digitalText = digitalText,
                                hintText = hintText,
                                onChoiceSelected = onChoiceSelected,
                                onClockChanged = onClockChanged,
                                onCheckClock = onCheckClock,
                                onRetry = onRetry,
                                onShowAnswer = onShowAnswer,
                                onContinue = onContinue
                            )
                        }
                    SessionPhase.RESULT -> ResultPhase(
                        state = state,
                        onReplay = onReplay,
                        onNextLevel = onNextLevel,
                        onHome = onHome
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.gapMedium),
                    verticalAlignment = Alignment.Bottom
                ) {
                    MascotView(mascot = settings.character, mood = mood, sizeDp = 100.dp)
                    Spacer(modifier = Modifier.width(Dimens.gapSmall))
                    Box(modifier = Modifier.weight(1f).padding(bottom = 12.dp)) {
                        MascotBubble(text = bubble)
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.gapMedium))
            }
        }

        if (state.phase == SessionPhase.RESULT && (state.result?.stars ?: 0) >= 1) {
            ConfettiOverlay(visible = true, modifier = Modifier.fillMaxSize())
        }

        if (state.breakTime) {
            BreakTimeDialog(
                gateQuestion = state.gateQuestion,
                minutes = state.settings.dailyLimitMinutes + state.grantedExtraMinutes,
                onTakeBreak = onTakeBreak,
                onAskGrownUp = onAskGrownUp,
                onGateAnswer = onGateAnswer,
                onCancelGate = onCancelGate
            )
        }
    }
}

// ------------------------------------------------------------------ top bar

@Composable
private fun SessionTopBar(state: SessionUiState, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.gapMedium, vertical = Dimens.gapSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(onClick = onBack, size = 46.dp) {
                ArrowBackIcon(tint = appColors.accent)
            }
            Spacer(modifier = Modifier.width(Dimens.gapSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title[state.settings.language],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (state.isEndless) {
                        "${state.correct} ✓"
                    } else {
                        stringResource(
                            R.string.lesson_question_of,
                            state.questionNumber.coerceAtLeast(1),
                            state.totalQuestions.coerceAtLeast(1)
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            state.remainingMs?.let { remaining ->
                val seconds = (remaining / 1000L).toInt().coerceAtLeast(0)
                Pill(
                    text = stringResource(R.string.game_time_left, seconds),
                    containerColor = if (seconds <= 5) Palette.Coral else appColors.accent
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapSmall))
        if (state.totalQuestions > 0 && state.totalQuestions <= 12) {
            StepDots(total = state.totalQuestions, current = state.questionNumber - 1)
        } else if (state.totalQuestions > 0) {
            ProgressBar(progress = state.progressFraction)
        }
    }
}

// ------------------------------------------------------------------ teach

@Composable
private fun TeachPhase(
    state: SessionUiState,
    onStart: () -> Unit,
    digitalText: (ClockTime) -> String
) {
    var time by remember { mutableStateOf(ClockTime.of12(3, 0, pm = false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.gapMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.lesson_teach_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Text(
                    text = state.title[state.settings.language],
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.accent,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Text(
                    text = state.teach?.get(state.settings.language).orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val clockSize: Dp = heightAwareClockSize(
                maxWidth = maxWidth,
                widthFraction = 0.8f,
                heightFraction = 0.46f,
                cap = 320.dp
            )
            InteractiveClock(
                time = time,
                style = state.settings.clockStyle,
                interactive = true,
                snapMinutes = 1,
                showDigital = true,
                use24Hour = state.settings.use24Hour,
                language = state.settings.language,
                hintHand = ClockHand.HOUR,
                reduceMotion = state.settings.reduceMotion,
                contentDescription = stringResource(
                    R.string.cd_clock_time,
                    TimeFormatter.spoken(time, state.settings.language)
                ),
                onTimeChanged = { time = it },
                modifier = Modifier.size(clockSize)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.gapSmall))
        Text(
            text = stringResource(R.string.clock_drag_both),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
        ArcadeButton(
            text = stringResource(R.string.lesson_start),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
    }
}

// ------------------------------------------------------------------ question

@Composable
private fun QuestionPhase(
    state: SessionUiState,
    digitalText: (ClockTime) -> String,
    hintText: LocalizedText,
    onChoiceSelected: (String) -> Unit,
    onClockChanged: (ClockTime) -> Unit,
    onCheckClock: () -> Unit,
    onRetry: () -> Unit,
    onShowAnswer: () -> Unit,
    onContinue: () -> Unit
) {
    val question = state.question ?: return
    val settings = state.settings
    val lang = settings.language
    val isSetClock = question.kind == QuestionKind.SET_CLOCK || question.kind == QuestionKind.ROUTINE_SET_CLOCK
    val finished = state.feedbackKind == FeedbackKind.CORRECT
    val canDrag = isSetClock && !finished
    val shownTime = if (isSetClock) state.clockTime else question.targetTime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.gapMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = question.prompt[lang],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (question.routine != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.game_routine_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                if (isSetClock) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.game_set_clock_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }
                if (state.hintsOn && !finished) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hintText[lang],
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.accent,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapSmall))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val clockSize: Dp = heightAwareClockSize(
                maxWidth = maxWidth,
                widthFraction = 0.86f,
                heightFraction = 0.4f,
                cap = 320.dp
            )
            InteractiveClock(
                time = shownTime,
                style = settings.clockStyle,
                interactive = canDrag,
                snapMinutes = if (isSetClock) state.snapMinutes else 5,
                showDigital = (isSetClock || state.settings.difficultyMode == com.clockadventure.domain.model.DifficultyMode.EASY) && !finished,
                use24Hour = settings.use24Hour,
                language = settings.language,
                hintHand = hintHandFor(state, question),
                ghostTime = if (state.revealAnswer) question.targetTime else null,
                successPulse = finished,
                reduceMotion = settings.reduceMotion,
                contentDescription = stringResource(
                    R.string.cd_clock_time,
                    TimeFormatter.spoken(shownTime, settings.language)
                ),
                onTimeChanged = onClockChanged,
                modifier = Modifier.size(clockSize)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.gapSmall))

        when {
            isSetClock && !finished -> {
                ArcadeButton(
                    text = stringResource(R.string.lesson_check),
                    onClick = onCheckClock,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
            question.choices.size == 2 -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                    question.choices.forEach { choice ->
                        AnswerButton(
                            text = choiceLabel(choice, state),
                            onClick = { onChoiceSelected(choice.id) },
                            state = choiceState(state, choice.id),
                            enabled = !finished,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            question.choices.size >= 3 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.choices.forEach { choice ->
                        AnswerButton(
                            text = choiceLabel(choice, state),
                            onClick = { onChoiceSelected(choice.id) },
                            state = choiceState(state, choice.id),
                            enabled = !finished,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.gapSmall))

        AnimatedVisibility(
            visible = state.feedback != null,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = fadeOut()
        ) {
            val result = state.feedback
            if (result != null) {
                FeedbackCard(
                    correct = state.feedbackKind == FeedbackKind.CORRECT,
                    message = result.message[lang],
                    expectedText = digitalText(result.expected),
                    revealed = state.revealAnswer,
                    onRetry = onRetry,
                    onShowAnswer = onShowAnswer,
                    onContinue = onContinue
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
    }
}

private fun choiceLabel(choice: com.clockadventure.domain.model.AnswerChoice, state: SessionUiState): String {
    val lang = state.settings.language
    return when {
        choice.text != null -> choice.text!![lang]
        choice.number != null -> choice.number.toString()
        choice.time != null -> TimeFormatter.digital(choice.time!!, state.settings.use24Hour, lang)
        else -> ""
    }
}

private fun choiceState(state: SessionUiState, id: String): AnswerState {
    val question = state.question ?: return AnswerState.IDLE
    if (state.feedbackKind == FeedbackKind.CORRECT && id == question.correctChoiceId) return AnswerState.CORRECT
    if (state.feedbackKind == FeedbackKind.WRONG && id == state.selectedChoiceId) return AnswerState.WRONG
    return AnswerState.IDLE
}

private fun hintHandFor(state: SessionUiState, question: Question): ClockHand? {
    val helping = state.feedbackKind == FeedbackKind.WRONG || state.revealAnswer || state.hintsOn
    if (!helping) return null
    return when (question.tapHand) {
        ClockHand.HOUR -> ClockHand.HOUR
        ClockHand.MINUTE -> ClockHand.MINUTE
        else -> if (state.revealAnswer || state.feedbackKind == FeedbackKind.WRONG) ClockHand.MINUTE else null
    }
}

@Composable
private fun FeedbackCard(
    correct: Boolean,
    message: String,
    expectedText: String,
    revealed: Boolean,
    onRetry: () -> Unit,
    onShowAnswer: () -> Unit,
    onContinue: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        topColor = if (correct) Palette.Mint.copy(alpha = 0.85f) else Color(0xFFFFF0D6),
        bottomColor = if (correct) Color(0xFFD9F7E9) else Color(0xFFFFE3C2),
        cornerRadius = Dimens.cornerMedium
    ) {
        Column(modifier = Modifier.padding(Dimens.gapMedium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (correct) "⭐ " + stringResource(R.string.result_title) else "💡 " + stringResource(R.string.lesson_hint),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (!correct) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = expectedText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = appColors.accentDark,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            if (correct) {
                ArcadeButton(
                    text = stringResource(R.string.lesson_continue),
                    onClick = onContinue,
                    topColor = Palette.Mint,
                    bottomColor = Color(0xFF1E9E6E),
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcadeButton(
                        text = stringResource(R.string.lesson_try_again),
                        onClick = onRetry,
                        topColor = Palette.SunYellow,
                        bottomColor = Palette.Orange,
                        modifier = Modifier.weight(1f)
                    )
                    ArcadeButton(
                        text = stringResource(R.string.lesson_show_answer),
                        onClick = onShowAnswer,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (revealed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ArcadeButton(
                        text = stringResource(R.string.lesson_continue),
                        onClick = onContinue,
                        topColor = Palette.Mint,
                        bottomColor = Color(0xFF1E9E6E),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ match game

@Composable
private fun MatchPhase(
    state: SessionUiState,
    matchState: MatchState,
    onClockSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val settings = state.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.gapMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.game_match_hint),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Pill(
            text = stringResource(R.string.game_matched_pairs, matchState.matchedCount, matchState.totalCount),
            containerColor = appColors.secondary,
            contentColor = Palette.Ink
        )
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                matchState.clocks.forEach { pair ->
                    val matched = pair.id in matchState.matched
                    val selected = matchState.selectedClock == pair.id
                    MatchClockTile(
                        time = pair.time,
                        settings = settings,
                        matched = matched,
                        selected = selected,
                        onClick = { onClockSelected(pair.id) },
                        modifier = Modifier.size(92.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                matchState.times.forEach { pair ->
                    val matched = pair.id in matchState.matched
                    AnswerButton(
                        text = TimeFormatter.digital(pair.time, settings.use24Hour, settings.language),
                        onClick = { onTimeSelected(pair.id) },
                        state = if (matched) AnswerState.CORRECT else AnswerState.IDLE,
                        enabled = !matched,
                        modifier = Modifier
                            .height(92.dp)
                            .width(92.dp + 30.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
    }
}

@Composable
private fun MatchClockTile(
    time: ClockTime,
    settings: AppSettings,
    matched: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (matched) Palette.Mint else if (selected) appColors.accent else Color.Transparent
    RoundIconButton(onClick = onClick, size = 92.dp, containerColor = Color.White) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            MiniClock(
                time = time,
                style = settings.clockStyle,
                size = 92.dp - 14.dp,
                showNumbers = true
            )
        }
    }
    if (tint != Color.Transparent) {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

// ------------------------------------------------------------------ result

/**
 * The daily limit was reached. Nothing is taken away from the child - the session simply pauses
 * behind this dialog, and a grown-up can grant fifteen more minutes by solving a little sum.
 */
@Composable
private fun BreakTimeDialog(
    gateQuestion: GateQuestion?,
    minutes: Int,
    onTakeBreak: () -> Unit,
    onAskGrownUp: () -> Unit,
    onGateAnswer: (Int) -> Unit,
    onCancelGate: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Dimens.gapLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.limit_break_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Text(
                    text = stringResource(R.string.limit_break_body, minutes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.gapMedium))

                if (gateQuestion == null) {
                    ArcadeButton(
                        text = stringResource(R.string.limit_break_ask),
                        onClick = onAskGrownUp,
                        topColor = Palette.SunYellow,
                        bottomColor = Palette.Orange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    ArcadeButton(
                        text = stringResource(R.string.limit_break_done),
                        onClick = onTakeBreak,
                        topColor = Palette.Mint,
                        bottomColor = Color(0xFF1E9E6E),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.limit_gate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    Text(
                        text = gateQuestion.text(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                        gateQuestion.options.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                            ) {
                                row.forEach { option ->
                                    ArcadeButton(
                                        text = option.toString(),
                                        onClick = { onGateAnswer(option) },
                                        topColor = Palette.Ocean,
                                        bottomColor = Palette.DeepBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    ArcadeButton(
                        text = stringResource(R.string.limit_gate_cancel),
                        onClick = onCancelGate,
                        height = 52.dp,
                        topColor = Palette.Silver,
                        bottomColor = Palette.InkSoft,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultPhase(
    state: SessionUiState,
    onReplay: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onHome: () -> Unit
) {
    val result: SessionResult? = state.result
    val lang = state.settings.language
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.gapMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (result == null) {
            Text(text = stringResource(R.string.loading), style = MaterialTheme.typography.bodyLarge)
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.gapLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = result.title[lang],
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    Text(
                        text = stringResource(R.string.common_stars_of, result.stars),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapSmall))
                    Text(
                        text = result.message[lang],
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ResultStat(value = result.coinsEarned.toString(), label = stringResource(R.string.hud_coins))
                        ResultStat(value = result.xpEarned.toString(), label = stringResource(R.string.hud_xp))
                        ResultStat(value = "${(result.accuracy * 100f).toInt()}%", label = stringResource(R.string.progress_accuracy))
                    }
                    if (result.leveledUp) {
                        Spacer(modifier = Modifier.height(Dimens.gapSmall))
                        Pill(
                            text = stringResource(R.string.result_level_up, result.newLevel),
                            containerColor = Palette.SunYellow,
                            contentColor = Palette.Ink
                        )
                    }
                    if (result.isNewBest) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.result_new_best),
                            style = MaterialTheme.typography.titleSmall,
                            color = appColors.accent
                        )
                    }
                    if (result.newAchievements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Dimens.gapSmall))
                        Text(
                            text = stringResource(R.string.result_new_achievements),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        result.newAchievements.forEach { achievement ->
                            Text(
                                text = achievement.title[lang],
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (result.newUnlocks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Dimens.gapSmall))
                        Text(
                            text = stringResource(R.string.result_new_unlocks),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        result.newUnlocks.forEach { unlock ->
                            Text(
                                text = unlock.name[lang],
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (state.dailyLimitReached) {
                        Spacer(modifier = Modifier.height(Dimens.gapSmall))
                        Text(
                            text = stringResource(R.string.limit_reached),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.Coral,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.gapMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                ArcadeButton(
                    text = stringResource(R.string.result_replay),
                    onClick = onReplay,
                    topColor = Palette.Ocean,
                    bottomColor = Color(0xFF1560C0),
                    modifier = Modifier.weight(1f)
                )
                ArcadeButton(
                    text = stringResource(R.string.result_home),
                    onClick = onHome,
                    modifier = Modifier.weight(1f)
                )
            }
            state.nextLevelId?.let { next ->
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                ArcadeButton(
                    text = stringResource(R.string.result_next_level),
                    onClick = { onNextLevel(next) },
                    topColor = Palette.Mint,
                    bottomColor = Color(0xFF1E9E6E),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.gapMedium))
    }
}

@Composable
private fun ResultStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
