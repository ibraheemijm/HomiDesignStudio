package com.homi.designstudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.Color as AColor
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class DesignItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val colorIndex: Int,
    val savedAt: Long = System.currentTimeMillis(),
    val extraTexts: List<String> = emptyList(),
    val elementXs: List<Float> = emptyList(),
    val elementYs: List<Float> = emptyList(),
    val elementSizes: List<Float> = emptyList(),
    val elementColors: List<Int> = emptyList(),
    val elementOrder: List<Int> = emptyList(),
    val imageUri: String? = null,
    val imageX: Float = .5f,
    val imageY: Float = .17f,
    val imageSize: Float = .19f,
    val shapes: List<ShapeLayer> = emptyList(),
    val elementFonts: List<String> = emptyList()
)

data class ShapeLayer(val type: String, val x: Float, val y: Float, val size: Float, val color: Int = 1)

private fun Int?.orZero(): Int = this ?: 0

private fun readFloats(o: JSONObject, key: String): List<Float> = (0 until (o.optJSONArray(key)?.length() ?: 0)).mapNotNull { o.optJSONArray(key)?.optDouble(it)?.toFloat() }
private fun readInts(o: JSONObject, key: String): List<Int> = (0 until (o.optJSONArray(key)?.length() ?: 0)).mapNotNull { o.optJSONArray(key)?.optInt(it) }
private fun readStrings(o: JSONObject, key: String): List<String> = (0 until (o.optJSONArray(key)?.length() ?: 0)).mapNotNull { o.optJSONArray(key)?.optString(it) }

private fun readShapes(o: JSONObject): List<ShapeLayer> {
    val a = o.optJSONArray("shapes") ?: return emptyList()
    return (0 until a.length()).mapNotNull { i -> runCatching {
        val v = a.getJSONObject(i)
        ShapeLayer(v.optString("type", "circle"), v.optDouble("x", .5).toFloat(), v.optDouble("y", .5).toFloat(), v.optDouble("size", .12).toFloat(), v.optInt("color", 1))
    }.getOrNull() }
}

object DesignRepository {
    private const val PREFS = "homi_designs"
    private const val KEY = "items"

    fun all(context: Context): List<DesignItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                DesignItem(o.optLong("id"), o.optString("title"), o.optString("subtitle"), o.optInt("colorIndex"), o.optLong("savedAt"), (0 until o.optJSONArray("extraTexts")?.length().orZero()).mapNotNull { j -> o.optJSONArray("extraTexts")?.optString(j) }, readFloats(o, "elementXs"), readFloats(o, "elementYs"), readFloats(o, "elementSizes"), readInts(o, "elementColors"), readInts(o, "elementOrder"), o.optString("imageUri").takeIf { it.isNotBlank() }, o.optDouble("imageX", .5).toFloat(), o.optDouble("imageY", .17).toFloat(), o.optDouble("imageSize", .19).toFloat(), readShapes(o), readStrings(o, "elementFonts"))
            }.sortedByDescending { it.savedAt }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, item: DesignItem) {
        val list = all(context).filterNot { it.id == item.id } + item
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("subtitle", it.subtitle).put("colorIndex", it.colorIndex).put("savedAt", it.savedAt).put("extraTexts", JSONArray(it.extraTexts)).put("elementXs", JSONArray(it.elementXs)).put("elementYs", JSONArray(it.elementYs)).put("elementSizes", JSONArray(it.elementSizes)).put("elementColors", JSONArray(it.elementColors)).put("elementOrder", JSONArray(it.elementOrder)).put("imageUri", it.imageUri ?: "").put("imageX", it.imageX).put("imageY", it.imageY).put("imageSize", it.imageSize).put("shapes", JSONArray(it.shapes.map { sh -> JSONObject().put("type", sh.type).put("x", sh.x).put("y", sh.y).put("size", sh.size).put("color", sh.color) })).put("elementFonts", JSONArray(it.elementFonts))) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }

    fun saveDraft(context: Context, item: DesignItem) {
        val draft = JSONObject().put("id", item.id).put("title", item.title).put("subtitle", item.subtitle)
            .put("colorIndex", item.colorIndex).put("savedAt", item.savedAt).put("extraTexts", JSONArray(item.extraTexts)).put("elementXs", JSONArray(item.elementXs)).put("elementYs", JSONArray(item.elementYs)).put("elementSizes", JSONArray(item.elementSizes)).put("elementColors", JSONArray(item.elementColors)).put("elementOrder", JSONArray(item.elementOrder)).put("imageUri", item.imageUri ?: "").put("imageX", item.imageX).put("imageY", item.imageY).put("imageSize", item.imageSize).put("shapes", JSONArray(item.shapes.map { sh -> JSONObject().put("type", sh.type).put("x", sh.x).put("y", sh.y).put("size", sh.size).put("color", sh.color) })).put("elementFonts", JSONArray(item.elementFonts))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("draft", draft.toString()).apply()
    }

    fun draft(context: Context): DesignItem? = try {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("draft", null) ?: throw IllegalStateException("No saved draft")
        val o = JSONObject(raw)
        val extras = o.optJSONArray("extraTexts")
        DesignItem(o.optLong("id"), o.optString("title"), o.optString("subtitle"), o.optInt("colorIndex"), o.optLong("savedAt"),
            (0 until (extras?.length() ?: 0)).mapNotNull { extras?.optString(it) }, readFloats(o, "elementXs"), readFloats(o, "elementYs"), readFloats(o, "elementSizes"), readInts(o, "elementColors"), readInts(o, "elementOrder"), o.optString("imageUri").takeIf { it.isNotBlank() }, o.optDouble("imageX", .5).toFloat(), o.optDouble("imageY", .17).toFloat(), o.optDouble("imageSize", .19).toFloat(), readShapes(o), readStrings(o, "elementFonts"))
    } catch (_: Exception) { null }

    fun delete(context: Context, id: Long) {
        val a = JSONArray()
        all(context).filterNot { it.id == id }.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("subtitle", it.subtitle).put("colorIndex", it.colorIndex).put("savedAt", it.savedAt).put("extraTexts", JSONArray(it.extraTexts)).put("elementXs", JSONArray(it.elementXs)).put("elementYs", JSONArray(it.elementYs)).put("elementSizes", JSONArray(it.elementSizes)).put("elementColors", JSONArray(it.elementColors)).put("elementOrder", JSONArray(it.elementOrder)).put("imageUri", it.imageUri ?: "").put("imageX", it.imageX).put("imageY", it.imageY).put("imageSize", it.imageSize).put("shapes", JSONArray(it.shapes.map { sh -> JSONObject().put("type", sh.type).put("x", sh.x).put("y", sh.y).put("size", sh.size).put("color", sh.color) })).put("elementFonts", JSONArray(it.elementFonts))) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }
}

object DesignExporter {
    private val palette = intArrayOf(0xFF7657FF.toInt(), 0xFF087E8B.toInt(), 0xFFB54772.toInt(), 0xFFCA8A04.toInt())

    private fun decodePhoto(context: Context, uri: android.net.Uri): Bitmap? = try { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }

    fun render(context: Context, item: DesignItem): Bitmap {
        val w = 1080
        val h = 1350
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = android.graphics.LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), palette[item.colorIndex.coerceIn(0, 3)], 0xFF0C1020.toInt(), android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        p.color = 0xDFFFFFFF.toInt()
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        p.textSize = 54f
        c.drawText("✦   ✦   ✦", 540f, 135f, p)
        item.shapes.forEach { shape ->
            val cx = shape.x.coerceIn(.02f, .98f) * w
            val cy = shape.y.coerceIn(.02f, .98f) * h
            val side = shape.size.coerceIn(.025f, .4f) * w
            p.color = intArrayOf(0xFFFFFFFF.toInt(), 0xFF53E0D0.toInt(), 0xFFFFD166.toInt(), 0xFFFF8FAB.toInt())[shape.color.coerceIn(0, 3)]
            p.style = Paint.Style.STROKE
            p.strokeWidth = 8f
            when (shape.type) {
                "line" -> c.drawLine(cx - side, cy, cx + side, cy, p)
                "rect" -> c.drawRoundRect(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2, 14f, 14f, p)
                else -> c.drawCircle(cx, cy, side / 2, p)
            }
            p.style = Paint.Style.FILL
        }
        item.imageUri?.let { uriString ->
            try {
                val photo = decodePhoto(context, android.net.Uri.parse(uriString))
                if (photo != null) {
                    val side = (item.imageSize.coerceIn(.08f, .45f) * w)
                    val left = (item.imageX.coerceIn(.08f, .92f) * w) - side / 2f
                    val top = (item.imageY.coerceIn(.08f, .85f) * h) - side / 2f
                    val save = c.save()
                    val path = android.graphics.Path().apply { addCircle(left + side / 2f, top + side / 2f, side / 2f, android.graphics.Path.Direction.CW) }
                    c.clipPath(path)
                    c.drawBitmap(photo, null, android.graphics.RectF(left, top, left + side, top + side), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    c.restoreToCount(save)
                    photo.recycle()
                }
            } catch (_: Exception) { }
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = 0x55FFFFFF
        c.drawRoundRect(48f, 48f, 1032f, 1302f, 38f, 38f, p)
        p.style = Paint.Style.FILL
        p.color = AColor.WHITE
        p.textAlign = Paint.Align.CENTER
        val texts = listOf(item.title.ifBlank { "عنوان تصميمك" }, item.subtitle.ifBlank { "أضف عبارة تلهم الآخرين" }) + item.extraTexts.take(4)
        val defaultYs = listOf(.392f, .533f, .615f, .668f, .721f, .774f)
        val defaultSizes = listOf(28f, 15f, 13f, 13f, 13f, 13f)
        val order = (item.elementOrder + texts.indices.filterNot { it in item.elementOrder }).filter { it in texts.indices }
        order.forEach { index ->
            p.textAlign = Paint.Align.CENTER
            p.color = item.elementColors.getOrNull(index)?.let { intArrayOf(0xFFFFFFFF.toInt(), 0xFF53E0D0.toInt(), 0xFFFFD166.toInt(), 0xFFFF8FAB.toInt())[it.coerceIn(0, 3)] } ?: 0xE6FFFFFF.toInt()
            p.typeface = try {
                val fontId = item.elementFonts.getOrNull(index) ?: "sans-serif"
                val base = if (fontId.startsWith("file:")) Typeface.createFromFile(File(fontId.removePrefix("file:"))) else Typeface.create(fontId, Typeface.NORMAL)
                Typeface.create(base, if (index == 0) Typeface.BOLD else Typeface.NORMAL)
            } catch (_: Exception) {
                Typeface.create(if (index == 0) "sans-serif" else "sans-serif-medium", if (index == 0) Typeface.BOLD else Typeface.NORMAL)
            }
            val sizeSp = item.elementSizes.getOrNull(index) ?: defaultSizes[index.coerceAtMost(defaultSizes.lastIndex)]
            p.textSize = sizeSp * 2.35f
            val x = (item.elementXs.getOrNull(index) ?: .5f).coerceIn(.08f, .92f) * w
            val y = (item.elementYs.getOrNull(index) ?: defaultYs[index.coerceAtMost(defaultYs.lastIndex)]) * h
            drawLines(c, texts[index], p, x, y, 850f, p.textSize * 1.22f)
        }
        p.textSize = 24f
        p.color = 0xBFFFFFFF.toInt()
        c.drawText("HOMI DESIGN", 540f, 1200f, p)
        return b
    }

    private fun drawLines(c: Canvas, value: String, p: Paint, x: Float, centerY: Float, maxWidth: Float, lineHeight: Float) {
        val lines = mutableListOf<String>()
        var line = ""
        value.split(Regex("\\s+")).forEach { word ->
            val test = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && p.measureText(test) > maxWidth) { lines.add(line); line = word } else line = test
        }
        if (line.isNotEmpty()) lines.add(line)
        val firstY = centerY - (lines.size - 1) * lineHeight / 2f
        lines.forEachIndexed { i, s -> c.drawText(s, x, firstY + i * lineHeight, p) }
    }

    fun exportPng(context: Context, item: DesignItem): String {
        val name = "HomiDesign_${item.id}.png"
        val bitmap = render(context, item)
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HomiDesignStudio")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("تعذر إنشاء الصورة")
            context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("تعذر حفظ الصورة")
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HomiDesignStudio").apply { mkdirs() }
            FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        bitmap.recycle()
        return name
    }

    fun exportPdf(context: Context, item: DesignItem): String {
        val name = "HomiDesign_${item.id}.pdf"
        val bitmap = render(context, item)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        // Keep the 4:5 artwork proportional and centered on an A4-sized page.
        page.canvas.drawBitmap(bitmap, null, Rect(0, 49, 595, 793), Paint(Paint.ANTI_ALIAS_FLAG))
        doc.finishPage(page)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/HomiDesignStudio")
                }
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values) ?: error("تعذر إنشاء PDF")
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } ?: error("تعذر حفظ PDF")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HomiDesignStudio").apply { mkdirs() }
                FileOutputStream(File(dir, name)).use { doc.writeTo(it) }
            }
        } finally { doc.close(); bitmap.recycle() }
        return name
    }
}
