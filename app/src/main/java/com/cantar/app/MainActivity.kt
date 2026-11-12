package com.cantar.app

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer

private val bg=Color(0xFF05091E)
private val panel=Color(0xFF101733)
private val purple=Color(0xFF8B20F5)
private val cyan=Color(0xFF00DFFF)
private val green=Color(0xFF16E34F)
private val gold=Color(0xFFFFD900)
private val muted=Color(0xFF9BA8C8)
private val magenta=Color(0xFFE52CFF)
private val orange=Color(0xFFFF8C00)
private val electricBlue=Color(0xFF087DFF)

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
 Song("Dragostea Din Tei","O-Zone","No verificada","Eurodance","Rumano"),
 Song("Bombón asesino","Los Palmeras","No verificada","Cumbia","Español"),
 Song("Nunca me faltes","Antonio Ríos","No verificada","Cumbia","Español"),
 Song("Cómo te voy a olvidar","Los Ángeles Azules","No verificada","Cumbia","Español"),
 Song("Beso a beso","La Mona Jiménez","No verificada","Cuarteto","Español"),
 Song("Quién se ha tomado todo el vino","La Mona Jiménez","No verificada","Cuarteto","Español"),
 Song("Propuesta indecente","Romeo Santos","No verificada","Bachata","Español"),
 Song("Obsesión","Aventura","No verificada","Bachata","Español"),
 Song("Suavemente","Elvis Crespo","No verificada","Merengue","Español"),
 Song("Por una cabeza","Carlos Gardel","No verificada","Tango","Español"),
 Song("Zamba de mi esperanza","Los Chalchaleros","No verificada","Folklore","Español"),
 Song("Alfonsina y el mar","Mercedes Sosa","No verificada","Folklore","Español"),
 Song("Garota de Ipanema","João Gilberto","No verificada","Bossa nova","Portugués"),
 Song("Take Me Home, Country Roads","John Denver","No verificada","Country","Inglés"),
 Song("Fly Me to the Moon","Frank Sinatra","No verificada","Jazz","Inglés"),
 Song("The Thrill Is Gone","B.B. King","No verificada","Blues","Inglés"),
 Song("Lose Yourself","Eminem","No verificada","Hip-hop","Inglés"),
 Song("Three Little Birds","Bob Marley & The Wailers","No verificada","Reggae","Inglés"),
 Song("Dynamite","BTS","No verificada","K-pop","Coreano"),
 Song("Lemon","Kenshi Yonezu","No verificada","J-pop","Japonés"),
 Song("Entre dos aguas","Paco de Lucía","No verificada","Flamenco","Instrumental"),
 Song("Oh Happy Day","Edwin Hawkins Singers","No verificada","Gospel","Inglés"),
 Song("Titanium","David Guetta feat. Sia","No verificada","Electrónica","Inglés"),
 Song("Contigo aprendí","Armando Manzanero","No verificada","Bolero","Español"),
 Song("Aquarela do Brasil","João Gilberto","No verificada","Samba","Portugués"),
 Song("Mas que nada","Sérgio Mendes","No verificada","Samba","Portugués"),
 Song("El Rey","Vicente Fernández","No verificada","Regional mexicano","Español"),
 Song("La puerta negra","Los Tigres del Norte","No verificada","Regional mexicano","Español")
)
private val styles=listOf("Todos","Rock nacional","Pop latino","Rock","Pop","Reguetón","Urbano latino","Cumbia","Cuarteto","Salsa","Bachata","Merengue","Vallenato","Tango","Folklore","Latin rock","Bolero","Bossa nova","Samba","Regional mexicano","Jazz","Blues","R&B","Hip-hop","Rap","Reggae","Electrónica","Dance","Disco","Country","Metal","Grunge","Indie","Funk","Gospel","Chanson","K-pop","J-pop","Afrobeats","Flamenco","Clásica")
private enum class Page { HOME, PROFILE, LIBRARY, SETUP, SING, RESULT, HISTORY, SETTINGS }
private enum class PlayMode { SOLO, DUEL }
private data class LyricLine(val timeMs: Long, val text: String)
private fun parseLrc(raw: String): List<LyricLine> {
 val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]([^\\r\n]*)")
 return regex.findAll(raw).mapNotNull { match ->
  val min = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
  val sec = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
  val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
  LyricLine((min * 60 + sec) * 1000 + fraction, match.groupValues[4].trim())
 }.sortedBy { it.timeMs }
 .toList()
}

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{Cantar(onExit={finish()})}}
}

@Composable private fun Cantar(onExit:()->Unit){
 val context= LocalContext.current
 val prefs= remember { context.getSharedPreferences("cantar_local_profile",Context.MODE_PRIVATE) }
 var page by remember { mutableStateOf(Page.HOME) }
 LaunchedEffect(page) {
  (context as? Activity)?.requestedOrientation = if (page == Page.SING) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
 }
 var previousPage by remember { mutableStateOf(Page.HOME) }
 var selected by remember {
  val savedTitle=prefs.getString("selected_title",library[6].title) ?: library[6].title
  val savedArtist=prefs.getString("selected_artist",library[6].artist) ?: library[6].artist
  mutableStateOf(library.firstOrNull{it.title==savedTitle && it.artist==savedArtist}
   ?: Song(savedTitle,savedArtist,"No verificada","Audio local","Local"))
 }
 var query by remember { mutableStateOf("") }
 var remoteTracks by remember { mutableStateOf(emptyList<LrclibTrack>()) }
 var searchingOnline by remember { mutableStateOf(false) }
 var remoteError by remember { mutableStateOf("") }
 var style by remember { mutableStateOf("Todos") }
 var lang by remember { mutableStateOf("Todos") }
 var shift by remember { mutableIntStateOf(prefs.getInt("transpose",0).coerceIn(-12,12)) }
 var profileName by remember { mutableStateOf(prefs.getString("profile_name","") ?: "") }
 var opponent by remember { mutableStateOf(prefs.getString("opponent_name","") ?: "") }
 var mode by remember { mutableStateOf(if(prefs.getString("mode","SOLO")=="DUEL") PlayMode.DUEL else PlayMode.SOLO) }
 var showExit by remember { mutableStateOf(false) }
 var points by remember { mutableIntStateOf(0) }
 var livePitch by remember { mutableStateOf<PitchAnalyzer.Pitch?>(null) }
 var selectedAudioUri by remember { mutableStateOf(prefs.getString("selected_audio_uri", null)) }
 var micPermissionGranted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
 var lyricLines by remember { mutableStateOf(emptyList<LyricLine>()) }
 var currentPositionMs by remember { mutableLongStateOf(0L) }
 var playerPlaying by remember { mutableStateOf(false) }
 var playerError by remember { mutableStateOf("") }
 val player = remember(context) { ExoPlayer.Builder(context).build() }
 DisposableEffect(player) {
  val listener=object:Player.Listener {
   override fun onIsPlayingChanged(isPlaying:Boolean){playerPlaying=isPlaying}
   override fun onPlaybackStateChanged(playbackState:Int){if(playbackState==Player.STATE_ENDED)playerPlaying=false}
   override fun onPlayerError(error:PlaybackException){playerError=error.errorCodeName;playerPlaying=false}
  }
  player.addListener(listener)
  onDispose { player.removeListener(listener); player.release() }
 }
 LaunchedEffect(selectedAudioUri) {
  playerError=""
  player.stop()
  selectedAudioUri?.let { player.setMediaItem(MediaItem.fromUri(it)); player.prepare() }
 }
 LaunchedEffect(query) {
  if (query.trim().length < 3) { remoteTracks = emptyList(); searchingOnline = false; remoteError="" }
  else {
   delay(700)
   searchingOnline = true
   remoteError=""
   remoteTracks = try { LrclibClient.search(query) }
   catch(error:LrclibClient.RateLimitedException){remoteError="LRCLIB pidió esperar ${error.retryAfterSeconds} s antes de otra búsqueda.";emptyList()}
   catch(_:Exception){remoteError="No se pudo consultar LRCLIB. Revisá la conexión e intentá más tarde.";emptyList()}
   searchingOnline = false
  }
 }
 LaunchedEffect(shift) { player.setPlaybackParameters(AudioPitchShift.playbackParameters(shift)) }
 var duelRound by remember { mutableIntStateOf(1) }
 var firstDuelScore by remember { mutableIntStateOf(0) }
 var secondDuelScore by remember { mutableIntStateOf(0) }
 var duelHistory by remember { mutableStateOf(prefs.getString("duel_history", "") ?: "") }
 var sessions by remember { mutableIntStateOf(prefs.getInt("sessions",0)) }
 var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score",0)) }
 var lastScore by remember { mutableIntStateOf(prefs.getInt("last_score",0)) }
 var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
 var showFavoritesOnly by remember { mutableStateOf(false) }
 val savedReferenceKey=prefs.getString("reference_song_key","") ?: ""
 val currentSongKey="${selected.artist}|${selected.title}"
 var referenceCsv by remember { mutableStateOf(if(savedReferenceKey==currentSongKey) prefs.getString("reference_csv","") ?: "" else "") }
 var referenceNotes by remember { mutableStateOf(if(referenceCsv.isBlank()) emptyList() else runCatching{MelodyReferenceParser.parseCsv(referenceCsv)}.getOrDefault(emptyList())) }
 var midiTracks by remember { mutableStateOf(emptyList<MidiTrackOption>()) }
 var selectedMidiTrackIndex by remember { mutableIntStateOf(0) }
 var midiError by remember { mutableStateOf("") }
 var referenceError by remember { mutableStateOf("") }
 var pitchTrace by remember { mutableStateOf(emptyList<PitchTracePoint>()) }
 var scoreSnapshot by remember { mutableStateOf(ScoringSnapshot(totalNotes=referenceNotes.size)) }
 var scoringAvailable by remember { mutableStateOf(false) }
 var playbackStarted by remember { mutableStateOf(false) }
 val scorer=remember(referenceNotes,duelRound,shift){MelodyScorer(referenceNotes,shift)}
 val displayReferenceNotes=remember(referenceNotes,shift){referenceNotes.map{it.copy(midiNote=(it.midiNote+shift).coerceIn(0,127))}}
 LaunchedEffect(profileName,opponent,mode,selected,shift,favorites){
  prefs.edit().putString("profile_name",profileName.trim()).putString("opponent_name",opponent.trim())
   .putString("mode",mode.name).putString("selected_title",selected.title)
   .putString("selected_artist",selected.artist).putInt("transpose",shift)
   .putStringSet("favorites",favorites).apply()
 }
 val lyricPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) try {
   context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
   lyricLines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { parseLrc(it.readText()) } ?: emptyList()
  } catch (_: Exception) { lyricLines = emptyList() }
 }
 LaunchedEffect(page, playerPlaying) {
  while (page == Page.SING && playerPlaying) { currentPositionMs = player.currentPosition; delay(100) }
 }
 val referencePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) try {
   val raw=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}
    ?: throw IllegalArgumentException("No se pudo leer el archivo.")
   val parsed=MelodyReferenceParser.parseCsv(raw)
   referenceCsv=raw
   referenceNotes=parsed
   midiTracks=emptyList()
   midiError=""
   referenceError=""
   scoreSnapshot=ScoringSnapshot(totalNotes=parsed.size)
   prefs.edit().putString("reference_csv",raw).putString("reference_song_key","${selected.artist}|${selected.title}").apply()
  } catch(error:Exception) {
   referenceError=error.message ?: "No se pudo importar la melodía."
   referenceCsv=""
   referenceNotes=emptyList()
   prefs.edit().remove("reference_csv").remove("reference_song_key").apply()
  }
 }
 val midiPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) try {
   val bytes=context.contentResolver.openInputStream(uri)?.use { input ->
    val output=ByteArrayOutputStream()
    val buffer=ByteArray(8_192)
    var total=0
    while(true){
     val count=input.read(buffer)
     if(count<0) break
     total+=count
     require(total<=5_242_880) { "El MIDI supera el límite de 5 MB." }
     output.write(buffer,0,count)
    }
    output.toByteArray()
   } ?: throw IllegalArgumentException("No se pudo leer el archivo MIDI.")
   val parsed=MidiReferenceParser.parse(bytes)
   midiTracks=parsed.tracks
   selectedMidiTrackIndex=0
   midiError=""
  } catch(error:Exception) {
   midiTracks=emptyList()
   selectedMidiTrackIndex=0
   midiError=error.message ?: "No se pudo importar el MIDI."
  }
 }
 var audioError by remember { mutableStateOf("") }
 val audioPicker= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null) try {
   context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
   selectedAudioUri=uri.toString()
   prefs.edit().putString("selected_audio_uri",uri.toString()).apply()
   referenceCsv=""
   referenceNotes=emptyList()
   referenceError=""
   prefs.edit().remove("reference_csv").remove("reference_song_key").apply()
   if(page!=Page.SETUP){
    val displayName=uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Audio local"
    selected=Song(displayName,"Archivo local","No verificada","Audio local","Local")
    lyricLines=emptyList()
   }
   playbackStarted=false
   audioError=""
   shift=0
   page=Page.SETUP
  } catch(error:Exception) {
   audioError=error.message ?: "No se pudo conservar el acceso al archivo de audio."
  }
 }
 fun openTvCast(){
  try { context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS)) }
  catch (_:Exception) { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
 }
 fun clearReference(){
  referenceCsv=""
  referenceNotes=emptyList()
  midiTracks=emptyList()
  selectedMidiTrackIndex=0
  midiError=""
  referenceError=""
  scoreSnapshot=ScoringSnapshot()
  prefs.edit().remove("reference_csv").remove("reference_song_key").apply()
 }
 fun selectSong(song:Song){
  selected=song
  selectedAudioUri=null
  prefs.edit().remove("selected_audio_uri").apply()
  player.stop()
  playerPlaying=false
  playbackStarted=false
  clearReference()
  lyricLines=emptyList()
  shift=0
  page=Page.SETUP
 }
 fun finishRound(){
  val canScore=selectedAudioUri!=null && referenceNotes.isNotEmpty() && playbackStarted && player.currentPosition>0L
  scoreSnapshot=if(canScore) scorer.finish() else ScoringSnapshot(totalNotes=referenceNotes.size)
  points=if(canScore) scoreSnapshot.totalPoints else 0
  player.pause()
  playerPlaying=false
  if(canScore && mode==PlayMode.DUEL && duelRound==1){
   firstDuelScore=points
   duelRound=2
   points=0
   scoreSnapshot=ScoringSnapshot(totalNotes=referenceNotes.size)
   livePitch=null
   pitchTrace=emptyList()
   currentPositionMs=0L
   player.seekTo(0)
   player.play()
   playerPlaying=true
  } else {
   scoringAvailable=canScore
   if(canScore){
    sessions++
    secondDuelScore=if(mode==PlayMode.DUEL) points else 0
    lastScore=if(mode==PlayMode.DUEL) maxOf(firstDuelScore,secondDuelScore) else points
    if(lastScore>bestScore) bestScore=lastScore
    val editor=prefs.edit()
     .putInt("last_score",lastScore)
     .putInt("best_score",bestScore)
     .putInt("sessions",sessions)
    if(mode==PlayMode.DUEL){
     val firstKey="profile_best_"+profileName.trim().ifBlank{"Jugador 1"}
     val secondKey="profile_best_"+opponent.trim().ifBlank{"Jugador 2"}
     duelHistory=(duelHistory+"\n"+profileName.ifBlank{"Jugador 1"}+": "+firstDuelScore+" - "+opponent.ifBlank{"Jugador 2"}+": "+secondDuelScore).takeLast(4000)
     editor.putString("duel_history",duelHistory)
      .putInt(firstKey,maxOf(prefs.getInt(firstKey,0),firstDuelScore))
      .putInt(secondKey,maxOf(prefs.getInt(secondKey,0),secondDuelScore))
    } else {
     val profileKey="profile_best_"+profileName.trim().ifBlank{"Invitado"}
     editor.putInt(profileKey,maxOf(prefs.getInt(profileKey,0),points))
    }
    editor.apply()
   }
   page=Page.RESULT
  }
 }
 fun useSelectedMidiTrack(){
  val track=midiTracks.getOrNull(selectedMidiTrackIndex) ?: return
  val csv=buildString {
   append("start_ms,duration_ms,midi_note\n")
   track.referenceNotes.forEach { note ->
    append(note.startMs).append(',').append(note.durationMs).append(',').append(note.midiNote).append('\n')
   }
  }
  referenceCsv=csv
  referenceNotes=track.referenceNotes
  referenceError=""
  scoreSnapshot=ScoringSnapshot(totalNotes=track.referenceNotes.size)
  prefs.edit().putString("reference_csv",csv).putString("reference_song_key","${selected.artist}|${selected.title}").apply()
 }
 fun beginSession(){
  points=0
  duelRound=1
  firstDuelScore=0
  secondDuelScore=0
  scoreSnapshot=ScoringSnapshot(totalNotes=referenceNotes.size)
  pitchTrace=emptyList()
  livePitch=null
  scoringAvailable=false
  playbackStarted=false
  currentPositionMs=0L
  if(selectedAudioUri!=null){
   player.seekTo(0)
   player.play()
   playerPlaying=true
   playbackStarted=true
  }
  page=Page.SING
 }
 val micPermissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  micPermissionGranted=granted
  beginSession()
 }
 fun startSession(){
  micPermissionGranted=context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
  if(micPermissionGranted) beginSession()
  else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
 }
 fun saveProfile(){
  prefs.edit().putString("profile_name",profileName.trim()).putString("opponent_name",opponent.trim())
   .putString("mode",mode.name).putStringSet("favorites",favorites).apply()
 }
 fun goBack(){
  if(page==Page.SING){player.pause();playerPlaying=false}
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
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A4B),bg)))){
   when(page){
    Page.HOME->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Box(Modifier.size(142.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF00D9FF),purple,Color(0xFFE735D4)))),contentAlignment=Alignment.Center){Icon(Icons.Default.Mic,null,tint=Color.White,modifier=Modifier.size(88.dp))}
     Text("Cantar",fontSize=48.sp,fontWeight=FontWeight.Black);Text("DAR LA NOTA",color=gold,fontSize=22.sp,fontWeight=FontWeight.ExtraBold)
     Spacer(Modifier.height(22.dp));Text(if(profileName.isBlank())"Creá tu perfil para guardar tus resultados" else "Hola, ${profileName}",color=muted)
     HomeButton("♫  Elegir canción",Brush.horizontalGradient(listOf(purple,magenta))){showFavoritesOnly=false;previousPage=Page.HOME;page=Page.LIBRARY}
     HomeButton("▣  Conectar a Smart TV",Brush.horizontalGradient(listOf(electricBlue,cyan))){openTvCast()}
     HomeButton("🏆  Solo / Duelo",Brush.horizontalGradient(listOf(Color(0xFF00C853),green))){page=Page.PROFILE}
     HomeButton("★  Mis Favoritas",Brush.horizontalGradient(listOf(orange,Color(0xFFFF5A00)))){style="Todos";lang="Todos";query="";showFavoritesOnly=true;page=Page.LIBRARY}
     HomeButton("▤  Historial y puntuaciones",Brush.horizontalGradient(listOf(electricBlue,Color(0xFF165DDB)))){page=Page.HISTORY}
     HomeButton("⚙  Configuración",Brush.horizontalGradient(listOf(Color(0xFF087DFF),Color(0xFF1646C8)))){page=Page.SETTINGS}
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
     Text("Mejor marca de ${profileName.ifBlank{"Invitado"}}: ${prefs.getInt("profile_best_"+profileName.trim().ifBlank{"Invitado"},0)} puntos",color=green,fontSize=12.sp)
     if(mode==PlayMode.DUEL)Text("Mejor marca de ${opponent.ifBlank{"Jugador 2"}}: ${prefs.getInt("profile_best_"+opponent.trim().ifBlank{"Jugador 2"},0)} puntos",color=gold,fontSize=12.sp)
     Text("Los datos del perfil, la modalidad y los resultados se guardan en este dispositivo.",color=muted,fontSize=12.sp)
     Spacer(Modifier.weight(1f));HomeButton("Guardar perfil",green){saveProfile();page=Page.HOME}
    }
    Page.LIBRARY->{
     val filtered=library.filter{(style=="Todos"||it.genre==style)&&(lang=="Todos"||it.lang==lang||(lang=="Otros"&&it.lang !in listOf("Español","Inglés")))&&(!showFavoritesOnly||favorites.contains("${it.artist}|${it.title}"))&&(it.title.contains(query,true)||it.artist.contains(query,true)||it.genre.contains(query,true))}
     Column(Modifier.fillMaxSize().padding(14.dp)){
      Header(if(showFavoritesOnly)"Mis favoritas" else "Seleccionar canción"){showFavoritesOnly=false;goBack()}
      Text("Ejemplos de catálogo: no incluyen audio ni licencia musical.",color=muted,fontSize=11.sp)
      OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar canción...")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
      Text("IDIOMA",color=cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Todos","Español","Inglés","Francés","Otros").forEach{FilterChip(lang==it,{lang=it},{Text(it)})}}
      Text("ESTILOS MUSICALES",color=cyan,fontSize=12.sp,fontWeight=FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=5.dp)){styles.forEach{FilterChip(style==it,{style=it},{Text(it,fontSize=12.sp)})}}
      Row(verticalAlignment=Alignment.CenterVertically){Text(if(searchingOnline)"Buscando letras en LRCLIB…" else "${remoteTracks.size} letras coincidentes · ${filtered.size} ejemplos",color=muted,modifier=Modifier.weight(1f));TextButton({audioPicker.launch(arrayOf("audio/*"))}){Icon(Icons.Default.LibraryMusic,null);Text(" Importar audio")}}
      Text("Los resultados LRCLIB aportan letras; no son pistas de audio ni un catálogo por género.",color=muted,fontSize=10.sp)
      if(remoteError.isNotBlank())Text(remoteError,color=gold,fontSize=11.sp)
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
       items(remoteTracks,key={ "lrc-${it.id}" }){track->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(panel).clickable{
        selectSong(Song(track.trackName,track.artistName,"No verificada","Sin clasificar","Sin clasificar"));lyricLines=parseLrc(track.syncedLyrics.orEmpty())
       }.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.MusicNote,null,tint=green,modifier=Modifier.size(34.dp));Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(track.trackName,fontWeight=FontWeight.Bold);Text(track.artistName,color=muted,fontSize=12.sp);Text(if(track.syncedLyrics!=null)"Letra sincronizada disponible" else if(track.plainLyrics!=null)"Letra disponible (sin tiempos)" else "Sin letra disponible",color=cyan,fontSize=11.sp)};Text("ONLINE",color=green,fontSize=10.sp)
       }}
       items(filtered){item->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(panel).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
         Row(Modifier.weight(1f).clickable{selectSong(item)},verticalAlignment=Alignment.CenterVertically){
          Icon(Icons.Default.MusicNote,null,tint=cyan,modifier=Modifier.size(34.dp))
          Column(Modifier.weight(1f).padding(horizontal=10.dp)){
           Text(item.title,fontWeight=FontWeight.Bold);Text(item.artist,color=muted,fontSize=12.sp)
           Text(item.genre+" · "+item.lang+" · ejemplo",color=cyan,fontSize=10.sp)
          }
         }
         val favoriteKey="${item.artist}|${item.title}"
         IconButton(onClick={favorites=if(favorites.contains(favoriteKey))favorites-favoriteKey else favorites+favoriteKey}){
          Icon(if(favorites.contains(favoriteKey))Icons.Default.Star else Icons.Default.StarBorder,null,tint=gold)
         }
        }
       }
       if(filtered.isEmpty())item{Text("Sin resultados. Cambiá los filtros o importá una pista.",color=muted)}
      }
     }
    }
    Page.SETUP->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Header("Configurar canción"){page=Page.LIBRARY}
     Spacer(Modifier.height(22.dp));Panel { Text(selected.title,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(selected.artist,color=muted);Text("Tonalidad original: no verificada",color=gold) }
     Spacer(Modifier.height(12.dp))
     Button(onClick={
      val terms=android.net.Uri.encode("${selected.artist} ${selected.title} karaoke instrumental")
      try { context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.youtube.com/results?search_query=$terms"))) } catch (_:Exception) {}
     },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=purple)){Text("▶ Buscar pista karaoke en YouTube",fontWeight=FontWeight.Bold)}
     Text("Se abre la búsqueda oficial de YouTube; elegí un video karaoke. LRCLIB aporta la letra, pero no el audio.",color=muted,fontSize=11.sp)
     OutlinedButton(onClick={audioPicker.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){Text(if(selectedAudioUri==null)"Importar pista de audio" else "Cambiar pista de audio")}
     if(audioError.isNotBlank())Text(audioError,color=gold,fontSize=11.sp)
     if(playerError.isNotBlank())Text("No se pudo reproducir la pista: $playerError",color=gold,fontSize=11.sp)
     OutlinedButton(onClick={referencePicker.launch(arrayOf("text/csv","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("Importar melodía de referencia (.CSV)")}
     Text(if(referenceNotes.isEmpty())"Sin melodía de referencia: no se calcularán puntos por nota." else "Melodía cargada: ${referenceNotes.size} notas · puntuación nota por nota activa.",color=if(referenceNotes.isEmpty())gold else green,fontSize=11.sp)
     Text("Formato CSV: start_ms,duration_ms,midi_note. Los tiempos se miden desde el inicio del audio.",color=muted,fontSize=10.sp)
     OutlinedButton(onClick={midiPicker.launch(arrayOf("audio/midi","audio/x-midi","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("Importar melodía MIDI (.mid)") }
     if(midiError.isNotBlank())Text(midiError,color=gold,fontSize=11.sp)
     if(midiTracks.isNotEmpty()){
      Text("Elegí una pista MIDI. En pistas con acordes se toma la nota más aguda activa como guía monofónica.",color=muted,fontSize=10.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
       midiTracks.forEachIndexed { index, track ->
        FilterChip(selected=selectedMidiTrackIndex==index,onClick={selectedMidiTrackIndex=index},label={Text("${track.name} · ${track.referenceNotes.size} notas")})
       }
      }
      OutlinedButton(onClick={useSelectedMidiTrack()},modifier=Modifier.fillMaxWidth()){Text("Usar pista MIDI seleccionada")}
     }
     if(referenceError.isNotBlank())Text(referenceError,color=gold,fontSize=11.sp)
     if(selectedAudioUri!=null){
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       Button(onClick={if(player.isPlaying){player.pause();playerPlaying=false}else{player.play();playerPlaying=true}}){Text(if(player.isPlaying)"Pausar pista" else "▶ Reproducir pista")}
       OutlinedButton(onClick={openTvCast()}){Text("▣ TV")}
      }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
       OutlinedButton(onClick={lyricPicker.launch(arrayOf("text/plain", "application/octet-stream"))}) { Text("Importar letra .LRC") }
       Text(if(lyricLines.isEmpty()) "Sin letra sincronizada" else "${lyricLines.size} líneas", color=if(lyricLines.isEmpty()) gold else green, fontSize=11.sp)
      }
      Text("La letra acompaña el tiempo de la pista cuando importás su archivo .LRC.",color=muted,fontSize=12.sp)
     } else { Text("Importá un archivo de audio para reproducirlo y cambiar su tono. El catálogo de muestra no incluye grabaciones.",color=muted,fontSize=12.sp) }
     Spacer(Modifier.height(10.dp));Text("ELEGIR TONO",color=cyan,fontWeight=FontWeight.Bold)
     Text(if(shift==0)"Tono original no verificado" else "Transposición ${if(shift>0)"+" else ""}$shift semitonos",fontSize=20.sp,color=green,fontWeight=FontWeight.Bold)
     Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(-2,-1,0,1,2).forEach{n->OutlinedButton({shift=if(n==0)0 else (shift+n).coerceIn(-12,12)}){Text(if(n==0)"Normal" else if(n<0)"Más bajo\n($n)" else "Más alto\n(+$n)")}}}
     Text("Ajuste de −12 a +12 semitonos",color=muted,fontSize=12.sp)
     Spacer(Modifier.height(16.dp));HomeButton("▶  Comenzar a cantar",green){startSession()}
    }
    Page.SING->BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)){
     val compact=maxHeight<500.dp
     val lyricIndex=lyricLines.indexOfLast{it.timeMs<=currentPositionMs}
     val currentLyric=if(lyricLines.isEmpty())"La letra sincronizada aparecerá aquí al conectar una pista online." else lyricLines.getOrNull(lyricIndex)?.text?.ifBlank{"♪"}?:"Preparados..."
     val nextLyric=lyricLines.getOrNull(lyricIndex+1)?.text.orEmpty()
     val targetNote=scorer.targetAt(currentPositionMs)
     val targetError=if(targetNote!=null && livePitch!=null) (livePitch!!.midiNote-targetNote.midiNote)*100f+livePitch!!.cents else null
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)){
      Row(Modifier.fillMaxWidth().height(if(compact)30.dp else 38.dp),verticalAlignment=Alignment.CenterVertically){
       Text("♫  ${selected.title} · ${selected.artist}",fontWeight=FontWeight.ExtraBold,fontSize=if(compact)13.sp else 17.sp,modifier=Modifier.weight(1f),maxLines=1)
       ScorePill("PUNTOS",points.toString())
       TextButton(onClick={openTvCast()},contentPadding=PaddingValues(horizontal=5.dp)){Text("▣ TV",color=cyan)}
      }
      Panel(Modifier.fillMaxWidth().height(if(compact)if(nextLyric.isBlank())58.dp else 72.dp else 112.dp)){
       Text("LETRA EN VIVO",color=cyan,fontWeight=FontWeight.Bold,fontSize=if(compact)9.sp else 11.sp)
       Text(currentLyric,fontSize=if(compact)17.sp else 24.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,maxLines=1)
       if(nextLyric.isNotBlank())Text(nextLyric,color=muted,fontSize=if(compact)11.sp else 15.sp,maxLines=1)
      }
      MelodyPitchChart(displayReferenceNotes,pitchTrace,currentPositionMs,Modifier.fillMaxWidth())
      Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
       Panel(Modifier.weight(1f).fillMaxHeight()){
        Text("NOTAS OBJETIVO",color=cyan,fontWeight=FontWeight.Bold,fontSize=if(compact)10.sp else 12.sp)
        Box(Modifier.weight(1f).fillMaxWidth().padding(top=6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF080F29)),contentAlignment=Alignment.Center){
         Column(horizontalAlignment=Alignment.CenterHorizontally){
          Text(targetNote?.let{midiNoteName(it.midiNote)}?:"—",color=cyan,fontSize=if(compact)27.sp else 36.sp,fontWeight=FontWeight.Black)
          Text("Nota objetivo",color=muted,fontSize=10.sp)
          Text(if(targetNote==null)"Sin nota en este instante" else "Referencia MIDI ${targetNote.midiNote}",color=muted,fontSize=9.sp)
         }
        }
       }
       Panel(Modifier.weight(1f).fillMaxHeight()){
        Text("AFINACIÓN EN VIVO",color=cyan,fontWeight=FontWeight.Bold,fontSize=if(compact)10.sp else 12.sp)
        Text(livePitch?.let{"${it.noteName} · ${it.frequencyHz.toInt()} Hz"}?:"Esperando voz",fontSize=if(compact)16.sp else 21.sp,fontWeight=FontWeight.ExtraBold,color=targetError?.let{if(kotlin.math.abs(it)<=50f)green else gold}?:muted)
        Text(targetError?.let{"${it.toInt()} cents respecto al objetivo"}?:"",color=muted,fontSize=10.sp)
        Spacer(Modifier.weight(1f))
        VocalPitchCapture(onPitch={pitch->
         livePitch=pitch
         val position=player.currentPosition
         if(player.isPlaying && referenceNotes.isNotEmpty()){
          val target=scorer.targetAt(position)
          scoreSnapshot=scorer.observe(position,pitch)
          points=scoreSnapshot.totalPoints
          if(pitch!=null){
           val error=if(target==null)Float.MAX_VALUE else (pitch.midiNote-target.midiNote)*100f+pitch.cents
           pitchTrace=(pitchTrace+PitchTracePoint(position,pitch.midiNote+pitch.cents/100f,target!=null && kotlin.math.abs(error)<=50f)).takeLast(240)
          }
         }
        },autoStart=micPermissionGranted,onPermissionGranted={micPermissionGranted=true},modifier=Modifier.fillMaxWidth())
       }
      }
      if(!compact)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
       Text(if(playerPlaying)"● REPRODUCIENDO" else "PAUSADO",color=if(playerPlaying)green else gold,fontSize=11.sp,modifier=Modifier.weight(1f))
       TextButton(onClick={if(player.isPlaying){player.pause();playerPlaying=false}else if(selectedAudioUri!=null){player.play();playerPlaying=true}}){Text(if(playerPlaying)"Pausar" else "Reproducir",color=cyan)}
       TextButton(onClick={finishRound()}){Text(if(mode==PlayMode.DUEL && duelRound==1)"Finalizar turno 1" else "Finalizar",color=green)}
      }
     }
    }
    Page.RESULT->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
     Icon(Icons.Default.EmojiEvents,null,tint=gold,modifier=Modifier.size(90.dp));Text("¡Resultado!",fontSize=30.sp,fontWeight=FontWeight.Bold)
     Text(if(!scoringAvailable)"Puntuación no disponible" else if(mode==PlayMode.DUEL) "${firstDuelScore}  —  ${secondDuelScore}" else lastScore.toString(),fontSize=if(scoringAvailable)52.sp else 24.sp,color=if(scoringAvailable)green else gold,fontWeight=FontWeight.Black)
     Text(
      if(scoringAvailable) "puntos · ${if(mode==PlayMode.SOLO)"Partida individual" else "Duelo"}"
      else when {
       selectedAudioUri==null -> "Importá una pista de audio local para evaluar."
       referenceNotes.isEmpty() -> "Importá la melodía de referencia correspondiente a esta pista."
       !playbackStarted -> "La pista no llegó a iniciarse; no se calculó una puntuación."
       else -> "No se pudo completar la evaluación de audio."
      },
      color=muted
     )
     if(scoringAvailable)Text("${scoreSnapshot.correctNotes} notas afinadas de ${scoreSnapshot.evaluatedNotes} evaluadas · ${scoreSnapshot.accuracyPercent}% de acierto",color=cyan,fontSize=13.sp)
     if(scoringAvailable && scoreSnapshot.detectedNotes==0)Text("No se detectó voz durante la pista; revisá el permiso del micrófono.",color=gold,fontSize=12.sp)
     if(mode==PlayMode.DUEL && scoringAvailable){
      Text("${profileName.ifBlank{"Jugador 1"}}  vs  ${opponent.ifBlank{"Jugador 2"}}",color=cyan)
      Text("Resultado del duelo: ${firstDuelScore} - ${secondDuelScore}",color=gold)
      Text(if(firstDuelScore>secondDuelScore) "Ganó ${profileName.ifBlank{"Jugador 1"}}" else if(secondDuelScore>firstDuelScore) "Ganó ${opponent.ifBlank{"Jugador 2"}}" else "Empate",color=green)
      Text("Historial local guardado",color=muted,fontSize=12.sp)
     } else if(mode==PlayMode.DUEL){
      Text("${profileName.ifBlank{"Jugador 1"}}  vs  ${opponent.ifBlank{"Jugador 2"}}",color=cyan)
      if(duelRound==2)Text("Turno 1: ${firstDuelScore} puntos · turno 2 sin evaluar",color=gold,fontSize=12.sp)
      Text("Duelo incompleto: no se declaró ganador ni se guardó un resultado ficticio.",color=gold,fontSize=12.sp)
     }
     if(scoringAvailable)Text("Mejor marca: $bestScore · Sesiones: $sessions",color=gold)
     HomeButton("Volver al menú",green){page=Page.HOME};HomeButton("Otra canción",purple){page=Page.LIBRARY}
    }
    Page.HISTORY->Column(Modifier.fillMaxSize().padding(20.dp)){
     Header("Historial del perfil"){page=Page.HOME}
     Panel{Text(profileName.ifBlank{"Perfil sin nombre"},fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Mejor puntuación global: $bestScore",color=green);Text("Mejor marca del perfil: ${prefs.getInt("profile_best_"+profileName.trim().ifBlank{"Invitado"},0)}",color=cyan);if(mode==PlayMode.DUEL)Text("Mejor marca del rival: ${prefs.getInt("profile_best_"+opponent.trim().ifBlank{"Jugador 2"},0)}",color=gold);Text("Última puntuación: $lastScore",color=gold);Text("Partidas guardadas: $sessions",color=muted);if(duelHistory.isNotBlank()){Text("DUELOS RECIENTES",color=cyan,fontWeight=FontWeight.Bold);Text(duelHistory,color=muted)}}
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
@Composable private fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.clip(RoundedCornerShape(18.dp)).background(panel).padding(if(modifier==Modifier)16.dp else 10.dp),content=content)}
@Composable private fun ModeCard(title:String,subtitle:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){Column(modifier.clip(RoundedCornerShape(16.dp)).background(if(selected)Color(0xFF233A70) else panel).clickable(onClick = onClick).padding(13.dp)){Text(title,color=if(selected)cyan else Color.White,fontWeight=FontWeight.Bold);Text(subtitle,color=muted,fontSize=11.sp)}}
@Composable private fun ScorePill(label:String,value:String){Column(Modifier.clip(RoundedCornerShape(13.dp)).background(panel).padding(horizontal=16.dp, vertical=9.dp)){Text(label,color=cyan,fontSize=11.sp);Text(value,color=green,fontWeight=FontWeight.Bold,fontSize=20.sp)}}
@Composable private fun HomeButton(label:String,brush:Brush,onClick:()->Unit){Box(Modifier.fillMaxWidth().padding(vertical=5.dp).height(54.dp).clip(RoundedCornerShape(16.dp)).background(brush).clickable(onClick=onClick),contentAlignment=Alignment.Center){Text(label,fontWeight=FontWeight.Bold,fontSize=15.sp,color=Color.White)}}
@Composable private fun HomeButton(label:String,color:Color,onClick:()->Unit)=HomeButton(label,Brush.horizontalGradient(listOf(color,color.copy(alpha=0.82f))),onClick)
