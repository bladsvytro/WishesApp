package com.wishesapp.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wishesapp.MainActivity
import com.wishesapp.data.model.WidgetMode
import com.wishesapp.data.model.WidgetState
import com.wishesapp.data.repository.WidgetPrefsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.io.File

class WishWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WishWidgetEntryPoint {
        fun widgetPrefsRepository(): WidgetPrefsRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WishWidgetEntryPoint::class.java,
        )
        val state: WidgetState = entryPoint.widgetPrefsRepository().widgetState.first()

        provideContent {
            WishWidgetContent(context = context, state = state)
        }
    }

    @Composable
    private fun WishWidgetContent(context: Context, state: WidgetState) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.BottomStart,
        ) {
            // Background image (if available)
            val imageFile = state.imagePath?.let { File(it) }
            if (imageFile != null && imageFile.exists()) {
                Image(
                    provider = ImageProvider(BitmapFactory.decodeFile(imageFile.absolutePath)),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Gradient-like fallback background
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFFD63865))),
                )
            }

            // Text overlay at the bottom
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(androidx.compose.ui.graphics.Color(0x99000000)))
                    .padding(12.dp),
            ) {
                if (state.wishText.isNotBlank()) {
                    Text(
                        text = state.wishText.take(120),
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        maxLines = 3,
                    )
                }
                if (state.authorName.isNotBlank()) {
                    Text(
                        text = "— ${state.authorName}",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFB3C6)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                // Show "tap to shuffle" hint in random mode
                if (state.mode == WidgetMode.RANDOM) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to shuffle ↻",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xAAFFFFFF)),
                            fontSize = 10.sp,
                        ),
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<ShuffleWishAction>()
                        ),
                    )
                }
            }
        }
    }
}

/** Tapping "shuffle" picks a random wish from the cache and refreshes the widget. */
class ShuffleWishAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WishWidgetWorker.enqueueForShuffle(context)
    }
}

class WishWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WishWidget()
}
