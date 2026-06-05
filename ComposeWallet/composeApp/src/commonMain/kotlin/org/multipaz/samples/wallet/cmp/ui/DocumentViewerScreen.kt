package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.bluetooth_required
import mpzcmpwallet.composeapp.generated.resources.bluetooth_settings_message
import mpzcmpwallet.composeapp.generated.resources.hold_to_reader
import mpzcmpwallet.composeapp.generated.resources.more_options
import mpzcmpwallet.composeapp.generated.resources.ok
import mpzcmpwallet.composeapp.generated.resources.present
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.claim.Claim
import org.multipaz.claim.MdocClaim
import org.multipaz.claim.JsonClaim
import org.multipaz.compose.decodeImage
import org.multipaz.util.fromBase64Url
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.document.Document
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.presentment.model.PresentmentSource
import org.multipaz.prompt.PromptModel
import org.multipaz.util.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: String,
    documentModel: DocumentModel,
    promptModel: PromptModel,
    presentmentSource: PresentmentSource,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()
    var pendingQrRequest by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showBleInfoDialog by remember { mutableStateOf(false) }
    var firstAttemptToEnableBle by remember { mutableStateOf(false) }
    val documentInfo = documentModel.documentInfos.collectAsState().value.find {
        it.document.identifier == documentId
    }

    LaunchedEffect(
        pendingQrRequest,
        blePermissionState.isGranted,
        bleEnabledState.isEnabled
    ) {
        Logger.d("permission", "${blePermissionState.isGranted}")
        Logger.d("permissionEnables", "${bleEnabledState.isEnabled}")
        if (pendingQrRequest && blePermissionState.isGranted && bleEnabledState.isEnabled) {
            showQrDialog = true
            pendingQrRequest = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (documentInfo?.canPresentWithProximity() == true) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    when {
                                        !blePermissionState.isGranted -> {
                                            if (firstAttemptToEnableBle) {
                                                showBleInfoDialog = true
                                                if (pendingQrRequest) {
                                                    pendingQrRequest = false
                                                }
                                            } else {
                                                pendingQrRequest = true
                                                blePermissionState.launchPermissionRequest()
                                                firstAttemptToEnableBle = true
                                            }

                                        }

                                        !bleEnabledState.isEnabled -> {
                                            pendingQrRequest = true
                                            bleEnabledState.enable()
                                            firstAttemptToEnableBle = true
                                        }

                                        else -> {
                                            showQrDialog = true
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.QrCode,
                                contentDescription = stringResource(Res.string.present)
                            )
                        }
                    }
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(Res.string.more_options)
                        )
                    }
                }
            )
        }
    ) { padding ->
        documentInfo?.let { info ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                DocumentCard(info, documentTypeRepository= documentTypeRepository)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Contactless,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.hold_to_reader),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }


    }

    if (showQrDialog) {
        QrPresentmentDialog(
            presentmentSource = presentmentSource,
            promptModel = promptModel,
            onDismiss = {
                showQrDialog = false
            }
        )
    }
    if (showBleInfoDialog) {
        BleSettingsDialog(
            onDismiss = { showBleInfoDialog = false }
        )
    }
}

@Composable
fun DocumentCard(
    documentInfo: DocumentInfo,
    documentTypeRepository: DocumentTypeRepository
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = documentInfo.cardArt,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val claims = documentInfo.credentialInfos
            .filter { it.credential.isCertified }
            .flatMap { it.credential.getClaims(documentTypeRepository) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(claims) { claim ->
                if (claim.attribute?.identifier == "portrait") {
                    PortraitClaimRow(claim)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = claim.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = claim.render(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BleSettingsDialog(
    onDismiss: () -> Unit
) {
   AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.bluetooth_required)) },
        text = {
            Text(stringResource(Res.string.bluetooth_settings_message))
        },
        confirmButton = {
            TextButton(
                onClick = {

                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },

    )
}

private fun DocumentInfo.canPresentWithProximity(): Boolean {
    credentialInfos.forEach {
        if (it.credential is MdocCredential) {
            return true
        }
    }
    return false
}
@Composable
fun PortraitClaimRow(claim: Claim) {
    val portraitBitmap = remember(claim) {
        try {
            val bytes = when (claim) {
                is MdocClaim -> claim.value.asBstr
                is JsonClaim -> claim.value.jsonPrimitive.content.fromBase64Url()
            }
            decodeImage(bytes)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = claim.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        portraitBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Portrait",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}




