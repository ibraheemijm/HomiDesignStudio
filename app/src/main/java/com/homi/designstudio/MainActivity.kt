package com.homi.designstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF080B14)
private val SurfaceGlass = Color(0xFF151B2B)
private val Purple = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF53E0D0)
private val Muted = Color(0xFF9AA5BC)

private data class DesignTemplate(val title: String, val subtitle: String, val color: Color, val category: String)
private data class EditorSnapshot(val title: String, val subtitle: String, val colorIndex: Int, val extraTexts: List<String>, val xs: List<Float>, val ys: List<Float>, val sizes: List<Float>, val colors: List<Int>, val order: List<Int>, val imageUri: String?, val imageX: Float, val imageY: Float, val imageSize: Float, val shapes: List<ShapeLayer>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomiDesignApp() }
    }
}

@Composable
private fun HomiDesignApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var templateQuery by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("اصنع شيئًا مميزًا") }
    var subtitleText by remember { mutableStateOf("فكرتك تستحق أن تُرى") }
    var extraTexts by remember { mutableStateOf(listOf<String>()) }
    var selectedColor by remember { mutableIntStateOf(0) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var imageX by remember { mutableFloatStateOf(.5f) }
    var imageY by remember { mutableFloatStateOf(.17f) }
    var imageSize by remember { mutableFloatStateOf(.19f) }
    var elementXs by remember { mutableStateOf(listOf(.5f, .5f, .5f, .5f, .5f, .5f)) }
    var elementYs by remember { mutableStateOf(listOf(.392f, .533f, .615f, .668f, .721f, .774f)) }
    var elementSizes by remember { mutableStateOf(listOf(28f, 15f, 13f, 13f, 13f, 13f)) }
    var elementColors by remember { mutableStateOf(listOf(0, 0, 0, 0, 0, 0)) }
    var elementOrder by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5)) }
    var selectedElement by remember { mutableIntStateOf(0) }
    var shapes by remember { mutableStateOf(listOf<ShapeLayer>()) }
    var selectedShape by remember { mutableIntStateOf(-1) }
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val currentSnapshot = { EditorSnapshot(titleText, subtitleText, selectedColor, extraTexts, elementXs, elementYs, elementSizes, elementColors, elementOrder, null, imageX, imageY, imageSize, shapes) }
    val recordEdit = {
        undoStack.add(currentSnapshot())
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
    }
    val applySnapshot: (EditorSnapshot) -> Unit = { snapshot ->
        titleText = snapshot.title
        subtitleText = snapshot.subtitle
        selectedColor = snapshot.colorIndex
        extraTexts = snapshot.extraTexts
        elementXs = snapshot.xs; elementYs = snapshot.ys; elementSizes = snapshot.sizes; elementColors = snapshot.colors; elementOrder = snapshot.order; imageUri = snapshot.imageUri; imageX = snapshot.imageX; imageY = snapshot.imageY; imageSize = snapshot.imageSize; shapes = snapshot.shapes
    }
    val context = LocalContext.current
    var editorOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    val savedDesigns = remember { mutableStateListOf<DesignItem>().apply { addAll(DesignRepository.all(context)) } }
    var hasDraft by remember { mutableStateOf(DesignRepository.draft(context) != null) }
    LaunchedEffect(editorOpen, titleText, subtitleText, selectedColor, extraTexts, elementXs, elementYs, elementSizes, elementColors, elementOrder, imageUri, editingId) {
        if (editorOpen) {
            DesignRepository.saveDraft(context, DesignItem(editingId ?: -1L, titleText, subtitleText, selectedColor, extraTexts = extraTexts, elementXs = elementXs, elementYs = elementYs, elementSizes = elementSizes, elementColors = elementColors, elementOrder = elementOrder, imageUri = imageUri, imageX = imageX, imageY = imageY, imageSize = imageSize))
            hasDraft = true
        }
    }
    val accents = listOf(Color(0xFF7657FF), Color(0xFF087E8B), Color(0xFFB54772), Color(0xFFCA8A04))

    MaterialTheme(colorScheme = darkColorScheme(
        background = Background, surface = SurfaceGlass, primary = Purple,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF10152A), Background, Color(0xFF080B14)))
        )) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("HOMI  /  DESIGN", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(if (editorOpen) "محرر التصميم" else "استوديو التصميم", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("حوّل أفكارك إلى تصاميم", color = Muted, fontSize = 13.sp)
                    }
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Purple, Color(0xFF3C8DFF)))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                if (editorOpen) {
                    EditorScreen(
                        Modifier.weight(1f), titleText, { value -> recordEdit(); titleText = value }, subtitleText, { value -> recordEdit(); subtitleText = value },
                        accents, selectedColor, { value -> recordEdit(); selectedColor = value }, extraTexts, { value -> recordEdit(); extraTexts = value },
                        elementXs = elementXs, elementYs = elementYs, elementSizes = elementSizes, elementColors = elementColors, elementOrder = elementOrder,
                        imageUri = imageUri, imageX = imageX, imageY = imageY, imageSize = imageSize,
                        onMoveImage = { dx, dy -> recordEdit(); imageX = (imageX + dx).coerceIn(.08f, .92f); imageY = (imageY + dy).coerceIn(.08f, .85f) },
                        onImageSize = { recordEdit(); imageSize = it }, onPickImage = { imagePicker.launch(arrayOf("image/*")) }, onRemoveImage = { recordEdit(); imageUri = null },
                        selectedElement = selectedElement, onSelectElement = { selectedElement = it },
                        onMoveElement = { i, dx, dy -> elementXs = elementXs.toMutableList().also { while (it.size <= i) it.add(.5f); it[i] = (it[i] + dx).coerceIn(.08f, .92f) }; elementYs = elementYs.toMutableList().also { while (it.size <= i) it.add(.5f); it[i] = (it[i] + dy).coerceIn(.08f, .9f) } },
                        onSizeElement = { i, size -> recordEdit(); elementSizes = elementSizes.toMutableList().also { while (it.size <= i) it.add(13f); it[i] = size } },
                        onColorElement = { i, color -> recordEdit(); elementColors = elementColors.toMutableList().also { while (it.size <= i) it.add(0); it[i] = color } },
                        onLayerElement = { i, delta -> recordEdit(); val order = (elementOrder.filter { it < 2 + extraTexts.size } + (0 until (2 + extraTexts.size)).filterNot { it in elementOrder }).toMutableList(); val at = order.indexOf(i); val to = (at + delta).coerceIn(0, order.lastIndex); if (at >= 0) { val tmp = order[at]; order[at] = order[to]; order[to] = tmp; elementOrder = order } },
                        canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                        onUndo = { if (undoStack.isNotEmpty()) { redoStack.add(currentSnapshot()); applySnapshot(undoStack.removeAt(undoStack.lastIndex)) } },
                        onRedo = { if (redoStack.isNotEmpty()) { undoStack.add(currentSnapshot()); applySnapshot(redoStack.removeAt(redoStack.lastIndex)) } },
                        onBack = { editorOpen = false },
                        onSave = {
                            val id = editingId ?: System.currentTimeMillis()
                            val item = DesignItem(id, titleText, subtitleText, selectedColor, extraTexts = extraTexts, elementXs = elementXs, elementYs = elementYs, elementSizes = elementSizes, elementColors = elementColors, elementOrder = elementOrder, imageUri = imageUri, imageX = imageX, imageY = imageY, imageSize = imageSize)
                            DesignRepository.save(context, item)
                            savedDesigns.removeAll { it.id == id }; savedDesigns.add(0, item); editingId = id
                            Toast.makeText(context, "تم حفظ التصميم على الجهاز", Toast.LENGTH_SHORT).show()
                        },
                        onExportPng = {
                            try { Toast.makeText(context, "تم حفظ الصورة: " + DesignExporter.exportPng(context, DesignItem(editingId ?: System.currentTimeMillis(), titleText, subtitleText, selectedColor, extraTexts = extraTexts, elementXs = elementXs, elementYs = elementYs, elementSizes = elementSizes, elementColors = elementColors, elementOrder = elementOrder, imageUri = imageUri, imageX = imageX, imageY = imageY, imageSize = imageSize)), Toast.LENGTH_LONG).show() }
                            catch (e: Exception) { Toast.makeText(context, "تعذر تصدير الصورة: " + (e.message ?: ""), Toast.LENGTH_LONG).show() }
                        },
                        onExportPdf = {
                            try { Toast.makeText(context, "تم حفظ PDF: " + DesignExporter.exportPdf(context, DesignItem(editingId ?: System.currentTimeMillis(), titleText, subtitleText, selectedColor, extraTexts = extraTexts, elementXs = elementXs, elementYs = elementYs, elementSizes = elementSizes, elementColors = elementColors, elementOrder = elementOrder, imageUri = imageUri, imageX = imageX, imageY = imageY, imageSize = imageSize)), Toast.LENGTH_LONG).show() }
                            catch (e: Exception) { Toast.makeText(context, "تعذر تصدير PDF: " + (e.message ?: ""), Toast.LENGTH_LONG).show() }
                        }
                    )
                } else {
                    when (selectedTab) {
                        0 -> HomeScreen(Modifier.weight(1f), selectedCategory, { selectedCategory = it }, templateQuery, { templateQuery = it }, accents, selectedColor, hasDraft,
                            onRestoreDraft = { DesignRepository.draft(context)?.let { draft ->
                                titleText = draft.title; subtitleText = draft.subtitle; extraTexts = draft.extraTexts; imageUri = draft.imageUri; imageX = draft.imageX; imageY = draft.imageY; imageSize = draft.imageSize
                                selectedColor = draft.colorIndex.coerceIn(0, 3); elementXs = draft.elementXs.ifEmpty { elementXs }; elementYs = draft.elementYs.ifEmpty { elementYs }; elementSizes = draft.elementSizes.ifEmpty { elementSizes }; elementColors = draft.elementColors.ifEmpty { elementColors }; elementOrder = draft.elementOrder.ifEmpty { elementOrder }; editingId = null; editorOpen = true
                                undoStack.clear(); redoStack.clear()
                            } },
                            onNew = { editingId = null; editorOpen = true; titleText = "اصنع شيئًا مميزًا"; subtitleText = "فكرتك تستحق أن تُرى"; extraTexts = emptyList(); selectedColor = 0; elementXs = listOf(.5f,.5f,.5f,.5f,.5f,.5f); elementYs = listOf(.392f,.533f,.615f,.668f,.721f,.774f); elementSizes = listOf(28f,15f,13f,13f,13f,13f); elementColors = listOf(0,0,0,0,0,0); elementOrder = listOf(0,1,2,3,4,5); imageUri = null; imageX = .5f; imageY = .17f; imageSize = .19f; selectedElement = 0; undoStack.clear(); redoStack.clear() },
                            onTemplate = { text -> editingId = null; titleText = text; extraTexts = emptyList(); imageUri = null; elementXs = listOf(.5f,.5f,.5f,.5f,.5f,.5f); elementYs = listOf(.392f,.533f,.615f,.668f,.721f,.774f); elementSizes = listOf(28f,15f,13f,13f,13f,13f); elementColors = listOf(0,0,0,0,0,0); elementOrder = listOf(0,1,2,3,4,5); selectedElement = 0; editorOpen = true })
                        1 -> SavedDesignsScreen(Modifier.weight(1f), savedDesigns,
                            onOpen = { item -> titleText = item.title; subtitleText = item.subtitle; extraTexts = item.extraTexts; selectedColor = item.colorIndex.coerceIn(0, 3); elementXs = item.elementXs.ifEmpty { elementXs }; elementYs = item.elementYs.ifEmpty { elementYs }; elementSizes = item.elementSizes.ifEmpty { elementSizes }; elementColors = item.elementColors.ifEmpty { elementColors }; elementOrder = item.elementOrder.ifEmpty { elementOrder }; imageUri = item.imageUri; imageX = item.imageX; imageY = item.imageY; imageSize = item.imageSize; selectedElement = 0; editingId = item.id; editorOpen = true },
                            onDelete = { item -> DesignRepository.delete(context, item.id); savedDesigns.removeAll { it.id == item.id } })
                        2 -> SavedDesignsScreen(Modifier.weight(1f), savedDesigns,
                            onOpen = { item -> titleText = item.title; subtitleText = item.subtitle; extraTexts = item.extraTexts; selectedColor = item.colorIndex.coerceIn(0, 3); elementXs = item.elementXs.ifEmpty { elementXs }; elementYs = item.elementYs.ifEmpty { elementYs }; elementSizes = item.elementSizes.ifEmpty { elementSizes }; elementColors = item.elementColors.ifEmpty { elementColors }; elementOrder = item.elementOrder.ifEmpty { elementOrder }; imageUri = item.imageUri; imageX = item.imageX; imageY = item.imageY; imageSize = item.imageSize; selectedElement = 0; editingId = item.id; editorOpen = true },
                            onDelete = { item -> DesignRepository.delete(context, item.id); savedDesigns.removeAll { it.id == item.id } })
                        else -> SimpleState(Modifier.weight(1f), "الإعدادات", "قريبًا: اللغة، الجودة، ومظهر التطبيق.", Icons.Default.Settings)
                    }
                }
                NavigationBar(containerColor = Color(0xE6101422), tonalElevation = 0.dp) {
                    val items = listOf("الرئيسية", "قوالبي", "المحفوظات", "الإعدادات")
                    val icons = listOf(Icons.Default.Home, Icons.Default.Dashboard, Icons.Default.BookmarkBorder, Icons.Default.Settings)
                    items.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index && !editorOpen,
                            onClick = { selectedTab = index; editorOpen = false },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan,
                                indicatorColor = Color(0x333D3A6B), unselectedIconColor = Muted, unselectedTextColor = Muted)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier, selectedCategory: String, onCategory: (String) -> Unit,
    templateQuery: String, onTemplateQuery: (String) -> Unit, accents: List<Color>, selectedColor: Int,
    hasDraft: Boolean, onRestoreDraft: () -> Unit, onNew: () -> Unit, onTemplate: (String) -> Unit
) {
    val categories = listOf("الكل", "حالات", "اقتباسات", "تهاني", "أعمال")
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مساحة إبداعك", color = Muted, fontSize = 12.sp)
                        Text("ابدأ تصميمك القادم", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("إنشاء تصميم جديد", fontWeight = FontWeight.Bold)
                }
                if (hasDraft) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRestoreDraft, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Restore, null, tint = Cyan); Spacer(Modifier.width(8.dp))
                        Text("استعادة آخر مسودة", color = Cyan)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = templateQuery, onValueChange = onTemplateQuery,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("ابحث عن قالب...", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Cyan) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF30374C),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = Cyan, focusedContainerColor = SurfaceGlass, unfocusedContainerColor = SurfaceGlass
            )
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("قوالب مختارة", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            Text("اكتشف المزيد", fontSize = 12.sp, color = Cyan)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val active = selectedCategory == category
                Box(Modifier.clip(CircleShape).background(if (active) Purple.copy(alpha = .24f) else SurfaceGlass)
                    .border(1.dp, if (active) Purple else Color(0xFF293149), CircleShape)
                    .clickable { onCategory(category) }.padding(horizontal = 14.dp, vertical = 9.dp)) {
                    Text(category, color = if (active) Color.White else Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val templates = listOf(
            DesignTemplate("صباح الخير", "يوم جديد، فرصة جديدة", accents[0], "حالات"),
            DesignTemplate("كن مختلفًا", "اترك أثرًا جميلًا", accents[1], "اقتباسات"),
            DesignTemplate("كل عام وأنتم بخير", "أيامكم فرح وسلام", accents[2], "تهاني"),
            DesignTemplate("ابدأ من جديد", "خطوة صغيرة تصنع الفرق", accents[3], "اقتباسات"),
            DesignTemplate("نجاحك يبدأ الآن", "خطط بذكاء وابدأ بخطوة", accents[0], "أعمال"),
            DesignTemplate("مساء الورد", "راحة وطمأنينة لقلبك", accents[2], "حالات"),
            DesignTemplate("ألف مبروك", "فرحتكم تكمل فرحتنا", accents[1], "تهاني"),
            DesignTemplate("فكرة تصنع فرقًا", "ابتكر شيئًا يستحق المشاركة", accents[3], "أعمال")
        ).filter { (selectedCategory == "الكل" || it.category == selectedCategory) &&
            (templateQuery.isBlank() || it.title.contains(templateQuery.trim(), ignoreCase = true) || it.subtitle.contains(templateQuery.trim(), ignoreCase = true)) }
        if (templates.isEmpty()) {
            Text("لا توجد قوالب في هذا التصنيف حاليًا.", color = Muted, modifier = Modifier.padding(vertical = 20.dp))
        }
        templates.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    Column(Modifier.weight(1f).padding(bottom = 12.dp)) {
                        Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(item.color, Color(0xFF171B31))))
                            .clickable { onTemplate(item.title) }.padding(14.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(item.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(5.dp))
                                Text(item.subtitle, color = Color.White.copy(alpha = .8f), fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("قالب قابل للتعديل", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("صمّم، احفظ، وشارك إبداعك", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun EditorScreen(
    modifier: Modifier = Modifier, title: String, onTitle: (String) -> Unit, subtitle: String, onSubtitle: (String) -> Unit,
    accents: List<Color>, selectedColor: Int, onColor: (Int) -> Unit,
    extraTexts: List<String>, onExtraTexts: (List<String>) -> Unit,
    elementXs: List<Float>, elementYs: List<Float>, elementSizes: List<Float>, elementColors: List<Int>, elementOrder: List<Int>,
    shapes: List<ShapeLayer>, selectedShape: Int, onAddShape: (String) -> Unit, onMoveShape: (Int, Float, Float) -> Unit,
    onShapeSize: (Int, Float) -> Unit, onShapeColor: (Int, Int) -> Unit, onRemoveShape: (Int) -> Unit,
    selectedElement: Int, onSelectElement: (Int) -> Unit, onMoveElement: (Int, Float, Float) -> Unit,
    onSizeElement: (Int, Float) -> Unit, onColorElement: (Int, Int) -> Unit, onLayerElement: (Int, Int) -> Unit,
    canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit, onBack: () -> Unit,
    onSave: () -> Unit, onExportPng: () -> Unit, onExportPdf: () -> Unit
) {
    LaunchedEffect(extraTexts.size) { if (selectedElement >= 2 + extraTexts.size) onSelectElement((1 + extraTexts.size).coerceAtLeast(0)) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null); Text("العودة للقوالب") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.Default.Undo, "تراجع", tint = if (canUndo) Cyan else Muted) }
            IconButton(onClick = onRedo, enabled = canRedo) { Icon(Icons.Default.Redo, "إعادة", tint = if (canRedo) Cyan else Muted) }
        }
        val context = LocalContext.current
    val canvasTexts = listOf(title, subtitle) + extraTexts.take(4)

        val defaultSizes = listOf(28f, 15f, 13f, 13f, 13f, 13f)
        val defaultYs = listOf(.392f, .533f, .615f, .668f, .721f, .774f)
        val visibleOrder = (elementOrder + canvasTexts.indices.filterNot { it in elementOrder }).filter { it in canvasTexts.indices }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(accents[selectedColor], Color(0xFF121729), Color(0xFF080B14))))
            .border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(26.dp))) {
            val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            Icon(Icons.Default.AutoAwesome, null, tint = Color.White.copy(alpha = .45f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(24.dp))
            val photoSize = (maxWidth * imageSize).coerceIn(48.dp, 150.dp)
            photoBitmap?.let { photo -> Image(bitmap = photo, contentDescription = "صورة التصميم", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.offset { IntOffset((imageX * canvasWidth - photoSize.toPx() / 2f).roundToInt(), (imageY * canvasHeight - photoSize.toPx() / 2f).roundToInt()) }.size(photoSize).clip(CircleShape).border(2.dp, Color.White.copy(alpha = .8f), CircleShape).pointerInput(imageX, imageY, photoSize) { detectDragGestures { change, dragAmount -> change.consume(); onMoveImage(dragAmount.x / canvasWidth, dragAmount.y / canvasHeight) } }) }
            visibleOrder.forEach { index ->
                val value = canvasTexts[index].ifBlank { if (index == 0) "عنوان تصميمك" else if (index == 1) "أضف عبارة تلهم الآخرين" else "عنصر نصي ${index - 1}" }
                val palette = listOf(Color.White, Cyan, Color(0xFFFFD166), Color(0xFFFF8FAB))
                Text(value, modifier = Modifier.fillMaxWidth(.8f)
                    .offset { IntOffset(((elementXs.getOrElse(index) { .5f } * canvasWidth) - canvasWidth * .4f).roundToInt(), (elementYs.getOrElse(index) { defaultYs[index.coerceAtMost(5)] } * canvasHeight).roundToInt()) }
                    .pointerInput(index, elementXs, elementYs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onSelectElement(index)
                            onMoveElement(index, dragAmount.x / canvasWidth, dragAmount.y / canvasHeight)
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedElement == index) Color.White.copy(alpha = .10f) else Color.Transparent)
                    .border(if (selectedElement == index) 1.dp else 0.dp, if (selectedElement == index) Cyan.copy(alpha = .8f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelectElement(index) }.padding(horizontal = 4.dp, vertical = 3.dp),
                    color = palette[elementColors.getOrElse(index) { 0 }.coerceIn(0, palette.lastIndex)],
                    fontSize = (elementSizes.getOrElse(index) { defaultSizes[index.coerceAtMost(5)] }).sp,
                    fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.Medium,
                    textAlign = TextAlign.Center, lineHeight = (elementSizes.getOrElse(index) { defaultSizes[index.coerceAtMost(5)] } * 1.2f).sp
                )
            }
            Text("HOMI DESIGN", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                color = Color.White.copy(alpha = .45f), fontSize = 9.sp, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("أدوات العنصر المحدد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(when (selectedElement) { 0 -> "العنوان"; 1 -> "النص الفرعي"; else -> "النص الإضافي ${selectedElement - 1}" }, color = Cyan, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الحجم", color = Muted, fontSize = 12.sp)
                    Slider(value = elementSizes.getOrElse(selectedElement) { defaultSizes[selectedElement.coerceAtMost(5)] },
                        onValueChange = { onSizeElement(selectedElement, it) }, valueRange = 10f..48f,
                        modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Purple))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("اللون", color = Muted, fontSize = 12.sp)
                    listOf(Color.White, Cyan, Color(0xFFFFD166), Color(0xFFFF8FAB)).forEachIndexed { index, color ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(color)
                            .border(if (elementColors.getOrElse(selectedElement) { 0 } == index) 3.dp else 0.dp, Purple, CircleShape)
                            .clickable { onColorElement(selectedElement, index) })
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onLayerElement(selectedElement, -1) }) { Icon(Icons.Default.ArrowDownward, "إلى الخلف", tint = Cyan) }
                    IconButton(onClick = { onLayerElement(selectedElement, 1) }) { Icon(Icons.Default.ArrowUpward, "إلى الأمام", tint = Cyan) }
                }
                Text("اسحب النص داخل اللوحة لتحريك موضعه. اختر النص أولًا لتعديل حجمه ولونه وترتيبه.", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.height(16.dp))
        Text("الأشكال", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("circle" to "دائرة", "rect" to "مستطيل", "line" to "خط").forEach { (type, label) ->
                        OutlinedButton(onClick = { onAddShape(type) }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 12.sp) }
                    }
                }
                if (shapes.isNotEmpty()) {
                    Text("حرّك الشكل داخل اللوحة لتحديده؛ يمكن تغيير حجمه ولونه أو حذفه.", color = Muted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("حجم", color = Muted, fontSize = 12.sp)
                        Slider(value = shapes.getOrNull(selectedShape.coerceAtLeast(0))?.size ?: .12f,
                            onValueChange = { if (selectedShape in shapes.indices) onShapeSize(selectedShape, it) },
                            valueRange = .04f.. .32f, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (selectedShape in shapes.indices) onRemoveShape(selectedShape) }) { Icon(Icons.Default.DeleteOutline, "حذف الشكل", tint = Color(0xFFFF8FAB)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Color.White, Cyan, Color(0xFFFFD166), Color(0xFFFF8FAB)).forEachIndexed { idx, col ->
                            Box(Modifier.size(28.dp).clip(CircleShape).background(col).clickable { if (selectedShape in shapes.indices) onShapeColor(selectedShape, idx) })
                        }
                    }
                }
                Text("اسحب الأشكال والنصوص بحرية داخل إطار التصميم.", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("محتوى التصميم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("العنوان", color = Muted, fontSize = 12.sp)
                StyledField(title, onTitle, "اكتب عنوانًا جذابًا")
                HorizontalDivider(color = Color(0xFF30374C))
                Spacer(Modifier.height(8.dp))
                Text("النص الفرعي", color = Muted, fontSize = 12.sp)
                StyledField(subtitle, onSubtitle, "أضف وصفًا قصيرًا")
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("عناصر نصية إضافية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (extraTexts.size < 4) onExtraTexts(extraTexts + "") }) {
                Icon(Icons.Default.Add, null, tint = Cyan); Text("إضافة نص", color = Cyan)
            }
        }
        if (extraTexts.isEmpty()) Text("أضف عناوين أو عبارات مستقلة إلى تصميمك.", color = Muted, fontSize = 12.sp)
        extraTexts.forEachIndexed { index, text ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("العنصر ${index + 1}", color = Muted, fontSize = 11.sp)
                        StyledField(text, { value -> onExtraTexts(extraTexts.toMutableList().also { it[index] = value }) }, "اكتب نص العنصر")
                    }
                    IconButton(onClick = { onExtraTexts(extraTexts.toMutableList().also { it.removeAt(index) }) }) {
                        Icon(Icons.Default.DeleteOutline, "حذف العنصر", tint = Color(0xFFFF718B))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("اختر لوحة الألوان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            accents.forEachIndexed { index, color ->
                Box(Modifier.size(44.dp).clip(CircleShape).background(color)
                    .border(if (selectedColor == index) 3.dp else 0.dp, Color.White, CircleShape)
                    .clickable { onColor(index) }, contentAlignment = Alignment.Center) {
                    if (selectedColor == index) Icon(Icons.Default.Check, null, tint = Color.White)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("حفظ التصميم")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onExportPng, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text("تصدير PNG")
            }
            OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(5.dp)); Text("تصدير PDF")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StyledField(value: String, onValue: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value, onValueChange = onValue, singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
        cursorBrush = SolidColor(Cyan),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = Muted, fontSize = 14.sp)
                inner()
            }
        }
    )
}

@Composable
private fun SimpleState(modifier: Modifier = Modifier, title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(26.dp)).background(Purple.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp))
        .background(Color(0xAA171D2E))
        .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(24.dp)), content = content)
}


@Composable
private fun SavedDesignsScreen(
    modifier: Modifier = Modifier,
    items: List<DesignItem>,
    onOpen: (DesignItem) -> Unit,
    onDelete: (DesignItem) -> Unit
) {
    if (items.isEmpty()) {
        SimpleState(modifier, "لا توجد تصاميم محفوظة", "افتح محرر التصميم واضغط «حفظ التصميم» لتظهر أعمالك هنا.", Icons.Default.Dashboard)
        return
    }
    val palette = listOf(Color(0xFF7657FF), Color(0xFF087E8B), Color(0xFFB54772), Color(0xFFCA8A04))
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("تصاميمك على هذا الجهاز", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(palette[item.colorIndex.coerceIn(0, 3)], Background))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title.ifBlank { "بدون عنوان" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                        Text(item.subtitle, color = Muted, fontSize = 12.sp, maxLines = 2)
                    }
                    IconButton(onClick = { onOpen(item) }) { Icon(Icons.Default.Edit, "تعديل", tint = Cyan) }
                    IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Color(0xFFFF718B)) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}
