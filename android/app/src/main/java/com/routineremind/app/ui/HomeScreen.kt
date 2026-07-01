package com.routineremind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.routineremind.app.data.LinkedStudent
import com.routineremind.app.data.Question
import com.routineremind.app.data.ScheduleItem
import com.routineremind.app.ui.theme.Primary
import com.routineremind.app.ui.theme.Success
import com.routineremind.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("RoutineRemind", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { vm.signOut() }) { Text("Sign out") }
        }

        val profile = vm.profile
        when {
            profile?.role == null -> RoleSelection(vm)
            profile.role == "parent" && vm.linkedStudents.isEmpty() -> LinkStudent(vm)
            else -> {
                if (profile.role == "student" && profile.shareCode != null) {
                    ShareCodeCard(profile.shareCode)
                    Spacer(Modifier.height(16.dp))
                }
                if (profile.role == "parent") {
                    StudentSwitcher(vm)
                    Spacer(Modifier.height(16.dp))
                }
                TodaySchedule(vm)
                if (profile.role == "student" && vm.schedule != null) {
                    Spacer(Modifier.height(16.dp))
                    QuestionsSection(vm)
                    Spacer(Modifier.height(16.dp))
                    NativeMediaCard(vm)
                }
            }
        }
    }
}

@Composable
private fun RoleSelection(vm: AppViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Welcome to RoutineRemind", style = MaterialTheme.typography.headlineMedium)
            Text("Tell us who you are.", color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.chooseRole("student") },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("I'm a Student") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.chooseRole("parent") },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("I'm a Parent") }
        }
    }
}

@Composable
private fun LinkStudent(vm: AppViewModel) {
    var code by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Link a student", style = MaterialTheme.typography.titleLarge)
            Text("Enter your student's 6-character share code.", color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Share code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            vm.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.linkStudent(code) },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Link student") }
        }
    }
}

@Composable
private fun StudentSwitcher(vm: AppViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Selected student", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            vm.linkedStudents.forEach { student ->
                val selected = student.uid == vm.selectedStudentUid
                OutlinedButton(
                    onClick = { vm.selectStudent(student.uid) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (selected) "✓ ${studentLabel(student)}" else studentLabel(student))
                }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = { vm.unlinkSelectedStudent() }, enabled = !vm.busy && vm.selectedStudentUid != null) {
                Text("Unlink selected student")
            }
        }
    }
}

private fun studentLabel(student: LinkedStudent): String =
    student.displayName?.takeIf { it.isNotBlank() }
        ?: student.email?.takeIf { it.isNotBlank() }
        ?: student.shareCode?.takeIf { it.isNotBlank() }
        ?: student.uid

@Composable
private fun ShareCodeCard(code: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("YOUR SHARE CODE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(code, style = MaterialTheme.typography.headlineMedium, color = Primary)
            Text("Give this to your parent to link your account.", color = TextSecondary)
        }
    }
}

@Composable
private fun TodaySchedule(vm: AppViewModel) {
    Text("Today's schedule", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))

    val schedule = vm.schedule
    if (schedule == null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No schedule for today", style = MaterialTheme.typography.titleLarge)
                Text("It'll show up here once added.", color = TextSecondary)
            }
        }
    } else {
        Text(schedule.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(schedule.items) { item ->
                ScheduleRow(
                    item = item,
                    canComplete = vm.profile?.role == "student",
                    completing = vm.completingItemId == item.id,
                    onComplete = { vm.completeItem(item.id) },
                )
            }
        }
    }
}

@Composable
private fun NativeMediaCard(vm: AppViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Native media processing", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(vm.nativeAudioStatus, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.checkNativeAudio() }) {
                Text("Re-check C++ module")
            }
        }
    }
}

@Composable
private fun QuestionsSection(vm: AppViewModel) {
    Text("Questions", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    if (vm.questions.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No questions yet", style = MaterialTheme.typography.titleLarge)
                Text("Questions from your parent will appear here.", color = TextSecondary)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.questions.forEach { question -> QuestionCard(question, vm) }
        }
    }
}

@Composable
private fun QuestionCard(question: Question, vm: AppViewModel) {
    var answer by remember(question.id) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(question.prompt, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Your answer") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    vm.submitAnswer(question.id, answer)
                    answer = ""
                },
                enabled = vm.answeringQuestionId != question.id,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (vm.answeringQuestionId == question.id) "Submitting…" else "Submit answer")
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    canComplete: Boolean,
    completing: Boolean,
    onComplete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                item.time,
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                )
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            if (item.completed) {
                Text("✓", color = Success, style = MaterialTheme.typography.titleLarge)
            } else if (canComplete) {
                OutlinedButton(onClick = onComplete, enabled = !completing) {
                    Text(if (completing) "Saving…" else "Done")
                }
            }
        }
    }
}
