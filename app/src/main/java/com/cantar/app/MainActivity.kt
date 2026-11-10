package com.cantar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Cantar()}}}
private enum class Page{HOME,LIBRARY,SETUP,SING,RESULT}
@Composable private fun Cantar(){
 var page by remember{mutableStateOf(Page.HOME)}
 var selected by remember{mutableStateOf(library[0])}
 var query by remember{mutableStateOf("")}
 var style by remember{mutableStateOf("Todos")}
 var lang by remember{mutableStateOf("Todos")}
 var shift by remember{mutableIntStateOf(0)}
 val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){selected=Song(uri.lastPathSegment?: "Audio local","Archivo importado","Por analizar","Importada","Local");page=Page.SETUP}}
 MaterialTheme(colorScheme=darkColorScheme(background=bg,surface=panel,primary=purple,secondary=cyan)){
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10194A),bg)))){
   when(page){
    Page.HOME->Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Box(Modifier.size(160.dp).clip(CircleShape).background(Brush.linearGradient(listOf(purple,Color(0xFF142A78),Color(0xFFE735D4)))),contentAlignment=Alignment.Center){Icon(Icons.Default.Mic,null,tint=Color.White,modifier=Modifier.size(90.dp))}
     Text("Cantar",fontSize=48.sp,fontWeight=FontWeight.Black);Text("DAR LA NOTA",color=gold,fontSize=19.sp,fontWeight=FontWeight.Bold)
     Spacer(Modifier.height(30.dp));HomeButton("♫  Elegir canción",purple){page=Page.LIBRARY};HomeButton("🏆  Competencia",Color(0xFF08A94E)){page=Page.LIBRARY};HomeButton("★  Mis favoritas",Color(0xFFE58B10)){page=Page.LIBRARY};HomeButton("⚙  Configuración",Color(0xFF126BD9)){page=Page.LIBRARY}
     Spacer(Modifier.height(16.dp));Text("Tu voz. Tu tono. Tu puntuación.",color=muted)
    }
    Page.LIBRARY->{
     val filtered=library.filter{(style=="Todos"||it.genre==style)&&(lang=="Todos"||it.lang==lang)&&(it.title.contains(query,true)||it.artist.contains(query,true)||it.genre.contains(query,true))}
     Column(Modifier.fillMaxSize().padding(15.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){IconButton({page=Page.HOME}){Icon(Icons.Default.ArrowBack,null)};Text("Biblioteca musical",fontSize=22.sp,fontWeight=FontWeight.Bold)}
      OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar canción, artista o género")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
      Text("IDIOMA",color=cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Todos","Español","Inglés","Francés","Otros").forEach{FilterChip(lang==it,{lang=it},{Text(it)})}}
      Spacer(Modifier.height(8.dp));Text("ESTILOS MUSICALES",color=cyan,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("Ritmos latinos y géneros de todo el mundo",color=muted,fontSize=12.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=7.dp)){styles.forEach{FilterChip(style==it,{style=it},{Text(it,fontSize=12.sp)})}}
      Row(verticalAlignment=Alignment.CenterVertically){Text(filtered.size.toString()+" pistas de muestra",color=muted,modifier=Modifier.weight(1f));TextButton({import.launch(arrayOf("audio/*"))}){Icon(Icons.Default.LibraryMusic,null);Text(" Importar")}}
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
       items(filtered){item->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(panel).clickable{selected=item;shift=0;page=Page.SETUP}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.MusicNote,null,tint=cyan,modifier=Modifier.size(38.dp));Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(item.title,fontWeight=FontWeight.Bold);Text(item.artist,color=muted,fontSize=12.sp);Text(item.genre+" · "+item.lang,color=cyan,fontSize=11.sp)};Text(item.key,color=gold,fontSize=11.sp)
       }}
       if(filtered.isEmpty())item{Text("Sin resultados. Cambiá el filtro o importá una canción.",color=muted)}
      }
     }
    }
    Page.SETUP->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Row(verticalAlignment=Alignment.CenterVertically){IconButton({page=Page.LIBRARY}){Icon(Icons.Default.ArrowBack,null)};Text("Preparar canción",fontSize=22.sp,fontWeight=FontWeight.Bold)}
     Spacer(Modifier.height(25.dp));Text(selected.title,fontSize=25.sp,fontWeight=FontWeight.Bold);Text(selected.artist,color=muted);Text("Género: "+selected.genre,color=cyan);Text("Tono original: "+selected.key,color=gold)
     Spacer(Modifier.height(35.dp));Text("ELEGÍ TU TONALIDAD",color=cyan,fontWeight=FontWeight.Bold);Text(if(shift==0)selected.key else selected.key+"  ("+(if(shift>0)"+" else "")+shift+" semitonos)",fontSize=27.sp,color=green,fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(-2,-1,0,1,2).forEach{n->OutlinedButton({shift=if(n==0)0 else (shift+n).coerceIn(-12,12)}){Text(if(n==0)"Original" else n.toString())}}}
     Text("Ajuste de −12 a +12 semitonos",color=muted,fontSize=12.sp);Spacer(Modifier.weight(1f));HomeButton("▶  Comenzar a cantar",green){page=Page.SING}
    }
    Page.SING->Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Text(selected.title,fontSize=24.sp,fontWeight=FontWeight.Bold);Text("Tono elegido: "+selected.key,color=gold);Spacer(Modifier.height(25.dp));Text("LÍNEA DE AFINACIÓN",color=cyan,fontWeight=FontWeight.Bold);Text("El gráfico de notas se sincronizará con la pista melódica.",color=muted)
     Spacer(Modifier.height(30.dp));Icon(Icons.Default.Mic,null,tint=cyan,modifier=Modifier.size(75.dp));Text("Preparado para escuchar tu voz");Spacer(Modifier.height(20.dp));HomeButton("Finalizar sesión",green){page=Page.RESULT}
    }
    Page.RESULT->Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.EmojiEvents,null,tint=gold,modifier=Modifier.size(100.dp));Text("¡Terminaste!",fontSize=32.sp,fontWeight=FontWeight.Bold);HomeButton("Volver al menú",green){page=Page.HOME};HomeButton("Elegir otra canción",purple){page=Page.LIBRARY}}
   }
  }
 }
}
@Composable private fun HomeButton(label:String,color:Color,onClick:()->Unit){Button(onClick,Modifier.fillMaxWidth().padding(vertical=6.dp).height(55.dp),colors=ButtonDefaults.buttonColors(containerColor=color),shape=RoundedCornerShape(17.dp)){Text(label,fontWeight=FontWeight.Bold,fontSize=16.sp,color=Color.White)}}
