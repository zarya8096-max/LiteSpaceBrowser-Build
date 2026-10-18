package com.litespace.browser.core.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.litespace.browser.LiteAppActivity
import com.litespace.browser.domain.model.LiteAppConfig

object ShortcutPinManager {

    fun pinLiteAppToHomeScreen(context: Context, config: LiteAppConfig): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }

        val iconBitmap = renderLiteAppIconBitmap(
            context = context,
            iconText = config.iconText.ifBlank { config.name.take(2).uppercase() },
            colorHex = config.iconColorHex,
            customIconUri = config.customIconUri,
            sizePx = 192
        )

        val launchIntent = Intent(context, LiteAppActivity::class.java).apply {
            action = LiteAppActivity.ACTION_LAUNCH_LITE_APP
            putExtra(LiteAppActivity.EXTRA_LITE_APP_ID, config.id)
            putExtra(LiteAppActivity.EXTRA_LITE_APP_UUID, config.uuid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "lite_app_${config.uuid}")
            .setShortLabel(config.name.take(16).ifBlank { "Lite App" })
            .setLongLabel(config.name.ifBlank { "Lite App" })
            .setIcon(IconCompat.createWithAdaptiveBitmap(iconBitmap))
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }

    fun renderLiteAppIconBitmap(
        context: Context? = null,
        iconText: String,
        colorHex: String,
        customIconUri: String? = null,
        sizePx: Int = 192
    ): Bitmap {
        val safeSize = sizePx.coerceIn(48, 512)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, safeSize.toFloat(), safeSize.toFloat())
        val cornerRadius = safeSize * 0.26f

        if (context != null && !customIconUri.isNullOrBlank()) {
            val customDecoded = runCatching {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 2
                }
                context.contentResolver.openInputStream(Uri.parse(customIconUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            }.getOrNull()

            if (customDecoded != null && customDecoded.width > 0 && customDecoded.height > 0) {
                val swBitmap = if (customDecoded.config == Bitmap.Config.ARGB_8888) {
                    customDecoded
                } else {
                    customDecoded.copy(Bitmap.Config.ARGB_8888, false) ?: customDecoded
                }
                val shader = BitmapShader(swBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val scale = maxOf(
                    safeSize.toFloat() / swBitmap.width.coerceAtLeast(1),
                    safeSize.toFloat() / swBitmap.height.coerceAtLeast(1)
                )
                val dx = (safeSize - swBitmap.width * scale) * 0.5f
                val dy = (safeSize - swBitmap.height * scale) * 0.5f
                val matrix = Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(dx, dy)
                }
                shader.setLocalMatrix(matrix)
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, imagePaint)
                return bitmap
            }
        }

        val baseColor = runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.parseColor("#0B57D0"))
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val lighterColor = Color.HSVToColor(
            floatArrayOf(
                (hsv[0] + 12f) % 360f,
                (hsv[1] * 0.85f).coerceIn(0f, 1f),
                (hsv[2] * 1.18f).coerceIn(0f, 1f)
            )
        )
        val darkerColor = Color.HSVToColor(
            floatArrayOf(
                hsv[0],
                hsv[1],
                (hsv[2] * 0.78f).coerceIn(0f, 1f)
            )
        )

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, safeSize.toFloat(), safeSize.toFloat(),
                lighterColor, darkerColor, Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = safeSize * 0.03f
        }
        val inset = safeSize * 0.015f
        canvas.drawRoundRect(
            RectF(inset, inset, safeSize - inset, safeSize - inset),
            cornerRadius,
            cornerRadius,
            strokePaint
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = safeSize * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(safeSize * 0.04f, 0f, safeSize * 0.02f, Color.argb(65, 0, 0, 0))
        }

        val label = iconText.trim().take(2).uppercase().ifBlank { "LA" }
        val fontMetrics = textPaint.fontMetrics
        val baselineY = (safeSize / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, safeSize / 2f, baselineY, textPaint)

        return bitmap
    }
}
