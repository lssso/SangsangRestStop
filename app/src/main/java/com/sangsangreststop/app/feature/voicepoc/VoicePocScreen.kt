package com.sangsangreststop.app.feature.voicepoc

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sangsangreststop.app.R

@Composable
fun VoicePocScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var voiceState by remember { mutableStateOf(VoicePocState()) }
    val controller = remember {
        VoicePocController(context.applicationContext) { newState ->
            voiceState = newState
        }
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasMicrophonePermission = granted
        }

    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }

    val ttsSample = stringResource(R.string.voice_poc_tts_sample)
    val sequenceSample = stringResource(R.string.voice_poc_sequence_sample)

    VoicePocContent(
        state = voiceState,
        hasMicrophonePermission = hasMicrophonePermission,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStartListening = controller::startListening,
        onSpeak = {
            controller.speak(ttsSample)
        },
        onSpeakThenListen = {
            controller.speak(sequenceSample, thenListen = true)
        },
        onStop = controller::stop,
        modifier = modifier,
    )
}

@Composable
private fun VoicePocContent(
    state: VoicePocState,
    hasMicrophonePermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onSpeak: () -> Unit,
    onSpeakThenListen: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBusy =
        when (state.mode) {
            VoicePocMode.Listening,
            VoicePocMode.Processing,
            VoicePocMode.Speaking,
            -> true
            VoicePocMode.Idle,
            VoicePocMode.Error,
            -> false
        }
    val ttsStatus =
        when (state.ttsAvailability) {
            TtsAvailability.Initializing -> stringResource(R.string.voice_poc_initializing)
            TtsAvailability.Available -> stringResource(R.string.voice_poc_available)
            TtsAvailability.Unavailable -> stringResource(R.string.voice_poc_unavailable)
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_poc_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.voice_poc_description),
            style = MaterialTheme.typography.bodyLarge,
        )

        StatusCard(
            rows = listOf(
                stringResource(R.string.voice_poc_microphone_permission) to
                    if (hasMicrophonePermission) {
                        stringResource(R.string.voice_poc_permission_granted)
                    } else {
                        stringResource(R.string.voice_poc_permission_denied)
                    },
                stringResource(R.string.voice_poc_stt_support) to
                    if (state.recognitionAvailable) {
                        stringResource(R.string.voice_poc_available)
                    } else {
                        stringResource(R.string.voice_poc_unavailable)
                    },
                stringResource(R.string.voice_poc_tts_support) to ttsStatus,
                stringResource(R.string.voice_poc_current_state) to state.mode.label,
            ),
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Button(
            onClick = onRequestPermission,
            enabled = !hasMicrophonePermission && !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.voice_poc_request_permission))
        }

        Button(
            onClick = onStartListening,
            enabled = hasMicrophonePermission && state.recognitionAvailable && !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.voice_poc_start_stt))
        }

        Button(
            onClick = onSpeak,
            enabled = (state.ttsAvailability == TtsAvailability.Available) && !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.voice_poc_start_tts))
        }

        Button(
            onClick = onSpeakThenListen,
            enabled =
                hasMicrophonePermission &&
                    state.recognitionAvailable &&
                    (state.ttsAvailability == TtsAvailability.Available) &&
                    !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.voice_poc_tts_then_stt))
        }

        OutlinedButton(
            onClick = onStop,
            enabled = isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.voice_poc_stop))
        }

        ResultCard(state = state)
    }
}

@Composable
private fun StatusCard(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = label)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    state: VoicePocState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.voice_poc_result),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.recognizedText.ifBlank {
                    stringResource(R.string.voice_poc_no_result)
                },
            )
            Text(
                text = stringResource(R.string.voice_poc_events),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.events.isEmpty()) {
                Text(stringResource(R.string.voice_poc_no_events))
            } else {
                state.events.forEach { event ->
                    Text(
                        text = event,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
