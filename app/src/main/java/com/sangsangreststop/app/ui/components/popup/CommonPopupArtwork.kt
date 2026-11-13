package com.sangsangreststop.app.ui.components.popup

import androidx.annotation.DrawableRes
import com.sangsangreststop.app.R

internal data class CommonPopupArtwork(
    @DrawableRes val cloudRes: Int,
    @DrawableRes val charactersRes: Int,
    @DrawableRes val decorationRes: Int,
    @DrawableRes val secondaryDecorationRes: Int? = null,
    @DrawableRes val mainIconRes: Int? = null,
    val cloudAspectRatio: Float,
)

internal data class CommonPopupButtonArtwork(
    @DrawableRes val backgroundRes: Int,
    @DrawableRes val iconRes: Int,
)

internal fun commonPopupArtwork(model: CommonPopupUiModel): CommonPopupArtwork =
    when (model.type) {
        CommonPopupType.PHONE -> standardArtwork(R.drawable.sr_common_icon_phone)
        CommonPopupType.NAVIGATION -> standardArtwork(R.drawable.sr_common_icon_navigation)
        CommonPopupType.ERROR -> standardArtwork(
            iconRes = when (model.errorType) {
                CommonPopupErrorType.NETWORK -> R.drawable.sr_common_icon_network
                CommonPopupErrorType.SPEECH_RECOGNITION,
                null,
                -> R.drawable.sr_common_icon_microphone
            },
        )

        CommonPopupType.GAME_END -> CommonPopupArtwork(
            cloudRes = R.drawable.sr_common_end_cloud,
            charactersRes = R.drawable.sr_common_end_characters,
            decorationRes = R.drawable.sr_common_end_stars,
            mainIconRes = R.drawable.sr_common_end_crown,
            cloudAspectRatio = 16f / 9f,
        )

        CommonPopupType.GAME_PAUSE -> CommonPopupArtwork(
            cloudRes = R.drawable.sr_common_pause_cloud,
            charactersRes = R.drawable.sr_common_pause_characters,
            decorationRes = R.drawable.sr_common_pause_stars,
            secondaryDecorationRes = R.drawable.sr_common_pause_zzz,
            cloudAspectRatio = 16f / 9f,
        )
    }

private fun standardArtwork(@DrawableRes iconRes: Int) = CommonPopupArtwork(
    cloudRes = R.drawable.sr_common_popup_cloud,
    charactersRes = R.drawable.sr_common_popup_characters,
    decorationRes = R.drawable.sr_common_popup_stars,
    secondaryDecorationRes = R.drawable.sr_common_popup_small_cloud,
    mainIconRes = iconRes,
    cloudAspectRatio = 4f / 3f,
)

internal fun commonPopupButtonArtwork(
    popupType: CommonPopupType,
    button: CommonPopupButtonModel,
): CommonPopupButtonArtwork {
    val background = when (popupType) {
        CommonPopupType.GAME_END -> when (button.tone) {
            CommonPopupButtonTone.PINK -> R.drawable.sr_common_button_pink
            CommonPopupButtonTone.BLUE -> R.drawable.sr_common_button_blue
            CommonPopupButtonTone.GRAY -> R.drawable.sr_common_end_button_gray
            CommonPopupButtonTone.MINT -> R.drawable.sr_common_popup_button_mint
            CommonPopupButtonTone.PURPLE -> R.drawable.sr_common_pause_button_purple
        }

        CommonPopupType.GAME_PAUSE -> when (button.tone) {
            CommonPopupButtonTone.PINK -> R.drawable.sr_common_button_pink
            CommonPopupButtonTone.BLUE -> R.drawable.sr_common_button_blue
            CommonPopupButtonTone.PURPLE -> R.drawable.sr_common_pause_button_purple
            CommonPopupButtonTone.MINT -> R.drawable.sr_common_popup_button_mint
            CommonPopupButtonTone.GRAY -> R.drawable.sr_common_end_button_gray
        }

        else -> when (button.tone) {
            CommonPopupButtonTone.MINT -> R.drawable.sr_common_popup_button_mint
            CommonPopupButtonTone.BLUE -> R.drawable.sr_common_popup_button_blue
            CommonPopupButtonTone.PINK -> R.drawable.sr_common_popup_button_pink
            CommonPopupButtonTone.PURPLE -> R.drawable.sr_common_pause_button_purple
            CommonPopupButtonTone.GRAY -> R.drawable.sr_common_end_button_gray
        }
    }

    val icon = when (button.action) {
        CommonPopupAction.CALL -> R.drawable.sr_common_icon_phone
        CommonPopupAction.CONFIRM -> R.drawable.sr_common_icon_navigation
        CommonPopupAction.RETRY -> R.drawable.sr_common_icon_retry
        CommonPopupAction.HOME -> R.drawable.sr_common_icon_home
        CommonPopupAction.STOP -> if (popupType == CommonPopupType.GAME_END) {
            R.drawable.sr_common_end_icon_close
        } else {
            R.drawable.sr_common_icon_close
        }

        CommonPopupAction.RESUME -> R.drawable.sr_common_pause_icon_clock
        CommonPopupAction.VOICE_SETTINGS -> R.drawable.sr_common_pause_icon_voice
        CommonPopupAction.REPLAY -> R.drawable.sr_common_end_icon_replay
    }

    return CommonPopupButtonArtwork(
        backgroundRes = background,
        iconRes = icon,
    )
}
