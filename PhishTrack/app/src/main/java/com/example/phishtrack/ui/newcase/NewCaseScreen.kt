package com.example.phishtrack.ui.newcase

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import com.example.phishtrack.theme.LocalExtendedColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
    )
}

@Composable
fun RequiredLabel(text: String) {
    Text(buildAnnotatedString {
        append(text)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
            append(" *")
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCaseScreen(
    onBackClick: () -> Unit,
    onSubmitCase: (title: String, targetType: String, url: String?, targetIp: String?, description: String?, source: String, priority: String, tags: List<String>) -> Unit,
    submitResetTrigger: Int = 0
) {
    var title by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var selectedTargetType by remember { mutableStateOf("URL") }
    var description by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Email") }
    var selectedPriority by remember { mutableStateOf("High") }
    var tagsInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(submitResetTrigger) {
        if (submitResetTrigger > 0) isSubmitting = false
    }

    val urlFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }
    val tagsFocusRequester = remember { FocusRequester() }

    val maxDescLength = 500
    val ipv4Regex = remember { Regex("""^(?:(?:25[0-5]|2[0-4]\d|[01]?\d?\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d?\d?)$""") }

    val trimmedTarget = targetInput.trim()
    val isValidTarget = if (selectedTargetType == "URL") {
        trimmedTarget.isNotEmpty() && (trimmedTarget.startsWith("http://") || trimmedTarget.startsWith("https://"))
    } else {
        trimmedTarget.isNotEmpty() && ipv4Regex.matches(trimmedTarget)
    }
    val isTargetError = (targetInput.isNotEmpty() || hasAttemptedSubmit) && !isValidTarget

    val isValidTitle = title.trim().isNotEmpty()
    val isTitleError = hasAttemptedSubmit && !isValidTitle

    val isValidDesc = description.trim().isNotEmpty() && description.length <= maxDescLength
    val isDescError = (hasAttemptedSubmit && description.trim().isEmpty()) || description.length > maxDescLength

    val canSubmit = isValidTarget && isValidTitle && isValidDesc && !isSubmitting

    val submitInteractionSource = remember { MutableInteractionSource() }
    val isSubmitPressed by submitInteractionSource.collectIsPressedAsState()
    val submitScale by animateFloatAsState(if (isSubmitPressed && canSubmit) 0.96f else 1f, animationSpec = tween(150), label = "submitScale")

    val performSubmit = {
        hasAttemptedSubmit = true
        if (canSubmit) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSubmitting = true
            val tagsList = if (tagsInput.trim().isEmpty()) emptyList() else tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val finalDescription = description.trim().ifEmpty { null }
            val url = if (selectedTargetType == "URL") trimmedTarget else null
            val targetIp = if (selectedTargetType == "IP") trimmedTarget else null
            onSubmitCase(title.trim(), selectedTargetType, url, targetIp, finalDescription, selectedSource, selectedPriority, tagsList)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Error feedback
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NEW PHISHING CASE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    Text(
                        " ⬅ ",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            SectionHeader("TARGET INFORMATION")

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; hasAttemptedSubmit = false },
                label = { RequiredLabel("Case Title") },
                placeholder = { Text("e.g., Fake PayPal Login Page") },
                singleLine = true,
                isError = isTitleError,
                supportingText = {
                    AnimatedVisibility(
                        visible = isTitleError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text("Case title is required", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { urlFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TARGET TYPE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val targetTypes = listOf("URL", "IP")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                targetTypes.forEach { type ->
                    val isSelected = selectedTargetType == type
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedTargetType = type
                                targetInput = ""
                                hasAttemptedSubmit = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URL or IP input with Paste button
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it; hasAttemptedSubmit = false },
                label = { RequiredLabel(if (selectedTargetType == "URL") "Target Phishing URL" else "Target IP Address") },
                placeholder = { Text(if (selectedTargetType == "URL") "https://example-scam-site.com" else "192.168.1.1") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Target", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text
                            if (!text.isNullOrEmpty()) {
                                targetInput = text.toString()
                                hasAttemptedSubmit = false
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true,
                isError = isTargetError,
                supportingText = {
                    AnimatedVisibility(
                        visible = isTargetError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            when {
                                targetInput.isEmpty() -> if (selectedTargetType == "URL") "URL is required" else "IP address is required"
                                selectedTargetType == "URL" -> "Enter a valid URL starting with https://"
                                else -> "Enter a valid IPv4 address (e.g. 192.168.1.1)"
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { descFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(urlFocusRequester)
            )

            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("CASE DETAILS")

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; hasAttemptedSubmit = false },
                label = { RequiredLabel("Case Description / Context") },
                placeholder = { Text("Describe suspicious indicators, brand spoofing target, etc.") },
                minLines = 3,
                maxLines = 5,
                isError = isDescError,
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AnimatedVisibility(
                            visible = isDescError,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                if (description.isEmpty()) "Description is required" else "Description is too long",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (!isDescError) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = "${description.length} / $maxDescLength",
                            color = if (description.length > maxDescLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { tagsFocusRequester.requestFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descFocusRequester)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Source Selector Chips
            Text(
                text = "SOURCE CHANNEL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sources = listOf("Email", "WhatsApp", "SMS", "Other")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEach { source ->
                    val isSelected = selectedSource == source
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedSource = source; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = source,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Priority Selector Chips
            Text(
                text = "CASE PRIORITY LEVEL",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val priorities = listOf("Low", "Medium", "High", "Critical")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priorities.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val accentColor = when (priority) {
                        "Critical" -> MaterialTheme.colorScheme.error
                        "High" -> LocalExtendedColors.current.warning
                        "Medium" -> LocalExtendedColors.current.mediumPriority
                        else -> LocalExtendedColors.current.info
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPriority = priority; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tags
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (Comma Separated)") },
                placeholder = { Text("paypal, login, credential-harvesting") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus() 
                        performSubmit()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(tagsFocusRequester)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Submit Button wrapped in Box to intercept clicks if disabled
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(submitScale)
            ) {
                Button(
                    onClick = performSubmit,
                    enabled = canSubmit,
                    interactionSource = submitInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (selectedTargetType == "URL") "ANALYZE LINK SAFELY" else "ANALYZE IP SAFELY",
                            color = if (canSubmit) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // If button is visually disabled, we put a transparent clickable overlay to show errors when tapped
                if (!canSubmit && !isSubmitting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                performSubmit()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
