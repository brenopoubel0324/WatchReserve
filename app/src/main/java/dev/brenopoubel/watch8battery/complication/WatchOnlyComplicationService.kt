package dev.brenopoubel.watch8battery.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import dev.brenopoubel.watch8battery.R
import dev.brenopoubel.watch8battery.ui.ShortcutActivity

/** A complicação é apenas um atalho e não solicita atualizações periódicas. */
class WatchOnlyComplicationService : ComplicationDataSourceService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener,
    ) {
        listener.onComplicationData(data(request.complicationType, preview = false))
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        data(type, preview = true)

    private fun data(type: ComplicationType, preview: Boolean): ComplicationData? {
        val icon = Icon.createWithResource(this, R.drawable.ic_watch)
        val description = PlainComplicationText.Builder("Abrir o atalho Só relógio").build()
        // A prévia permanece constante e não cria uma ação executável.
        val tapAction = if (preview) null else PendingIntent.getActivity(
            this,
            SHORTCUT_REQUEST_CODE,
            Intent(this, ShortcutActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Relógio").build(),
                contentDescription = description,
            )
                .setMonochromaticImage(MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_watch_monochrome),
                ).build())
                .setTapAction(tapAction)
                .build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                contentDescription = description,
            )
                .setTapAction(tapAction)
                .build()

            else -> null
        }
    }

    companion object {
        private const val SHORTCUT_REQUEST_CODE = 100

        /** Renovação explícita ao reabrir o app, inclusive após uma parada forçada. */
        fun refresh(context: Context) {
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, WatchOnlyComplicationService::class.java),
            ).requestUpdateAll()
        }
    }
}
