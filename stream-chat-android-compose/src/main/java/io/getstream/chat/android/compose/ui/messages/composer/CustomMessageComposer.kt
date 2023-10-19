package io.getstream.chat.android.compose.ui.messages.composer

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.getstream.chat.android.client.errors.extractCause
import io.getstream.chat.android.compose.R
import io.getstream.chat.android.compose.state.messages.attachments.StatefulStreamMediaRecorder
import io.getstream.chat.android.compose.ui.components.composer.CoolDownIndicator
import io.getstream.chat.android.compose.ui.util.mirrorRtl
import io.getstream.chat.android.models.Attachment
import io.getstream.chat.android.models.ChannelCapabilities
import io.getstream.chat.android.ui.common.state.messages.composer.MessageComposerState
import io.getstream.chat.android.ui.common.state.messages.composer.ValidationError
import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.streamLog
import io.getstream.sdk.chat.audio.recording.MediaRecorderState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CustomComposerIntegrations(
    messageInputState: MessageComposerState,
    onAttachemntsClick: ()->Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    AnimatedVisibility(visible = messageInputState.hasCommands) {
        IconButton(
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.attach),
                    contentDescription = null,
                    tint = Color.White,
                )
            },
            onClick = {
                onAttachemntsClick.invoke()
            },
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CustomMessageComposerTrailingContent(
    value: String,
    coolDownTime: Int,
    attachments: List<Attachment>,
    validationErrors: List<ValidationError>,
    ownCapabilities: Set<String>,
    isInEditMode: Boolean,
    onSendMessage: (String, List<Attachment>) -> Unit,
    onRecordingSaved: (Attachment) -> Unit,
    onSmileyCLick: () -> Unit = {},
    shareLocation: () -> Unit = {},
    statefulStreamMediaRecorder: StatefulStreamMediaRecorder?,
) {
    val isSendButtonEnabled = ownCapabilities.contains(ChannelCapabilities.SEND_MESSAGE)
    val isInputValid by lazy { (value.isNotBlank() || attachments.isNotEmpty()) && validationErrors.isEmpty() }
    val sendButtonDescription = stringResource(id = R.string.stream_compose_cd_send_button)
    val recordAudioButtonDescription = stringResource(id = R.string.stream_compose_cd_record_audio_message)
    var permissionsRequested by rememberSaveable { mutableStateOf(false) }

    val isRecording = statefulStreamMediaRecorder?.mediaRecorderState?.value

    // TODO test permissions on lower APIs etc
    val storageAndRecordingPermissionState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.RECORD_AUDIO,
            )
        } else {
            listOf(
                Manifest.permission.RECORD_AUDIO
            )
        },
    ) {
        // TODO should we track this or always ask?
        permissionsRequested = true
    }

    // TODO test permissions on lower APIs etc
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    ) {
        permissionsRequested = true
    }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    if (coolDownTime > 0 && !isInEditMode) {
        CoolDownIndicator(coolDownTime = coolDownTime)
    } else {

        if(isSendButtonEnabled && isInputValid){
            Image(
                modifier = Modifier.height(48.dp).padding(horizontal = 8.dp).clickable { onSendMessage(value, attachments)},
                painter = painterResource(id = R.drawable.send_message),
                contentDescription = stringResource(id = R.string.stream_compose_send_message)
            )
        } else {

            // Icon(
            //     modifier = Modifier
            //         .size(40.dp)
            //         .padding(8.dp).clickable { onSmileyCLick.invoke() },
            //     painter = painterResource(id = R.drawable.smiley),
            //     contentDescription = stringResource(id = R.string.stream_compose_record_audio_message),
            //     // TODO disable if max attachments are reached
            //     tint = Color.White,
            // )

            Icon(
                modifier = Modifier.clickable {
                    if(!locationPermissionState.allPermissionsGranted) {
                        locationPermissionState.launchMultiplePermissionRequest()
                    } else {
                        shareLocation.invoke()
                    }
                },
                painter = painterResource(id = R.drawable.location_share),
                contentDescription = "location_share",
                tint = Color.White,
            )

            Row(
                modifier = Modifier
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // TODO don't show if the own ability to send attachments isn't given
                if (statefulStreamMediaRecorder != null) {
                    Box(
                        modifier = Modifier
                            .semantics { contentDescription = recordAudioButtonDescription }
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if(!storageAndRecordingPermissionState.allPermissionsGranted) {
                                            storageAndRecordingPermissionState.launchMultiplePermissionRequest()
                                        } else {
                                            coroutineScope.launch {
                                                statefulStreamMediaRecorder.startAudioRecording(
                                                    context = context,
                                                    recordingName = "audio_recording_${Date()}",
                                                )
                                                delay(1000)
                                                statefulStreamMediaRecorder.stopRecording()
                                                    .onSuccess {
                                                        StreamLog.i("MessageComposer") {
                                                            "[onRecordingSaved] attachment: $it"
                                                        }
                                                        onRecordingSaved(it.attachment)
                                                    }
                                                    .onError {
                                                        streamLog(throwable = it.extractCause()) {
                                                            "Could not save audio recording: ${it.message}"
                                                        }
                                                    }
                                            }
                                        }
                                    },
                                    onLongPress = {
                                        /**
                                         * An internal function used to handle audio recording. It initiates the recording
                                         * and stops and saves the file once the appropriate gesture has been completed.
                                         */
                                        fun handleAudioRecording() = coroutineScope.launch {
                                            awaitPointerEventScope {
                                                statefulStreamMediaRecorder.startAudioRecording(
                                                    context = context,
                                                    recordingName = "audio_recording_${Date()}",
                                                )

                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Main)

                                                    if (event.changes.all { it.changedToUp() }) {
                                                        statefulStreamMediaRecorder.stopRecording()
                                                            .onSuccess {
                                                                StreamLog.i("MessageComposer") {
                                                                    "[onRecordingSaved] attachment: $it"
                                                                }
                                                                onRecordingSaved(it.attachment)
                                                            }
                                                            .onError {
                                                                streamLog(throwable = it.extractCause()) {
                                                                    "Could not save audio recording: ${it.message}"
                                                                }
                                                            }
                                                        break
                                                    }
                                                }
                                            }
                                        }

                                        when {
                                            !storageAndRecordingPermissionState.allPermissionsGranted -> {
                                                storageAndRecordingPermissionState.launchMultiplePermissionRequest()
                                            }
                                            isRecording == MediaRecorderState.UNINITIALIZED -> {
                                                handleAudioRecording()
                                            }
                                            else -> streamLog(Priority.ERROR) { "Could not start audio recording" }
                                        }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val layoutDirection = LocalLayoutDirection.current

                        Icon(
                            modifier = Modifier.mirrorRtl(layoutDirection = layoutDirection),
                            painter = painterResource(id = R.drawable.audio_record),
                            contentDescription = stringResource(id = R.string.stream_compose_record_audio_message),
                            // TODO disable if max attachments are reached
                            tint = Color.White,
                        )
                    }
                }


            }
        }




    }

    // TODO release recorder after the composable moves of screen
}