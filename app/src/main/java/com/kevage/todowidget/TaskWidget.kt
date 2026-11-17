package com.kevage.todowidget

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private val taskId = ActionParameters.Key<String>("taskId")
private val white = ColorProvider(Color(0xFFFAFAFF))
private val muted = ColorProvider(Color(0xFFC9D2E5))

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}

internal class TaskWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = TaskStore.get(context)
        provideContent {
            val tasks by store.tasks.collectAsState()
            WidgetContent(context, tasks.filterNot(Task::completed))
        }
    }
}

@Composable
private fun WidgetContent(context: Context, tasks: List<Task>) {
    val rowCount = ((LocalSize.current.height.value - 40) / 30).toInt().coerceIn(1, 6)

    Column(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.app_name),
                modifier = GlanceModifier.defaultWeight()
                    .clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(color = white, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (tasks.size > rowCount) {
                Text(
                    text = "+${tasks.size - rowCount}",
                    modifier = GlanceModifier.padding(horizontal = 5.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    style = TextStyle(color = muted, fontSize = 12.sp),
                )
            }
            Box(
                modifier = GlanceModifier.width(36.dp).height(32.dp).clickable(actionStartActivity<QuickAddActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_add),
                    contentDescription = context.getString(R.string.new_task),
                    modifier = GlanceModifier.size(24.dp),
                )
            }
        }

        if (tasks.isEmpty()) {
            Text(
                text = "Keine offenen Aufgaben",
                modifier = GlanceModifier.padding(top = 8.dp),
                style = TextStyle(color = muted, fontSize = 13.sp),
                maxLines = 1,
            )
        }
        tasks.take(rowCount).forEach { task ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier.width(34.dp).height(30.dp)
                        .clickable(actionRunCallback<CompleteTask>(actionParametersOf(taskId to task.id))),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_check),
                        contentDescription = context.getString(R.string.completed_task, task.title),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }
                Text(
                    text = task.title,
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                    style = TextStyle(color = white, fontSize = 14.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

class CompleteTask : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[taskId] ?: return
        try {
            TaskStore.get(context).setCompleted(id, true)
        } catch (_: IOException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Speichern fehlgeschlagen.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        TaskWidget().updateAll(context)
    }
}
