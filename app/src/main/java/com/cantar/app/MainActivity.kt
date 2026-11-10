package com.cantar.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bg=Color(0xFF070B20)
private val panel=Color(0xFF121A39)
private val purple=Color(0xFF8B35F4)
private val cyan=Color(0xFF19D8FF)
private val green=Color(0xFF22E66B)
private val gold=Color(0xFFFFD43B)
private val muted=Color(0xFF9BA8C8)

private data class Song(val title:String,val artist:String,val key:String,val genre:String,val lang:String)
private val library=listOf(
 Song("De música ligera","Soda Stereo","Mi mayor","Rock nacional","Español"),
 Song("Persiana americana","Soda Stereo","Do sostenido menor","Rock nacional","Español"),
 Song("Lamento boliviano","Enanitos Verdes","Re mayor","Rock nacional","Español"),
 Song("Flaca","Andrés Calamaro","La mayor","Rock nacional","Español"),
 Song("La camisa negra","Juanes","Fa sostenido menor","Pop latino","Español"),
 Song("Vivir mi vida","Marc Anthony","Re menor","Salsa","Español"),
 Song("Despacito","Luis Fonsi","Si menor","Reguetón","Español"),
 Song("Provenza","Karol G","Do menor","Urbano latino","Español"),
 Song("Hasta la Raíz","Natalia Lafourcade","Do mayor","Pop latino","Español"),
 Song("La Bicicleta","Carlos Vives y Shakira","Fa mayor","Vallenato","Español"),
 Song("Todo de ti","Rauw Alejandro","Do sostenido menor","Urbano latino","Español"),
 Song("Oye cómo va","Santana","La menor","Latin rock","Español"),
 Song("Perfect","Ed Sheeran","Sol bemol mayor","Pop","Inglés"),
 Song("Shape of You","Ed Sheeran","Do sostenido menor","Pop","Inglés"),
 Song("Bohemian Rhapsody","Queen","Si bemol mayor","Rock","Inglés"),
 Song("Smells Like Teen Spirit","Nirvana","Fa menor","Grunge","Inglés"),
 Song("Billie Jean","Michael Jackson","Fa sostenido menor","Pop","Inglés"),
 Song("Hotel California","Eagles","Si menor","Rock","Inglés"),
 Song("Take On Me","a-ha","La mayor","Synth pop","Inglés"),
 Song("La vie en rose","Édith Piaf","Do mayor","Chanson","Francés"),
 Song("Dragostea Din Tei","O-Zone","Do mayor","Eurodance","Rumano")
)
private val styles=listOf("Todos","Rock nacional","Pop latino","Rock","Pop","Reguetón","Urbano latino","Cumbia","Cuarteto","Salsa","Bachata","Merengue","Vallenato","Tango","Folklore","Latin rock","Jazz","Blues","R&B","Hip-hop","Rap","Reggae","Electrónica","Dance","Disco","Country","Metal","Grunge","Indie","Funk","Gospel","Chanson","K-pop","J-pop","Afrobeats","Flamenco","Clásica")
private enum class Page { HOME, PROFILE, LIBRARY, SETUP, SING, RESULT, HISTORY, SETTINGS }
private enum class PlayMode { SOLO, DUEL }

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Cantar(onExit={finish()})}}
}

@Composable private fun Cantar(onExit:()->Unit){
 val context= LocalContext.current
 val prefs= remember { context.getSharedPreferences("cantar_local_profile",Context.MODE_PRIVATE) }
 var page by remember { mutableStateOf(Page.HOME) }
 var previousPage by remember { mutableStateOf(Page.HOME) }
 var selected by remember { mutableStateOf(library[6]) }
 var query by remember { mutableStateOf("") }
 var style by remember { mutableStateOf("Todos") }
 var lang by remember { mutableStateOf("Todos") }
 var shift by remember { mutableIntStateOf(0) }
 var profileName by remember { mutableStateOf(prefs.getString("profile_name","") ?: "") }
 var opponent by remember { mutableStateOf(prefs.getString("opponent_name","") ?: "") }
 var mode by remember { mutableStateOf(if(prefs.getString("mode","SOLO")=="DUEL") PlayMode.DUEL else PlayMode.SOLO) }
 var showExit by remember { mutableStateOf(false) }
 var points by remember { mutableIntStateOf(0) }
 var livePitch by remember { mutableStateOf<PitchAnalyzer.Pitch?>(null) }
 var duelRound by remember { mutableIntStateOf(1) }
 var firstDuelScore by remember { mutableIntStateOf(0) }
 var secondDuelScore by remember { mutableIntStateOf(0) }
 var duelHistory by remember { mutableStateOf(prefs.getString("duel_history", "") ?: "") }
 var sessions by remember { mutableIntStateOf(prefs.getInt("sessions",0)) }
 var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score",0)) }
 var lastScore by remember { mutableIntStateOf(prefs.getInt("last_score",0)) }
 var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
 LaunchedEffect(profileName,opponent,mode,selected,shift,favorites){
  prefs.edit().putString("profile_name",profileName.trim()).putString("opponent_name",opponent.trim())
   .putString("mode",mode.name).putString("selected_title",selected.title).putInt("transpose",shift)
   .putStringSet("favorites",favorites).apply()
 }
 val audioPicker= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);selected=Song(uri.lastPathSegment?.substringAfterLast('/') ?: "Audio local","Archivo del dispositivo","Por analizar","Importada","Local");shift=0;page=Page.SETUP}
 }
 fun saveProfile(){
  prefs.edit().putString("profile_name",profileName.trim()).putString("opponent_name",opponent.trim())
   .putString("mode",mode.name).putStringSet("favorites",favorites).apply()
 }
 fun goBack(){
  page=when(page){
   Page.HOME->{showExit=true;Page.HOME}
   Page.PROFILE->Page.HOME
   Page.LIBRARY->previousPage.takeIf{it==Page.HISTORY}?:Page.HOME
   Page.SETUP->Page.LIBRARY
   Page.SING->Page.SETUP
   Page.RESULT->Page.HOME
   Page.HISTORY,Page.SETTINGS->Page.HOME
  }
 }
 BackHandler(enabled=true){if(page==Page.HOME)showExit=true else goBack()}

 MaterialTheme(colorScheme=darkColorScheme(background=bg,surface=panel,primary=purple,secondary=cyan)){
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10194A),bg)))){
   when(page){
    Page.HOME->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Box(Modifier.size(142.dp).clip(CircleShape).background(Brush.linearGradient(listOf(purple,Color(0xFF142A78),Color(0xFFE735D4)))),contentAlignment=Alignment.Center){Icon(Icons.Default.Mic,null,tint=Color.White,modifier=Modifier.size(82.dp))}
     Text("Cantar",fontSize=48.sp,fontWeight=FontWeight.Black);Text("DAR LA NOTA",color=gold,fontSize=20.sp,fontWeight=FontWeight.Bold)
     Spacer(Modifier.height(22.dp));Text(if(profileName.isBlank())"Creá tu perfil para guardar tus resultados" else "Hola, ${profileName}",color=muted)
     HomeButton("♫  Elegir canción",purple){previousPage=Page.HOME;page=Page.LIBRARY}
     HomeButton("🏆  Solo / Duelo",Color(0xFF08A94E)){page=Page.PROFILE}
     HomeButton("★  Mis Favoritas",Color(0xFFE58B10)){style="Todos";query="";page=Page.LIBRARY}
     HomeButton("▤  Historial y puntuaciones",Color(0xFF126BD9)){page=Page.HISTORY}
     HomeButton("⚙  Configuración",Color(0xFF34446D)){page=Page.SETTINGS}
     Spacer(Modifier.height(10.dp));Text("Tu voz. Tu tono. Tu puntuación.",color=muted)
    }
    Page.PROFILE->Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     Header("Perfil y modalidad"){page=Page.HOME}
     Text("PERFIL LOCAL",color=cyan,fontWeight=FontWeight.Bold)
     OutlinedTextField(profileName,{profileName=it},Modifier.fillMaxWidth(),label={Text("Tu nombre o apodo")},singleLine=true)
     Text("TIPO DE PARTIDA",color=cyan,fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
      ModeCard("🎤  SOLO","Tu puntuación personal",mode==PlayMode.SOLO,Modifier.weight(1f)){mode=PlayMode.SOLO}
      ModeCard("⚔  DUELO","Compará dos perfiles",mode==PlayMode.DUEL,Modifier.weight(1f)){mode=PlayMode.DUEL}
     }
     if(mode==PlayMode.DUEL)OutlinedTextField(opponent,{opponent=it},Modifier.fillMaxWidth(),label={Text("Nombre del rival")},singleLine=true)
     Text("Los datos del perfil, la modalidad y los resultados se guardan en este dispositivo.",color=muted,fontSize=12.sp)
     Spacer(Modifier.weight(1f));HomeButton("Guardar perfil",green){saveProfile();page=Page.HOME}
    }
    Page.LIBRARY->{
     val filtered=library.filter{(style=="Todos"||it.genre==style)&&(lang=="Todos"||it.lang==lang||(lang=="Otros"&&it.lang !in listOf("Español","Inglés")))&&(it.title.contains(query,true)||it.artist.contains(query,true)||it.genre.contains(query,true))}
     Column(Modifier.fillMaxSize().padding(14.dp)){
      Header("Seleccionar canción"){goBack()}
      OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar canción...")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
      Text("IDIOMA",color=cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Todos","Español","Inglés","Francés","Otros").forEach{FilterChip(lang==it,{lang=it},{Text(it)})}}
      Text("ESTILOS MUSICALES",color=cyan,fontSize=12.sp,fontWeight=FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=5.dp)){styles.forEach{FilterChip(style==it,{style=it},{Text(it,fontSize=12.sp)})}}
      Row(verticalAlignment=Alignment.CenterVertically){Text("${filtered.size} pistas de muestra",color=muted,modifier=Modifier.weight(1f));TextButton({audioPicker.launch(arrayOf("audio/*"))}){Icon(Icons.Default.LibraryMusic,null);Text(" Importar")}}
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
       items(filtered){item->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(panel).clickable{selected=item;shift=0;page=Page.SETUP}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.MusicNote,null,tint=cyan,modifier=Modifier.size(34.dp));Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(item.title,fontWeight=FontWeight.Bold);Text(item.artist,color=muted,fontSize=12.sp);Text(item.genre+" · "+item.lang,color=cyan,fontSize=11.sp)};Text(item.key,color=gold,fontSize=11.sp)
       }}
       if(filtered.isEmpty())item{Text("Sin resultados. Cambiá los filtros o importá una pista.",color=muted)}
      }
     }
    }
    Page.SETUP->Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Header("Configurar canción"){page=Page.LIBRARY}
     Spacer(Modifier.height(22.dp));Panel { Text(selected.title,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(selected.artist,color=muted);Text("Tono original: ${selected.key}",color=gold) }
     Spacer(Modifier.height(22.dp));Text("ELEGIR TONO",color=cyan,fontWeight=FontWeight.Bold)
     Text(if(shift==0)selected.key else "${selected.key}  (${if(shift>0)"+" else ""}$shift)",fontSize=24.sp,color=green,fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(-2,-1,0,1,2).forEach{n->OutlinedButton({shift=if(n==0)0 else (shift+n).coerceIn(-12,12)}){Text(if(n==0)"Normal" else if(n<0)"Más bajo\n($n)" else "Más alto\n(+$n)")}}}
     Text("Ajuste de −12 a +12 semitonos",color=muted,fontSize=12.sp)
     Spacer(Modifier.weight(1f));HomeButton("▶  Comenzar a cantar",green){points=0;duelRound=1;firstDuelScore=0;secondDuelScore=0;page=Page.SING}
    }
    Page.SING->Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Text("${selected.title} · ${selected.artist}",fontWeight=FontWeight.Bold)
     Text(if(mode==PlayMode.DUEL) "Turno ${duelRound}: ${if(duelRound==1) profileName.ifBlank{"Jugador 1"} else opponent.ifBlank{"Jugador 2"}}" else "Perfil: ${profileName.ifBlank{"Invitado"}}",color=cyan,fontWeight=FontWeight.Bold)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ScorePill("PUNTOS",points.toString());ScorePill("MODO",if(mode==PlayMode.SOLO)"SOLO" else "DUELO")}
     Spacer(Modifier.height(18.dp));Text("AFINACIÓN EN VIVO",color=cyan,fontWeight=FontWeight.Bold)
     Text(livePitch?.noteName ?: "Esperando una nota...",color=livePitch?.let{if(kotlin.math.abs(it.cents)<=25)green else gold}?:muted,fontSize=25.sp,fontWeight=FontWeight.Bold)
     key(duelRound){ VocalPitchCapture(onPitch={livePitch=it},onPoints={points=it}) }
     Text("Puntos por mantener la voz afinada (±25 cents). La comparación sincronizada con la melodía original requiere una pista de referencia.",color=muted,fontSize=11.sp)
     Spacer(Modifier.height(10.dp))
     HomeButton(if(mode==PlayMode.DUEL && duelRound==1) "Finalizar turno 1" else "Finalizar sesión",green){
      if(mode==PlayMode.DUEL && duelRound==1){
       firstDuelScore=points;duelRound=2;points=0;livePitch=null
      } else {
       if(mode==PlayMode.DUEL) secondDuelScore=points
       lastScore=if(mode==PlayMode.DUEL) maxOf(firstDuelScore,secondDuelScore) else points
       sessions++;if(lastScore>bestScore)bestScore=lastScore
       if(mode==PlayMode.DUEL){
        val winner=if(firstDuelScore>secondDuelScore) profileName.ifBlank{"Jugador 1"} else if(secondDuelScore>firstDuelScore) opponent.ifBlank{"Jugador 2"} else "Empate"
        duelHistory=(duelHistory+"\n"+winner+": "+firstDuelScore+" - "+secondDuelScore).takeLast(3000)
        prefs.edit().putString("duel_history",duelHistory).apply()
       }
       prefs.edit().putInt("last_score",lastScore).putInt("best_score",bestScore).putInt("sessions",sessions).apply()
       page=Page.RESULT
      }
     }
    }
    Page.RESULT->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Icon(Icons.Default.EmojiEvents,null,tint=gold,modifier=Modifier.size(90.dp));Text("¡Resultado!",fontSize=30.sp,fontWeight=FontWeight.Bold)
     Text(if(mode==PlayMode.DUEL) "${firstDuelScore}  —  ${secondDuelScore}" else lastScore.toString(),fontSize=52.sp,color=green,fontWeight=FontWeight.Black);Text("puntos · ${if(mode==PlayMode.SOLO)"Partida individual" else "Duelo"}",color=muted)
     if(mode==PlayMode.DUEL){Text("${profileName.ifBlank{"Jugador 1"}}  vs  ${opponent.ifBlank{"Jugador 2"}}",color=cyan);Text("Resultado del duelo: ${firstDuelScore} - ${secondDuelScore}",color=gold);Text(if(firstDuelScore>secondDuelScore) "Ganó ${profileName.ifBlank{"Jugador 1"}}" else if(secondDuelScore>firstDuelScore) "Ganó ${opponent.ifBlank{"Jugador 2"}}" else "Empate",color=green);Text("Historial local guardado",color=muted,fontSize=12.sp)}
     Text("Mejor marca: $bestScore · Sesiones: $sessions",color=gold)
     HomeButton("Volver al menú",green){page=Page.HOME};HomeButton("Otra canción",purple){page=Page.LIBRARY}
    }
    Page.HISTORY->Column(Modifier.fillMaxSize().padding(20.dp)){
     Header("Historial del perfil"){page=Page.HOME}
     Panel{Text(profileName.ifBlank{"Perfil sin nombre"},fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Mejor puntuación: $bestScore",color=green);Text("Última puntuación: $lastScore",color=gold);Text("Partidas guardadas: $sessions",color=muted);if(duelHistory.isNotBlank()){Text("DUELOS RECIENTES",color=cyan,fontWeight=FontWeight.Bold);Text(duelHistory,color=muted)}}
     Text("Las puntuaciones son locales y permanecen en este dispositivo.",color=muted,fontSize=12.sp)
    }
    Page.SETTINGS->Column(Modifier.fillMaxSize().padding(20.dp)){
     Header("Configuración"){page=Page.HOME}
     Text("Perfil: ${profileName.ifBlank{"Sin registrar"}}",color=cyan)
     Text("Modalidad preferida: ${mode.name}",color=muted)
     Text("Los datos locales se conservan al cerrar la aplicación.",color=muted)
     TextButton({page=Page.PROFILE}){Text("Editar perfil y modalidad")}
    }
   }
  }
  if(showExit)AlertDialog(onDismissRequest={showExit=false},title={Text("Cerrar aplicación")},text={Text("¿Querés salir de Cantar? Tu perfil y las puntuaciones guardadas permanecerán en este dispositivo.")},confirmButton={TextButton(onClick={onExit}){Text("Cerrar aplicación",color=green)}},dismissButton={TextButton(onClick={showExit=false}){Text("Seguir cantando")}})
 }
}
@Composable private fun Header(title:String,onBack:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Default.ArrowBack,null)};Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Panel(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(panel).padding(16.dp),content=content)}
@Composable private fun ModeCard(title:String,subtitle:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){Column(modifier.clip(RoundedCornerShape(16.dp)).background(if(selected)Color(0xFF233A70) else panel).clickable(onClick).padding(13.dp)){Text(title,color=if(selected)cyan else Color.White,fontWeight=FontWeight.Bold);Text(subtitle,color=muted,fontSize=11.sp)}}
@Composable private fun ScorePill(label:String,value:String){Column(Modifier.clip(RoundedCornerShape(13.dp)).background(panel).padding(horizontal=16.dp, vertical=9.dp)){Text(label,color=cyan,fontSize=11.sp);Text(value,color=green,fontWeight=FontWeight.Bold,fontSize=20.sp)}}
@Composable private fun HomeButton(label:String,color:Color,onClick:()->Unit){Button(onClick,Modifier.fillMaxWidth().padding(vertical=5.dp).height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=color),shape=RoundedCornerShape(16.dp)){Text(label,fontWeight=FontWeight.Bold,fontSize=15.sp,color=Color.White)}}
