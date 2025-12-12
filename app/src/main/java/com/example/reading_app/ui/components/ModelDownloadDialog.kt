package com.example.reading_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.reading_app.utils.TranslationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadDialog(
    targetLanguage: String,
    targetLanguageCode: String,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isDownloading) onCancel() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isDownloading) Icons.Default.Download else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isDownloading) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.tertiary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isDownloading) 
                        "Downloading Model..." 
                    else 
                        "Language Model Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isDownloading) 
                        "Downloading $targetLanguage translation model. This usually takes 10-30 seconds.\n\nThe model will be saved for offline use."
                    else
                        "To translate to $targetLanguage, a ~30MB language model needs to be downloaded.\n\nThis is a one-time download and will enable offline translation.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isDownloading) {
                    CircularProgressIndicator()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            enabled = !isDownloading
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                isDownloading = true
                                scope.launch {
                                    try {
                                        TranslationManager.downloadLanguageModel(targetLanguageCode)
                                        onDownload()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onCancel()
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isDownloading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}
