package com.sangsangreststop.app.ui.components.popup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sangsangreststop.app.R

enum class CommonPopupType {
    PHONE,
    NAVIGATION,
    ERROR,
    GAME_END,
    GAME_PAUSE,
}

enum class CommonPopupErrorType {
    SPEECH_RECOGNITION,
    NETWORK,
}

enum class CommonPopupAction {
    CALL,
    CONFIRM,
    RETRY,
    HOME,
    STOP,
    RESUME,
    VOICE_SETTINGS,
    REPLAY,
}

enum class CommonPopupButtonTone {
    MINT,
    BLUE,
    PINK,
    PURPLE,
    GRAY,
}

data class CommonPopupButtonModel(
    val action: CommonPopupAction,
    val label: String,
    val supportingText: String? = null,
    val tone: CommonPopupButtonTone,
)

data class CommonPopupUiModel(
    val type: CommonPopupType,
    val titleStart: String = "",
    val titleAccent: String? = null,
    val titleEnd: String = "",
    val description: String,
    val supportingText: String? = null,
    val reassurance: String? = null,
    val errorType: CommonPopupErrorType? = null,
    val actions: List<CommonPopupButtonModel>,
)

@Composable
fun commonPopupModel(
    type: CommonPopupType,
    errorType: CommonPopupErrorType = CommonPopupErrorType.SPEECH_RECOGNITION,
): CommonPopupUiModel = when (type) {
    CommonPopupType.PHONE -> CommonPopupUiModel(
        type = type,
        titleAccent = stringResource(R.string.common_popup_phone_title_accent),
        titleEnd = stringResource(R.string.common_popup_phone_title_end),
        description = stringResource(R.string.common_popup_phone_description),
        supportingText = stringResource(R.string.common_popup_phone_supporting),
        reassurance = stringResource(R.string.common_popup_resume_reassurance),
        actions = listOf(
            CommonPopupButtonModel(
                action = CommonPopupAction.CALL,
                label = stringResource(R.string.common_popup_phone_call),
                tone = CommonPopupButtonTone.MINT,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.HOME,
                label = stringResource(R.string.common_popup_home),
                tone = CommonPopupButtonTone.BLUE,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.STOP,
                label = stringResource(R.string.common_popup_stop),
                tone = CommonPopupButtonTone.PINK,
            ),
        ),
    )

    CommonPopupType.NAVIGATION -> CommonPopupUiModel(
        type = type,
        titleAccent = stringResource(R.string.common_popup_navigation_title_accent),
        titleEnd = stringResource(R.string.common_popup_navigation_title_end),
        description = stringResource(R.string.common_popup_navigation_description),
        supportingText = stringResource(R.string.common_popup_navigation_supporting),
        reassurance = stringResource(R.string.common_popup_resume_reassurance),
        actions = listOf(
            CommonPopupButtonModel(
                action = CommonPopupAction.CONFIRM,
                label = stringResource(R.string.common_popup_confirm),
                tone = CommonPopupButtonTone.MINT,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.HOME,
                label = stringResource(R.string.common_popup_home),
                tone = CommonPopupButtonTone.BLUE,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.STOP,
                label = stringResource(R.string.common_popup_stop),
                tone = CommonPopupButtonTone.PINK,
            ),
        ),
    )

    CommonPopupType.ERROR -> CommonPopupUiModel(
        type = type,
        titleAccent = stringResource(R.string.common_popup_error_title_accent),
        titleEnd = stringResource(R.string.common_popup_error_title_end),
        description = when (errorType) {
            CommonPopupErrorType.SPEECH_RECOGNITION ->
                stringResource(R.string.common_popup_error_speech_description)

            CommonPopupErrorType.NETWORK ->
                stringResource(R.string.common_popup_error_network_description)
        },
        supportingText = when (errorType) {
            CommonPopupErrorType.SPEECH_RECOGNITION ->
                stringResource(R.string.common_popup_error_speech_supporting)

            CommonPopupErrorType.NETWORK ->
                stringResource(R.string.common_popup_error_network_supporting)
        },
        reassurance = stringResource(R.string.common_popup_error_reassurance),
        errorType = errorType,
        actions = listOf(
            CommonPopupButtonModel(
                action = CommonPopupAction.RETRY,
                label = stringResource(R.string.common_popup_retry),
                tone = CommonPopupButtonTone.MINT,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.HOME,
                label = stringResource(R.string.common_popup_home),
                tone = CommonPopupButtonTone.BLUE,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.STOP,
                label = stringResource(R.string.common_popup_stop),
                tone = CommonPopupButtonTone.PINK,
            ),
        ),
    )

    CommonPopupType.GAME_END -> CommonPopupUiModel(
        type = type,
        titleStart = stringResource(R.string.common_popup_end_title_start),
        titleAccent = stringResource(R.string.common_popup_end_title_accent),
        titleEnd = stringResource(R.string.common_popup_end_title_end),
        description = stringResource(R.string.common_popup_end_description),
        actions = listOf(
            CommonPopupButtonModel(
                action = CommonPopupAction.REPLAY,
                label = stringResource(R.string.common_popup_replay),
                supportingText = stringResource(R.string.common_popup_replay_supporting),
                tone = CommonPopupButtonTone.PINK,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.HOME,
                label = stringResource(R.string.common_popup_home),
                supportingText = stringResource(R.string.common_popup_home_supporting),
                tone = CommonPopupButtonTone.BLUE,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.STOP,
                label = stringResource(R.string.common_popup_stop),
                supportingText = stringResource(R.string.common_popup_stop_supporting),
                tone = CommonPopupButtonTone.GRAY,
            ),
        ),
    )

    CommonPopupType.GAME_PAUSE -> CommonPopupUiModel(
        type = type,
        titleEnd = stringResource(R.string.common_popup_pause_title),
        description = stringResource(R.string.common_popup_pause_description),
        actions = listOf(
            CommonPopupButtonModel(
                action = CommonPopupAction.RESUME,
                label = stringResource(R.string.common_popup_resume),
                supportingText = stringResource(R.string.common_popup_resume_supporting),
                tone = CommonPopupButtonTone.BLUE,
            ),
            CommonPopupButtonModel(
                action = CommonPopupAction.STOP,
                label = stringResource(R.string.common_popup_end_game),
                supportingText = stringResource(R.string.common_popup_end_game_supporting),
                tone = CommonPopupButtonTone.PINK,
            ),
        ),
    )
}
