package com.hjoozati.experimental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjoozati.experimental.data.AppDatabase
import com.hjoozati.experimental.data.Booking
import com.hjoozati.experimental.data.BookingDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Night = Color(0xFF07050D)
private val Glass = Color(0xFF171020)
private val Glass2 = Color(0xFF21142F)
private val Violet = Color(0xFFB64CFF)
private val Violet2 = Color(0xFF7425C8)
private val Gold = Color(0xFFFFC65C)
private val Soft = Color(0xFFB9ADC8)
private val Mint = Color(0xFF62E0B2)
private val Danger = Color(0xFFFF6B8B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = AppDatabase.get(this).bookingDao()
        setContent {
            val vm: ExperimentalViewModel = viewModel(factory = ExperimentalViewModel.Factory(dao))
            ExperimentalApp(vm)
        }
    }
}

private class ExperimentalViewModel(private val dao: BookingDao) : ViewModel() {
    val bookings = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var notice by mutableStateOf<String?>(null)
        private set
    fun clearNotice() { notice = null }
    fun add(name: String, phone: String, date: String, start: String, end: String, teamA: String, teamB: String, price: Long, paid: Long) = viewModelScope.launch {
        val startMin = parseMinutes(start)
        val endMin = parseMinutes(end)
        if (name.isBlank() || date.isBlank() || startMin == null || endMin == null || startMin >= endMin || price < 0 || paid < 0) { notice = "تحقق من بيانات الحجز والوقت بصيغة HH:mm"; return@launch }
        val conflict = dao.findOverlap(date, startMin, endMin, 0)
        if (conflict != null) { notice = "يوجد تعارض مع حجز ${conflict.customerName}"; return@launch }
        dao.insert(Booking(customerName=name, phone=phone, date=date, startTime=start, endTime=end, startMinutes=startMin, endMinutes=endMin, teamOne=teamA, teamTwo=teamB, pricePerTeam=price, paid=paid.coerceAtMost(price*2)))
        notice = "تم حفظ الحجز بنجاح"
    }
    fun cancel(item: Booking) = viewModelScope.launch { dao.update(item.copy(status="ملغي")); notice = "تم إلغاء الحجز" }
    class Factory(private val dao: BookingDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ExperimentalViewModel(dao) as T
    }
}

@Composable
private fun ExperimentalApp(vm: ExperimentalViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.notice) { vm.notice?.let { snack.showSnackbar(it); vm.clearNotice() } }
    MaterialTheme(colorScheme = darkColorScheme(primary=Violet, secondary=Gold, background=Night, surface=Glass, onSurface=Color.White)) {
        Scaffold(containerColor=Night, snackbarHost={ SnackbarHost(snack) }, bottomBar={
            NavigationBar(containerColor=Color(0xEE120C1B), contentColor=Soft, tonalElevation=0.dp) {
                val nav = listOf(Triple("الرئيسية", Icons.Default.Home, 0), Triple("الحجوزات", Icons.Default.CalendarMonth, 1), Triple("البطولات", Icons.Default.EmojiEvents, 2), Triple("التحليلات", Icons.Default.Insights, 3), Triple("المزيد", Icons.Default.GridView, 4))
                nav.forEach { (label, icon, index) -> NavigationBarItem(selected=tab==index, onClick={tab=index}, icon={Icon(icon, null)}, label={Text(label, fontSize=10.sp)}, colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold, selectedTextColor=Gold, indicatorColor=Violet2.copy(alpha=.28f), unselectedIconColor=Soft, unselectedTextColor=Soft)) }
            }
        }, floatingActionButton={ if (tab==0 || tab==1) FloatingActionButton(onClick={showAdd=true}, containerColor=Gold, contentColor=Night, shape=CircleShape) { Icon(Icons.Default.Add, "إضافة حجز") } }) { padding ->
            when(tab) {
                0 -> HomeScreen(bookings, Modifier.padding(padding), onAdd={showAdd=true}, onPreview={preview=it})
                1 -> BookingListScreen(bookings, Modifier.padding(padding), onCancel=vm::cancel)
                2 -> FeatureScreen("مركز البطولات", "إدارة المنافسات من مرحلة المجموعات حتى منصة التتويج", listOf("إنشاء بطولة وتقسيم الفرق تلقائياً", "جدول مباريات وشجرة خروج المغلوب", "ترتيب ونقاط وفارق أهداف", "الهداف وأفضل لاعب وحارس", "أرشيف الأبطال والوصيف"), Modifier.padding(padding), onPreview={preview=it})
                3 -> AnalyticsScreen(bookings, Modifier.padding(padding), onPreview={preview=it})
                else -> MoreScreen(Modifier.padding(padding), onPreview={preview=it})
            }
            if(showAdd) AddBookingDialog(onDismiss={showAdd=false}, onSave={n,p,d,s,e,a,b,price,paid -> vm.add(n,p,d,s,e,a,b,price,paid); showAdd=false})
            preview?.let { title -> AlertDialog(onDismissRequest={preview=null}, containerColor=Glass2, title={Text(title, color=Gold, fontWeight=FontWeight.Bold)}, text={Text("هذه ميزة ضمن النسخة التجريبية. الواجهة والتدفق الأولي قيد التجربة، وسيتم ربط الوظائف والبيانات الفعلية على مراحل.", color=Color.White)}, confirmButton={TextButton(onClick={preview=null}){Text("حسناً", color=Gold)}}) }
        }
    }
}

@Composable
private fun HomeScreen(bookings: List<Booking>, modifier: Modifier, onAdd:()->Unit, onPreview:(String)->Unit) {
    val today=LocalDate.now().toString()
    val active=bookings.filter{it.date==today && it.status!="ملغي"}
    val income=active.sumOf{it.paid}
    val upcoming=bookings.filter{it.date>=today && it.status!="ملغي"}.take(3)
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp), contentPadding=PaddingValues(top=28.dp,bottom=28.dp), verticalArrangement=Arrangement.spacedBy(17.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Column { Text("مساحة الإدارة الذكية", color=Soft, fontSize=12.sp); Text("حجوزاتي", color=Color.White, fontSize=30.sp, fontWeight=FontWeight.Black); Text("EXPERIMENTAL  •  PROTOTYPE", color=Gold, fontSize=9.sp, letterSpacing=2.sp) }
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(Brush.linearGradient(listOf(Violet, Color(0xFF32134F), Gold))).border(1.dp, Gold.copy(.7f), RoundedCornerShape(19.dp)), contentAlignment=Alignment.Center) { Icon(Icons.Default.SportsSoccer, null, tint=Color.White, modifier=Modifier.size(31.dp)) }
        } }
        item { GlassPanel(Modifier.fillMaxWidth(), accent=Violet) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Column { Text("نظرة اليوم", color=Soft, fontSize=12.sp); Text(LocalDate.now().format(DateTimeFormatter.ofPattern("dd / MM / yyyy")), color=Color.White, fontSize=18.sp, fontWeight=FontWeight.Bold) }; Box(Modifier.clip(CircleShape).background(Gold.copy(.13f)).padding(horizontal=12.dp, vertical=7.dp)){Text("LIVE", color=Gold, fontSize=10.sp, fontWeight=FontWeight.Bold)} }
            Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) { Metric("حجوزات اليوم", active.size.toString(), Violet, Modifier.weight(1f)); Metric("المقبوض", money(income), Gold, Modifier.weight(1f)); Metric("إجمالي الحجوزات", bookings.size.toString(), Mint, Modifier.weight(1f)) }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Text("الوصول السريع", color=Color.White, fontWeight=FontWeight.Bold, fontSize=18.sp); Text("مختبر الميزات", color=Gold, fontSize=11.sp) } }
        item { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { FeatureTile("مساعد HJ AI", "تحليل وأوامر عربية", Icons.Default.AutoAwesome, Violet, Modifier.weight(1f)){onPreview("مساعد HJ AI")}; FeatureTile("إدارة البطولة", "فرق ومجموعات ونتائج", Icons.Default.EmojiEvents, Gold, Modifier.weight(1f)){onPreview("إدارة البطولة")} }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { FeatureTile("بطاقة العميل", "عضوية ونقاط وQR", Icons.Default.CardMembership, Mint, Modifier.weight(1f)){onPreview("بطاقات العملاء والولاء")}; FeatureTile("التقارير الذكية", "إيرادات وتوقعات", Icons.Default.QueryStats, Violet, Modifier.weight(1f)){onPreview("التقارير الذكية")} }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Text("المواعيد القادمة", color=Color.White, fontWeight=FontWeight.Bold, fontSize=18.sp); TextButton(onClick=onAdd){Text("+ حجز", color=Gold)} } }
        if(upcoming.isEmpty()) item { GlassPanel(Modifier.fillMaxWidth()) { Text("لا توجد حجوزات قادمة", color=Color.White, fontWeight=FontWeight.Bold); Text("ابدأ بإضافة أول موعد للملعب", color=Soft, fontSize=12.sp); OutlinedButton(onClick=onAdd){Text("إضافة حجز", color=Gold)} } }
        items(upcoming, key={it.id}) { item -> BookingGlassCard(item) }
        item { Text("نسخة تجريبية مستقلة — لا تؤثر على التطبيق الأساسي", color=Soft.copy(.7f), fontSize=10.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth().padding(top=8.dp)) }
    }
}

@Composable private fun Metric(label:String, value:String, tint:Color, modifier:Modifier=Modifier) { Column(modifier) { Text(label, color=Soft, fontSize=10.sp); Spacer(Modifier.height(5.dp)); Text(value, color=tint, fontSize=15.sp, fontWeight=FontWeight.ExtraBold, maxLines=1) } }

@Composable private fun GlassPanel(modifier:Modifier=Modifier, accent:Color=Violet, content:@Composable ColumnScope.()->Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xCC28183A), Color(0xD9100C18), Color(0xCC21152C)))).border(1.dp, Brush.linearGradient(listOf(accent.copy(.65f), Color.White.copy(.12f), Gold.copy(.25f))), RoundedCornerShape(24.dp)).padding(17.dp), verticalArrangement=Arrangement.spacedBy(9.dp), content=content)
}

@Composable private fun FeatureTile(title:String, subtitle:String, icon:androidx.compose.ui.graphics.vector.ImageVector, tint:Color, modifier:Modifier=Modifier, onClick:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Glass2, Glass, Color(0xFF100B17)))).border(1.dp, tint.copy(.35f), RoundedCornerShape(20.dp)).clickable(onClick=onClick).padding(14.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(.14f)), contentAlignment=Alignment.Center){Icon(icon,null,tint=tint)}
        Text(title, color=Color.White, fontWeight=FontWeight.Bold, fontSize=13.sp); Text(subtitle, color=Soft, fontSize=10.sp)
    }
}

@Composable private fun BookingListScreen(bookings:List<Booking>, modifier:Modifier, onCancel:(Booking)->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp), contentPadding=PaddingValues(top=28.dp,bottom=28.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("الحجوزات", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Black); Text("إدارة المواعيد وحالة كل حجز", color=Soft, fontSize=12.sp) }
        if(bookings.isEmpty()) item { GlassPanel(Modifier.fillMaxWidth()){Text("لا توجد حجوزات بعد", color=Color.White);Text("استخدم زر + لإضافة حجز تجريبي", color=Soft, fontSize=12.sp)} }
        items(bookings, key={it.id}) { b -> Column { BookingGlassCard(b); if(b.status!="ملغي") TextButton(onClick={onCancel(b)}, modifier=Modifier.align(Alignment.End)){Text("إلغاء الحجز", color=Danger)} } }
    }
}

@Composable private fun BookingGlassCard(b:Booking) {
    GlassPanel(Modifier.fillMaxWidth(), accent=if(b.status=="ملغي") Danger else Violet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(b.customerName, color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp);Text("${b.teamOne.ifBlank{"الفريق الأول"}}  ×  ${b.teamTwo.ifBlank{"الفريق الثاني"}}", color=Soft, fontSize=11.sp)}; Text(b.status, color=if(b.status=="ملغي")Danger else Mint, fontSize=11.sp) }
        HorizontalDivider(color=Color.White.copy(.09f)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){Text("${b.date}  •  ${b.startTime} - ${b.endTime}", color=Gold, fontSize=11.sp);Text("${money(b.total)} د.ع", color=Color.White, fontWeight=FontWeight.Bold, fontSize=12.sp)}
        Text("المدفوع ${money(b.paid)}  •  المتبقي ${money(b.remaining)} د.ع", color=Soft, fontSize=10.sp)
    }
}

@Composable private fun AnalyticsScreen(bookings:List<Booking>, modifier:Modifier, onPreview:(String)->Unit) {
    val paid=bookings.sumOf{it.paid}; val due=bookings.sumOf{it.remaining}; val active=bookings.count{it.status!="ملغي"}
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp), contentPadding=PaddingValues(top=28.dp,bottom=28.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("مركز التحليلات", color=Color.White, fontSize=27.sp, fontWeight=FontWeight.Black); Text("قراءة أولية لأداء الملعب", color=Soft, fontSize=12.sp) }
        item { GlassPanel(Modifier.fillMaxWidth(), Gold){ Text("ملخص مالي", color=Gold, fontWeight=FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){Metric("المقبوض",money(paid),Mint,Modifier.weight(1f));Metric("المتبقي",money(due),Gold,Modifier.weight(1f));Metric("النشطة",active.toString(),Violet,Modifier.weight(1f))} } }
        item { FeatureTile("تحليل أوقات الذروة", "اكتشاف الساعات الأكثر طلباً", Icons.Default.Timeline, Violet, Modifier.fillMaxWidth()){onPreview("تحليل أوقات الذروة")} }
        item { FeatureTile("توقع الإيرادات", "توقعات مبنية على سجل الحجوزات", Icons.Default.TrendingUp, Gold, Modifier.fillMaxWidth()){onPreview("توقع الإيرادات")} }
        item { FeatureTile("تقارير PDF و Excel", "تصدير كشوفات الإدارة", Icons.Default.FileDownload, Mint, Modifier.fillMaxWidth()){onPreview("تصدير التقارير")} }
        item { Text("الأرقام أعلاه محسوبة من بيانات الحجوزات المحلية الحالية.", color=Soft, fontSize=10.sp) }
    }
}

@Composable private fun FeatureScreen(title:String, subtitle:String, features:List<String>, modifier:Modifier, onPreview:(String)->Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp), contentPadding=PaddingValues(top=28.dp,bottom=30.dp), verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item { Text(title, color=Color.White, fontSize=27.sp, fontWeight=FontWeight.Black); Text(subtitle, color=Soft, fontSize=12.sp) }
        item { GlassPanel(Modifier.fillMaxWidth(), Gold){Text("TOURNAMENT LAB", color=Gold, fontSize=10.sp, letterSpacing=2.sp);Text("منظم البطولة", color=Color.White, fontSize=21.sp, fontWeight=FontWeight.Bold);Text("من التسجيل إلى التتويج في مساحة واحدة", color=Soft, fontSize=11.sp)} }
        items(features){ f -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Glass).border(1.dp,Violet.copy(.22f),RoundedCornerShape(17.dp)).clickable{onPreview(f)}.padding(15.dp), verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AutoAwesome,null,tint=Gold);Spacer(Modifier.width(12.dp));Text(f,color=Color.White,fontSize=13.sp,modifier=Modifier.weight(1f));Icon(Icons.Default.ChevronLeft,null,tint=Soft)} }
    }
}

@Composable private fun MoreScreen(modifier:Modifier, onPreview:(String)->Unit) {
    val options=listOf("دفتر الملعب المتقدم" to Icons.Default.MenuBook,"الأمان والبصمة" to Icons.Default.Fingerprint,"النسخ الاحتياطي المشفر" to Icons.Default.CloudSync,"الموظفون والصلاحيات" to Icons.Default.Groups,"المخزون والصيانة" to Icons.Default.Build,"العروض والاشتراكات" to Icons.Default.CardGiftcard,"إعدادات الهوية الزجاجية" to Icons.Default.Palette,"استيراد وتصدير البيانات" to Icons.Default.SwapHoriz)
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp), contentPadding=PaddingValues(top=28.dp,bottom=30.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("المزيد", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Black); Text("مختبر الأدوات والإعدادات المتقدمة", color=Soft, fontSize=12.sp) }
        items(options){ (label,icon) -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass).border(1.dp,Gold.copy(.16f),RoundedCornerShape(18.dp)).clickable{onPreview(label)}.padding(15.dp), verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Gold);Spacer(Modifier.width(12.dp));Text(label,color=Color.White,modifier=Modifier.weight(1f),fontSize=13.sp);Icon(Icons.Default.ChevronLeft,null,tint=Soft)} }
    }
}

@Composable private fun AddBookingDialog(onDismiss:()->Unit, onSave:(String,String,String,String,String,String,String,Long,Long)->Unit) {
    var name by remember{mutableStateOf("")}; var phone by remember{mutableStateOf("")}; var date by remember{mutableStateOf(LocalDate.now().toString())}; var start by remember{mutableStateOf("18:00")}; var end by remember{mutableStateOf("19:00")}; var a by remember{mutableStateOf("")}; var b by remember{mutableStateOf("")}; var price by remember{mutableStateOf("25000")}; var paid by remember{mutableStateOf("0")}
    AlertDialog(onDismissRequest=onDismiss, containerColor=Glass2, title={Text("حجز جديد",color=Gold,fontWeight=FontWeight.Bold)}, text={
        Column(Modifier.heightIn(max=490.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it},label={Text("اسم الزبون")},singleLine=true)
            OutlinedTextField(phone,{phone=it},label={Text("رقم الهاتف")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
            OutlinedTextField(date,{date=it},label={Text("التاريخ YYYY-MM-DD")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(start,{start=it},label={Text("من")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(end,{end=it},label={Text("إلى")},modifier=Modifier.weight(1f),singleLine=true)}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(a,{a=it},label={Text("الفريق الأول")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(b,{b=it},label={Text("الفريق الثاني")},modifier=Modifier.weight(1f),singleLine=true)}
            OutlinedTextField(price,{price=it},label={Text("السعر لكل فريق بالدينار")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            OutlinedTextField(paid,{paid=it},label={Text("المبلغ المدفوع")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        }
    }, confirmButton={TextButton(onClick={onSave(name,phone,date,start,end,a,b,price.toLongOrNull()?:0L,paid.toLongOrNull()?:0L)}){Text("حفظ",color=Gold)}}, dismissButton={TextButton(onClick=onDismiss){Text("إلغاء",color=Soft)}})
}

internal fun parseMinutes(value: String): Int? {
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun money(value:Long):String = "%,d".format(value).replace(",", "٬")
