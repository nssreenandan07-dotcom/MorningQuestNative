package com.morningquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import android.content.Context
import com.google.gson.Gson
import java.time.LocalDate
import kotlin.random.Random

private val BG = Color(0xFF0C0A08)
private val CARD = Color(0xFF1A1612)
private val INK = Color(0xFFF6EFE3)
private val MUTED = Color(0xFFA79C8A)
private val AMBER = Color(0xFFFF9D2E)
private val GOLD = Color(0xFFFFD27A)
private val LINE = Color(0x24FFFFFF)

 data class QuestTask(val id: Long, val name: String, val time: String, val minutes: Int, val done: Boolean = false)
 data class Reward(val name: String, val cost: Int)
 data class DayRecord(val date: String, val done: Int, val total: Int, val skipped: List<String> = emptyList())
 data class GameState(
    var xp: Int = 0, var coins: Int = 0, var streak: Int = 0, var lastFinish: String = "", var day: String = LocalDate.now().toString(),
    var tasks: MutableList<QuestTask> = mutableListOf(), var history: MutableList<DayRecord> = mutableListOf(), var badges: MutableSet<String> = mutableSetOf(),
    var pet: String = "plant", var growth: Int = 0, var themes: MutableSet<String> = mutableSetOf("Sunrise", "Ocean"), var theme: String = "Sunrise",
    var rewards: MutableList<Reward> = mutableListOf(), var loot: Int = 0, var bosses: Int = 0, var combo: Int = 0, var lastTaskAt: Long = 0,
    var reminder: String = "07:00", var customTemplates: MutableMap<String, MutableList<QuestTask>> = mutableMapOf(), var feeds: Int = 0, var feedDay: String = ""
 )

private val templates = linkedMapOf(
 "Student" to listOf(Triple("Wake up & drink water", "06:30", 2), Triple("Stretch", "06:35", 5), Triple("Review today’s classes", "06:45", 10), Triple("Pack your bag", "07:00", 5)),
 "Fitness" to listOf(Triple("Drink water", "06:00", 2), Triple("Warm-up", "06:05", 5), Triple("Workout", "06:15", 20), Triple("Healthy breakfast", "06:45", 15)),
 "Productive" to listOf(Triple("Make your bed", "06:30", 3), Triple("Meditate", "06:35", 5), Triple("Plan the day", "06:45", 10), Triple("Most important task", "07:00", 25)),
 "Mindful" to listOf(Triple("Drink warm water", "06:30", 2), Triple("Breathing", "06:35", 5), Triple("Gratitude journal", "06:42", 8), Triple("Slow stretch", "06:52", 10)),
 "Creative" to listOf(Triple("Free-write", "06:30", 10), Triple("Sketch something", "06:45", 15), Triple("Collect 3 ideas", "07:05", 5), Triple("Make one small thing", "07:15", 20))
)
private val themeColors = mapOf("Sunrise" to AMBER, "Ocean" to Color(0xFF2B9CF0), "Forest" to Color(0xFF2FA84F), "Grape" to Color(0xFFA24BD6))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MorningQuestApp(this) } }
}

@Composable private fun MorningQuestApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("mq_native", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    var state by remember { mutableStateOf(runCatching { gson.fromJson(prefs.getString("state", null), GameState::class.java) }.getOrNull() ?: GameState()) }
    var revision by remember { mutableIntStateOf(0) }
    val observedRevision = revision
    fun save() { prefs.edit().putString("state", gson.toJson(state)).apply(); revision++ }
    var tab by remember { mutableStateOf("Today") }
    var newName by remember { mutableStateOf("") }; var newTime by remember { mutableStateOf("07:00") }; var newMin by remember { mutableStateOf("5") }
    var rewardName by remember { mutableStateOf("") }; var rewardCost by remember { mutableStateOf("") }; var templateName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }; var timerTask by remember { mutableStateOf<Long?>(null) }; var timerEnd by remember { mutableLongStateOf(0L) }
    val accent = themeColors[state.theme] ?: AMBER
    fun notify(msg: String) { message = msg }
    fun useTemplate(name: String) { val rows = templates[name] ?: state.customTemplates[name]?.map { Triple(it.name,it.time,it.minutes) } ?: return; state.tasks = rows.map { QuestTask(System.nanoTime()+Random.nextLong(1000),it.first,it.second,it.third) }.toMutableList(); state.tasks.sortBy { it.time }; save(); notify("$name routine ready ✨") }
    fun complete(task: QuestTask) {
        if (task.done) return
        val now = System.currentTimeMillis(); state.combo = if (now-state.lastTaskAt < 600000) state.combo+1 else 1; state.lastTaskAt=now
        val bonus=(state.combo.coerceAtMost(5)-1)*3; state.xp += 10+bonus; state.coins += 5
        state.tasks = state.tasks.map { if(it.id==task.id) it.copy(done=true) else it }.toMutableList()
        if(state.tasks.isNotEmpty() && state.tasks.all{it.done}) {
            val yesterday=LocalDate.now().minusDays(1).toString(); state.streak=if(state.lastFinish==yesterday) state.streak+1 else if(state.lastFinish==LocalDate.now().toString()) state.streak else 1
            state.lastFinish=LocalDate.now().toString(); state.growth++; state.loot++; state.xp+=30; state.coins+=20; state.bosses++
            state.badges.addAll(listOf("🎉 First win","⚔️ Boss slayer")); if(state.streak>=3)state.badges.add("🔥 3-day streak"); if(state.streak>=7)state.badges.add("🏆 Week warrior"); if(java.time.LocalTime.now().hour<8)state.badges.add("🌅 Early bird")
            notify("Routine complete! Boss defeated · loot earned 🎁")
        } else notify("Task complete · +${10+bonus} XP · +5 coins")
        if(state.xp>=400)state.badges.add("⭐ Level 5"); if(state.coins>=200)state.badges.add("💰 Saver")
        save()
    }
    fun rollLoot() { if(state.loot<=0)return; state.loot--; when(Random.nextInt(10)){ in 0..4->{val c=Random.nextInt(10,51);state.coins+=c;notify("Loot box: +$c coins 💰")}; in 5..8->{val x=Random.nextInt(20,61);state.xp+=x;notify("Loot box: +$x XP ✨")};else->{state.growth++;notify("Your companion grew! 🌟")} }; save() }
    fun addTask() { if(newName.isBlank()){notify("Enter a task name first");return};state.tasks.add(QuestTask(System.nanoTime(),newName.trim(),newTime,newMin.toIntOrNull()?.coerceAtLeast(1)?:5));state.tasks.sortBy{it.time};newName="";save() }
    fun feed() { val today=LocalDate.now().toString(); if(state.feedDay!=today){state.feedDay=today;state.feeds=0}; if(state.feeds>=3)notify("All full for today!") else if(state.coins<5)notify("You need 5 coins") else {state.coins-=5;state.feeds++;state.growth++;save();notify(if(state.pet=="plant")"Watered! Growth +1 🌱" else "Fed! Growth +1 🍎")} }

    MaterialTheme(colorScheme = darkColorScheme(background=BG,surface=CARD,primary=accent,onPrimary=Color(0xFF1A0E02),onBackground=INK,onSurface=INK,secondary=GOLD)) {
        Scaffold(containerColor=BG, bottomBar={ NavigationBar(containerColor=Color(0xEE181410),contentColor=INK) { listOf("Today" to Icons.Default.CheckCircle,"Boss" to Icons.Default.SportsMma,"Pet" to Icons.Default.Pets,"Shop" to Icons.Default.ShoppingBag,"Report" to Icons.Default.BarChart).forEach { (name,icon)-> NavigationBarItem(selected=tab==name,onClick={tab=name},icon={Icon(icon,null)},label={Text(name)},colors=NavigationBarItemDefaults.colors(selectedIconColor=GOLD,selectedTextColor=GOLD,indicatorColor=Color(0x33FF9D2E),unselectedIconColor=MUTED,unselectedTextColor=MUTED)) } } }, snackbarHost={}) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).background(Brush.radialGradient(listOf(Color(0x443F220B),BG),radius=900f)).padding(horizontal=16.dp)) {
                Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Column { Text("YOUR DAILY RITUAL",color=MUTED,fontSize=11.sp,letterSpacing=1.5.sp);Text("Morning Quest",fontSize=27.sp,fontWeight=FontWeight.Bold) }; Surface(shape=CircleShape,color=accent){Text("Lv ${state.xp/100+1}",Modifier.padding(horizontal=14.dp,vertical=8.dp),color=Color(0xFF1A0E02),fontWeight=FontWeight.Bold)} }
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){ StatChip("🔥 ${state.streak} day streak");StatChip("🪙 ${state.coins}");StatChip("✨ ${state.xp} XP") }
                Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress={(state.xp%100)/100f},modifier=Modifier.fillMaxWidth().height(7.dp),color=accent,trackColor=LINE);Text("${100-state.xp%100} XP to level ${state.xp/100+2}",color=MUTED,fontSize=12.sp,modifier=Modifier.padding(top=5.dp,bottom=10.dp))
                if(message.isNotBlank()){ Surface(color=Color(0xFF302216),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{message=""}){Text(message,Modifier.padding(12.dp),color=GOLD,fontWeight=FontWeight.SemiBold)};Spacer(Modifier.height(8.dp)) }
                when(tab){
                    "Today" -> LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=18.dp)) {
                        item { SectionTitle("Today’s routine · ${state.tasks.count{it.done}}/${state.tasks.size}");LinearProgressIndicator(progress={if(state.tasks.isEmpty())0f else state.tasks.count{it.done}.toFloat()/state.tasks.size},modifier=Modifier.fillMaxWidth().height(7.dp),color=accent,trackColor=LINE) }
                        if(state.tasks.isEmpty()) item { MQCard { Text("No tasks yet. Add one below or choose a template.",color=MUTED) } }
                        items(state.tasks,key={it.id}) { task -> MQCard { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){ Surface(shape=CircleShape,color=if(task.done)Color(0xFFB88735) else Color.Transparent,border=BorderStroke(2.dp,accent),modifier=Modifier.size(36.dp).clickable{complete(task)}){Box(contentAlignment=Alignment.Center){Text(if(task.done)"✓" else "",color=BG,fontWeight=FontWeight.Bold)}};Column(Modifier.weight(1f)){Text(task.name,fontWeight=FontWeight.SemiBold,color=if(task.done)MUTED else INK);Text("${task.time} · ${task.minutes} min",fontSize=12.sp,color=MUTED)};if(!task.done) TextButton(onClick={timerTask=task.id;timerEnd=System.currentTimeMillis()+task.minutes*60000L;notify("Timer started for ${task.name}")}){Text("Timer",color=GOLD)};IconButton(onClick={state.tasks.removeAll{it.id==task.id};save()}){Icon(Icons.Default.Close,null,tint=MUTED)} } } }
                        item { MQCard { Text("Add a task",fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));OutlinedTextField(newName,{newName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Task name")});Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(newTime,{newTime=it},modifier=Modifier.weight(1f),singleLine=true,label={Text("Time (HH:mm)")});OutlinedTextField(newMin,{newMin=it.filter(Char::isDigit)},modifier=Modifier.width(95.dp),singleLine=true,label={Text("Minutes")})};Button(onClick={addTask()},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("＋ Add task",fontWeight=FontWeight.Bold)} } }
                        item { SectionTitle("Template deck") }
                        items(templates.keys.toList()+state.customTemplates.keys.toList()) { name -> val count=templates[name]?.size?:state.customTemplates[name]?.size?:0; MQCard { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("${mapOf("Student" to "🎒","Fitness" to "💪","Productive" to "🚀","Mindful" to "🧘","Creative" to "🎨")[name]?:"⭐"}  $name",fontWeight=FontWeight.Bold);Text("$count tasks · ${if(name=="Student")"Classes and focus" else "Your morning routine"}",fontSize=12.sp,color=MUTED)};Button(onClick={useTemplate(name)},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("Use")}} } }
                        item { MQCard { Text("Build your own template",fontWeight=FontWeight.Bold);OutlinedTextField(templateName,{templateName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Template name")});Button(onClick={if(templateName.isBlank()||state.tasks.isEmpty())notify("Add tasks and a template name first") else if(templateName in templates)notify("That template name is already used") else {state.customTemplates[templateName]=state.tasks.map{it.copy(done=false)}.toMutableList();notify("Template saved ✨");templateName="";save()}},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("Save current tasks as template")} } }
                        item { MQCard { Text("Smart reminder",fontWeight=FontWeight.Bold);OutlinedTextField(state.reminder,{state.reminder=it;save()},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Reminder time (HH:mm)")});Text("Reminder preference saved on this device. Scheduled background notifications are not yet enabled in this build.",fontSize=12.sp,color=MUTED) } }
                    }
                    "Boss" -> LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)) { item { SectionTitle("Daily boss") };item { MQCard { val hp=if(state.tasks.isEmpty())100 else 100-(state.tasks.count{it.done}*100/state.tasks.size);Text(if(hp==0)"💀" else "👹",fontSize=76.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center);LinearProgressIndicator(progress={hp/100f},modifier=Modifier.fillMaxWidth().height(9.dp),color=accent,trackColor=LINE);Text(if(hp==0)"Boss defeated! Come back tomorrow." else "Boss HP $hp% · every completed task deals damage",color=MUTED,modifier=Modifier.fillMaxWidth().padding(top=8.dp),textAlign=TextAlign.Center);Text("Bosses defeated: ${state.bosses}",color=GOLD) } };item { SectionTitle("Loot box") };item { MQCard { Text("🎁",fontSize=54.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center);Text("${state.loot} loot boxes available",color=MUTED,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center);Button(onClick={rollLoot()},enabled=state.loot>0,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("Open loot box") } } };item { SectionTitle("Friends & challenges") };item { MQCard { Text("Weekly challenge: 5 full mornings",fontWeight=FontWeight.Bold);val wins=state.history.count{it.total>0&&it.done==it.total}+(if(state.tasks.isNotEmpty()&&state.tasks.all{it.done})1 else 0);LinearProgressIndicator(progress={(wins.coerceAtMost(5))/5f},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp),color=accent,trackColor=LINE);Text("${wins.coerceAtMost(5)}/5 completed mornings",color=MUTED);Divider(color=LINE);listOf("Aarav" to 210,"Meera" to 160,"Kabir" to 95,"You" to state.xp).sortedByDescending{it.second}.forEachIndexed{idx,p->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${idx+1}. ${p.first}");Text("${p.second} XP",color=GOLD)}};Text("Friend scores are sample/demo players, as in the original.",fontSize=11.sp,color=MUTED) } } }
                    "Pet" -> LazyColumn(contentPadding=PaddingValues(bottom=18.dp)) { item { SectionTitle("Your companion") };item { MQCard { val stage=when {state.growth<2->0;state.growth<5->1;state.growth<10->2;else->3};val emoji=if(state.pet=="plant")listOf("🌱","🌿","🪴","🌳")[stage] else listOf("🥚","🐣","🐥","🦚")[stage];Text(emoji,fontSize=94.sp,modifier=Modifier.fillMaxWidth().clickable{notify(if(state.pet=="plant")"I love sunshine ☀️ · Growing strong!" else "Purr… 💛 · Let’s crush today!")},textAlign=TextAlign.Center);Text(if(state.pet=="plant")"Growth ${state.growth} · Your plant companion" else "Growth ${state.growth} · Your pet companion",color=MUTED,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center);LinearProgressIndicator(progress={(state.growth%5)/5f},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),color=accent,trackColor=LINE);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={feed()},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text(if(state.pet=="plant")"💧 Water · 5" else "🍎 Feed · 5")};OutlinedButton(onClick={state.pet="plant";save()},modifier=Modifier.weight(1f)){Text("Plant")};OutlinedButton(onClick={state.pet="pet";save()},modifier=Modifier.weight(1f)){Text("Pet")}};Text("${if(state.feedDay==LocalDate.now().toString())state.feeds else 0}/3 care actions today · each adds growth",fontSize=12.sp,color=MUTED,modifier=Modifier.fillMaxWidth().padding(top=8.dp),textAlign=TextAlign.Center) } } }
                    "Shop" -> LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=18.dp)) { item { SectionTitle("Morning Quest Premium") };item { MQCard { Text("⭐ Premium",fontWeight=FontWeight.Bold,fontSize=20.sp);Text("The original design lists Forest and Grape themes, custom templates and deeper weekly insights as Premium benefits.",color=MUTED);Text("₹99 · one-time unlock",color=GOLD,fontWeight=FontWeight.Bold);Text("Play Billing is not configured in this build, so no payment will be taken here.",fontSize=12.sp,color=MUTED) } };item { SectionTitle("Themes") };items(themeColors.entries.toList()){(name,color)->MQCard{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(24.dp).background(color,CircleShape));Column(Modifier.weight(1f).padding(start=10.dp)){Text(name,fontWeight=FontWeight.Bold);Text(if(name in state.themes)"Unlocked" else "Premium theme",fontSize=12.sp,color=MUTED)};if(name in state.themes)TextButton(onClick={state.theme=name;save()}){Text(if(state.theme==name)"Active" else "Use",color=GOLD)}else Text("🔒",fontSize=18.sp)}}};item { SectionTitle("My rewards") };items(state.rewards){r->MQCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.name,fontWeight=FontWeight.SemiBold);Text("Cost: ${r.cost} coins",fontSize=12.sp,color=MUTED)};Button(onClick={if(state.coins>=r.cost){state.coins-=r.cost;save();notify("Enjoy: ${r.name} 🎮")}else notify("You need ${r.cost-state.coins} more coins")},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("Claim")}}}};item { MQCard { OutlinedTextField(rewardName,{rewardName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Reward name")});OutlinedTextField(rewardCost,{rewardCost=it.filter(Char::isDigit)},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Coin cost")});Button(onClick={val c=rewardCost.toIntOrNull();if(rewardName.isNotBlank()&&c!=null&&c>0){state.rewards.add(Reward(rewardName,c));rewardName="";rewardCost="";save()}else notify("Enter a reward and coin cost")},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=BG)){Text("＋ Add reward") } } } }
                    else -> LazyColumn(contentPadding=PaddingValues(bottom=18.dp)) { item { SectionTitle("Weekly report") };item { MQCard { val days=(0..6).map{d->LocalDate.now().minusDays((6-d).toLong()).toString()};val records=days.map{date->if(date==LocalDate.now().toString()) DayRecord(date,state.tasks.count{it.done},state.tasks.size) else (state.history.find{it.date==date} ?: DayRecord(date,0,0)) };val valid=records.filter{it.total>0};val avg=if(valid.isEmpty())0 else valid.map{it.done*100/it.total}.average().toInt();Text("This week",fontWeight=FontWeight.Bold,fontSize=18.sp);Row(Modifier.fillMaxWidth().height(130.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.Bottom){records.forEach{r->Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom){Box(Modifier.width(24.dp).height((if(r.total==0)3 else (r.done*100/r.total).coerceAtLeast(3)).dp).background(Brush.verticalGradient(listOf(GOLD,accent)),RoundedCornerShape(topStart=6.dp,topEnd=6.dp)));Text(LocalDate.parse(r.date).dayOfWeek.name.take(3),fontSize=9.sp,color=MUTED)}}};Text("Completion rate: $avg%",fontWeight=FontWeight.Bold);val best=valid.maxByOrNull{it.done.toFloat()/it.total};Text("Best day: ${best?.let{LocalDate.parse(it.date).dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)}?:"none yet"}",color=MUTED);Text("Most skipped: ${state.history.flatMap{it.skipped}.groupingBy{it}.eachCount().maxByOrNull{it.value}?.key?:"nothing yet"}",color=MUTED) } };item { SectionTitle("Badges") };items(listOf("🌅 Early bird" to "Finish a routine before 8 AM","🎉 First win" to "Finish your first routine","🔥 3-day streak" to "Keep a 3-day streak","🏆 Week warrior" to "Keep a 7-day streak","⭐ Level 5" to "Reach level 5","⚔️ Boss slayer" to "Defeat a boss","💰 Saver" to "Hold 200 coins")){(b,desc)->MQCard{Row(verticalAlignment=Alignment.CenterVertically){Text(if(b in state.badges)"🏅" else "🔒",fontSize=22.sp);Column(Modifier.padding(start=10.dp)){Text(b,fontWeight=FontWeight.Bold,color=if(b in state.badges)GOLD else MUTED);Text(desc,fontSize=12.sp,color=MUTED)}}}} }
                }
            }
        }
    }
}

@Composable private fun StatChip(text:String){Surface(color=Color(0x1AFFFFFF),shape=CircleShape,border=BorderStroke(1.dp,LINE)){Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=INK)}}
@Composable private fun SectionTitle(text:String){Text(text,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp,bottom=4.dp),color=INK)}
@Composable private fun MQCard(content:@Composable ColumnScope.()->Unit){Surface(color=Color(0xE61A1612),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,LINE),shadowElevation=8.dp,modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(15.dp),content=content)}}
