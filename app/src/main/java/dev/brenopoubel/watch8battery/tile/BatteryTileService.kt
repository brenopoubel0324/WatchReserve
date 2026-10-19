package dev.brenopoubel.watch8battery.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.brenopoubel.watch8battery.R
import dev.brenopoubel.watch8battery.ui.PanelActivity
import dev.brenopoubel.watch8battery.ui.ShortcutActivity

/** Atalhos estáticos: nenhuma leitura de bateria nem atualização periódica. */
class BatteryTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val content = LayoutElementBuilders.Column.Builder()
            .setWidth(dp(requestParams.deviceConfiguration.screenWidthDp - 48f))
            .setHeight(wrap())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(label(getString(R.string.app_name), 14f, 0xFFCDD6E1.toInt()))
            .addContent(spacer(10f))
            .addContent(button("Só relógio", "watch_only", ShortcutActivity::class.java.name))
            .addContent(spacer(8f))
            .addContent(button("Economia", "economy", PanelActivity::class.java.name))
            .build()

        val root = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder().setColor(argb(0xFF000000.toInt()))
                            .build(),
                    )
                    .build(),
            )
            .addContent(content)
            .build()

        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build())
                            .build(),
                    ).build(),
                )
                .build(),
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = Futures.immediateFuture(
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build(),
    )

    private fun button(
        title: String,
        id: String,
        activityName: String,
    ): LayoutElementBuilders.Box {
        val action = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(activityName)
                .build(),
        ).build()
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(dp(48f))
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder().setId(id).setOnClick(action).build(),
                    )
                    .setSemantics(
                        ModifiersBuilders.Semantics.Builder().setContentDescription(title).build(),
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF202E39.toInt()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder().setRadius(dp(24f)).build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .addContent(label(title, 16f, 0xFFFFFFFF.toInt()))
            .build()
    }

    private fun label(text: String, size: Float, color: Int) =
        LayoutElementBuilders.Text.Builder()
            .setText(text)
            .setMaxLines(1)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder().setSize(sp(size))
                    .setColor(argb(color)).build(),
            )
            .build()

    private fun spacer(height: Float) = LayoutElementBuilders.Spacer.Builder()
        .setHeight(dp(height)).build()

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
