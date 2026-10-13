package app.hundred

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Paper = Color(0xFFFAF7F2)
private val Ink = Color(0xFF1F1B16)
private val Muted = Color(0xFF8A8378)
private val Track = Color(0xFFE9E3D8)
private val Accent = Color(0xFFD9573B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Ink, onPrimary = Paper, background = Paper, surface = Paper,
                    onSurface = Ink, error = Accent,
                )
            ) {
                App(store)
            }
        }
    }
}

@Composable
fun App(store: Store) {
    var skills by remember { mutableStateOf(store.load()) }
    var openId by remember { mutableStateOf<Long?>(null) }
    fun update(new: List<Skill>) { skills = new; store.save(new) }

    val open = skills.firstOrNull { it.id == openId }
    BackHandler(open != null) { openId = null }

    Surface(Modifier.fillMaxSize(), color = Paper) {
        AnimatedContent(
            targetState = open?.id,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "screen",
        ) { id ->
            val skill = skills.firstOrNull { it.id == id }
            if (skill == null) {
                Home(
                    skills,
                    onOpen = { openId = it },
                    onAdd = { name ->
                        val s = Skill(System.currentTimeMillis(), name)
                        update(skills + s); openId = s.id
                    },
                )
            } else {
                Practice(
                    skill,
                    onBack = { openId = null },
                    onAttempt = { update(skills.map { if (it.id == skill.id) it.copy(attempts = it.attempts + 1) else it }) },
                    onUndo = { update(skills.map { if (it.id == skill.id) it.copy(attempts = (it.attempts - 1).coerceAtLeast(0)) else it }) },
                    onQuit = { update(skills.filter { it.id != skill.id }); openId = null },
                )
            }
        }
    }
}

@Composable
fun Home(skills: List<Skill>, onOpen: (Long) -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() { val t = text.trim(); if (t.isNotEmpty()) { onAdd(t); text = "" } }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("100", fontSize = 56.sp, fontWeight = FontWeight.Light, color = Ink)
        Text("Try it 100 times before you're allowed to quit.", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(skills, key = { it.id }) { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(s.id) }.padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.name, fontSize = 20.sp, color = Ink, modifier = Modifier.weight(1f))
                    Text(
                        if (s.canQuit) "free" else "${s.remaining}",
                        fontSize = 20.sp, fontWeight = FontWeight.Medium,
                        color = if (s.canQuit) Muted else Accent,
                    )
                }
                HorizontalDivider(color = Track)
            }
            if (skills.isEmpty()) {
                item { Text("Nothing yet. What do you want to get good at?", color = Muted, modifier = Modifier.padding(top = 8.dp)) }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("New skill…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink, unfocusedBorderColor = Track, cursorColor = Ink,
            ),
            trailingIcon = {
                TextButton(onClick = ::submit, enabled = text.isNotBlank()) { Text("Add") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun Practice(skill: Skill, onBack: () -> Unit, onAttempt: () -> Unit, onUndo: () -> Unit, onQuit: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var confirmQuit by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        (skill.attempts.coerceAtMost(GOAL)) / GOAL.toFloat(), tween(400), label = "progress",
    )

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = Muted) }
            Spacer(Modifier.weight(1f))
            if (skill.attempts > 0) TextButton(onClick = onUndo) { Text("Undo", color = Muted) }
        }
        Spacer(Modifier.height(8.dp))
        Text(skill.name, fontSize = 28.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 8.dp.toPx()
                    val inset = w / 2
                    val sz = Size(size.width - w, size.height - w)
                    drawArc(Track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(w))
                    drawArc(Accent, -90f, 360f * progress, false, Offset(inset, inset), sz,
                        style = Stroke(w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${skill.remaining}", fontSize = 96.sp, fontWeight = FontWeight.ExtraLight,
                    )
                    Text(
                        if (skill.canQuit) "${skill.attempts} attempts" else "to go",
                        color = Muted, fontSize = 16.sp,
                    )
                }
            }
        }

        Button(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAttempt() },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
        ) { Text("I tried", fontSize = 20.sp) }

        Spacer(Modifier.height(12.dp))
        if (skill.canQuit) {
            Text("You've earned the right to quit.", color = Muted, fontSize = 14.sp)
            TextButton(onClick = { confirmQuit = true }) { Text("Quit this skill", color = Accent) }
        } else {
            Text("Quit unlocks in ${skill.remaining}", color = Track, fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp))
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            containerColor = Paper,
            title = { Text("Quit ${skill.name}?") },
            text = { Text("${skill.attempts} attempts. That's real effort either way.") },
            confirmButton = { TextButton(onClick = onQuit) { Text("Quit", color = Accent) } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Keep going", color = Ink) } },
        )
    }
}
