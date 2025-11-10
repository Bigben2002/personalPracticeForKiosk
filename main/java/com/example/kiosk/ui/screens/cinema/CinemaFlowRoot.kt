package com.example.kiosk.ui.screens.cinema

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiosk.ui.components.KioskCard
import com.example.kiosk.ui.components.KioskButton
import java.util.Locale
import kotlin.math.min

enum class CinemaStage { HOME, BOOKING, SEAT, PAYMENT, PRINT }

data class MovieItem(
    val id: String,
    val title: String,
    val posterName: String, // drawable 이름(확정 전에도 안전). 예: "poster_avengers"
    val runningTimeMin: Int,
    val showTimes: List<String>,
    val totalSeats: Int = 120,
    val remainingSeats: Int = 45
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaFlowRoot(
    isPracticeMode: Boolean,
    onExit: () -> Unit
) {
    var stage by remember { mutableStateOf(CinemaStage.HOME) }

    val todayMillis = remember { System.currentTimeMillis() }
    var bookingDateMillis by remember { mutableStateOf(todayMillis) }

    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var peopleCount by remember { mutableStateOf(1) }

    var selectedSeats by remember { mutableStateOf<Set<String>>(emptySet()) }

    val barColor = Color(0xFF334155)

    @Composable
    fun PracticeBanner(text: String) {
        if (isPracticeMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2563EB))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) { Text(text, color = Color.White, fontSize = 14.sp) }
        }
    }

    fun resetFlow() {
        stage = CinemaStage.HOME
        bookingDateMillis = todayMillis
        selectedMovie = null
        selectedTime = null
        peopleCount = 1
        selectedSeats = emptySet()
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("영화관", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                },
                actions = {
                    if (stage != CinemaStage.HOME) {
                        IconButton(onClick = { resetFlow() }) {
                            Icon(Icons.Default.Home, contentDescription = "홈", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = barColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            when (stage) {
                CinemaStage.HOME -> {
                    PracticeBanner("먼저 ‘티켓 구매’를 눌러주세요")
                    CinemaHome(
                        onTicket = { stage = CinemaStage.BOOKING },
                        onPrint = { stage = CinemaStage.PRINT },
                        onRefund = { /* 확장 예정 */ },
                        onSnack = { /* 확장 예정 */ }
                    )
                }

                CinemaStage.BOOKING -> {
                    PracticeBanner("예매일을 고르고, 영화 포스터에서 상영시간을 선택하세요")
                    BookingScreen(
                        bookingDateMillis = bookingDateMillis,
                        onChangeDate = { bookingDateMillis = it },
                        movies = rememberMovies(),
                        selectedMovie = selectedMovie,
                        onSelectMovie = { selectedMovie = it },
                        selectedTime = selectedTime,
                        onSelectTime = { selectedTime = it },
                        peopleCount = peopleCount,
                        onPeopleInc = { peopleCount = min(8, peopleCount + 1) },
                        onPeopleDec = { peopleCount = maxOf(1, peopleCount - 1) },
                        onNext = { stage = CinemaStage.SEAT },
                        onBack = { stage = CinemaStage.HOME }
                    )
                }

                CinemaStage.SEAT -> {
                    PracticeBanner("인원 수만큼 좌석을 선택한 뒤 ‘결제하기’를 눌러주세요")
                    SeatSelectScreen(
                        peopleCount = peopleCount,
                        selectedSeats = selectedSeats,
                        onToggleSeat = { seatCode ->
                            selectedSeats = if (selectedSeats.contains(seatCode)) {
                                selectedSeats - seatCode
                            } else {
                                if (selectedSeats.size < peopleCount) selectedSeats + seatCode else selectedSeats
                            }
                        },
                        onNext = { stage = CinemaStage.PAYMENT },
                        onBack = { stage = CinemaStage.BOOKING }
                    )
                }

                CinemaStage.PAYMENT -> {
                    PracticeBanner("결제 수단을 선택하세요 (카드/QR)")
                    PaymentScreen(
                        onPaid = { stage = CinemaStage.PRINT },
                        onBack = { stage = CinemaStage.SEAT }
                    )
                }

                CinemaStage.PRINT -> {
                    PracticeBanner("티켓이 출력됩니다 (시연)")
                    TicketPrintScreen(
                        movie = selectedMovie,
                        time = selectedTime,
                        seats = selectedSeats.toList().sorted(),
                        dateMillis = bookingDateMillis,
                        onDone = onExit,
                        onAgain = { resetFlow() }
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------
 * HOME
 * ------------------------------------------------------------- */
@Composable
private fun CinemaHome(
    onTicket: () -> Unit,
    onPrint: () -> Unit,
    onRefund: () -> Unit,
    onSnack: () -> Unit
) {
    val items = listOf(
        "🎟️ 티켓 구매" to onTicket,
        "🧾 예매티켓 출력" to onPrint,
        "↩️ 환불" to onRefund,
        "🍿 음식" to onSnack
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { (label, action) ->
            ElevatedCard(onClick = action) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* -------------------------------------------------------------
 * BOOKING
 * ------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingScreen(
    bookingDateMillis: Long,
    onChangeDate: (Long) -> Unit,
    movies: List<MovieItem>,
    selectedMovie: MovieItem?,
    onSelectMovie: (MovieItem) -> Unit,
    selectedTime: String?,
    onSelectTime: (String) -> Unit,
    peopleCount: Int,
    onPeopleInc: () -> Unit,
    onPeopleDec: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var datePickerOpen by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = bookingDateMillis)

    val dateText = remember(bookingDateMillis) {
        SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).format(bookingDateMillis)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { datePickerOpen = true },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("예매일 변경 • $dateText")
                }
                OutlinedButton(
                    onClick = { /* 상영시간표 보기: 각 카드의 시간 Row로 표현 */ },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("상영시간표 보기")
                }
            }

            PeopleSelectorBar(
                count = peopleCount,
                onDec = onPeopleDec,
                onInc = onPeopleInc
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(movies) { movie ->
                        MovieCard(
                            movie = movie,
                            selected = movie.id == selectedMovie?.id,
                            selectedTime = if (movie.id == selectedMovie?.id) selectedTime else null,
                            onClickPoster = { onSelectMovie(movie) },
                            onSelectTime = { time ->
                                onSelectMovie(movie)
                                onSelectTime(time)
                            }
                        )
                    }
                }
            )
        }

        val enabled = selectedMovie != null && selectedTime != null
        KioskButton(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .height(56.dp),
            enabled = enabled
        ) {
            Text("좌석 선택", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (datePickerOpen) {
            DatePickerDialog(
                onDismissRequest = { datePickerOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = dateState.selectedDateMillis
                        if (picked != null) onChangeDate(picked)
                        datePickerOpen = false
                    }) { Text("확인") }
                },
                dismissButton = {
                    TextButton(onClick = { datePickerOpen = false }) { Text("취소") }
                }
            ) {
                DatePicker(state = dateState)
            }
        }
    }
}

@Composable
private fun PeopleSelectorBar(
    count: Int,
    onDec: () -> Unit,
    onInc: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("인원 선택", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDec, enabled = count > 1) { Text("－") }
            Text("$count 명", modifier = Modifier.width(72.dp), textAlign = TextAlign.Center, fontSize = 18.sp)
            OutlinedButton(onClick = onInc, enabled = count < 8) { Text("＋") }
        }
    }
}

@Composable
private fun MovieCard(
    movie: MovieItem,
    selected: Boolean,
    selectedTime: String?,
    onClickPoster: () -> Unit,
    onSelectTime: (String) -> Unit
) {
    val borderColor = if (selected) Color(0xFF2563EB) else Color(0xFFE5E7EB)

    KioskCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderColor,
        onClick = onClickPoster
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val label = if (movie.posterName.isNotBlank()) movie.posterName else movie.title
                Text(label, fontSize = 16.sp, color = Color(0xFF374151))
            }

            Spacer(Modifier.height(8.dp))
            Text(movie.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${movie.runningTimeMin}분", fontSize = 12.sp, color = Color(0xFF6B7280))
            Text("잔여 ${movie.remainingSeats}/${movie.totalSeats}", fontSize = 12.sp, color = Color(0xFF6B7280))

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(movie.showTimes) { t ->
                    val isSel = selectedTime == t && selected
                    AssistChip(
                        onClick = { onSelectTime(t) },
                        label = { Text(t, fontSize = 12.sp) },
                        shape = RoundedCornerShape(6.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSel) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                            labelColor = if (isSel) Color.White else Color(0xFF374151)
                        )
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------
 * SEAT
 * ------------------------------------------------------------- */
@Composable
private fun SeatSelectScreen(
    peopleCount: Int,
    selectedSeats: Set<String>,
    onToggleSeat: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val rows = ('A'..'J').map { it.toString() }
    val cols = (1..12).toList()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("좌석 선택", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("선택 ${selectedSeats.size}/$peopleCount", color = Color(0xFF2563EB))
            }
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { Text("SCREEN", fontSize = 12.sp, color = Color(0xFF6B7280)) }

            Spacer(Modifier.height(12.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                rows.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        cols.forEach { c ->
                            val seat = "$r$c"
                            val isSelected = seat in selectedSeats
                            SeatChip(
                                code = seat,
                                selected = isSelected,
                                enabled = isSelected || selectedSeats.size < peopleCount,
                                onClick = { onToggleSeat(seat) }
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(Color(0xFFF3F4F6)); Text("빈 좌석", fontSize = 12.sp)
                LegendDot(Color(0xFF2563EB)); Text("선택", fontSize = 12.sp)
                LegendDot(Color(0xFF9CA3AF)); Text("예매불가(예시)", fontSize = 12.sp)
            }
        }

        val enabled = selectedSeats.size == peopleCount
        KioskButton(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .height(56.dp),
            enabled = enabled
        ) { Text("결제하기", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun SeatChip(
    code: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        !enabled && !selected -> Color(0xFF9CA3AF)
        selected -> Color(0xFF2563EB)
        else -> Color(0xFFF3F4F6)
    }
    val fg = if (selected) Color.White else Color(0xFF111827)

    Surface(
        modifier = Modifier.size(28.dp).clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(code, fontSize = 10.sp, color = fg)
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(14.dp).background(color, CircleShape).border(1.dp, Color(0xFFE5E7EB), CircleShape))
}

/* -------------------------------------------------------------
 * PAYMENT
 * ------------------------------------------------------------- */
@Composable
private fun PaymentScreen(
    onPaid: () -> Unit,
    onBack: () -> Unit
) {
    var method by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("결제 방식 선택", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMethodCard(
                title = "카드 결제",
                icon = Icons.Default.SimCard,
                selected = method == "CARD",
                onClick = { method = "CARD" },
                modifier = Modifier.weight(1f) // ✅ weight는 호출부(Row)에서
            )
            PaymentMethodCard(
                title = "QR 결제",
                icon = Icons.Default.QrCodeScanner,
                selected = method == "QR",
                onClick = { method = "QR" },
                modifier = Modifier.weight(1f) // ✅
            )
        }

        Spacer(Modifier.height(20.dp))

        KioskCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                when (method) {
                    "CARD" -> {
                        Text("카드를 리더기에 넣어주세요", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        InstructionImageBox(drawableName = "img_card_reader")
                    }
                    "QR" -> {
                        Text("QR리더기에 QR코드를 맞춰주세요", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        InstructionImageBox(drawableName = "img_qr_reader")
                    }
                    else -> {
                        Text("결제 수단을 선택해주세요", color = Color(0xFF6B7280))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        KioskButton(
            onClick = onPaid,
            modifier = Modifier.align(Alignment.End).height(56.dp),
            enabled = method != null
        ) { Text("결제 완료", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // ✅ modifier 인자 추가
) {
    val border = if (selected) Color(0xFF2563EB) else Color(0xFFE5E7EB)
    KioskCard(
        modifier = modifier, // ✅ 전달받은 modifier 사용
        borderColor = border,
        onClick = onClick
    ) {
        Column(
            Modifier.padding(16.dp).height(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF111827))
            Spacer(Modifier.height(8.dp))
            Text(title)
        }
    }
}

/* -------------------------------------------------------------
 * PRINT
 * ------------------------------------------------------------- */
@SuppressLint("NewApi")
@Composable
private fun TicketPrintScreen(
    movie: MovieItem?,
    time: String?,
    seats: List<String>,
    dateMillis: Long,
    onDone: () -> Unit,
    onAgain: () -> Unit
) {
    val dateText = remember(dateMillis) {
        SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).format(dateMillis)
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("영화표를 출력중입니다...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(28.dp))

        KioskCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("예매 정보", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text("영화: ${movie?.title ?: "-"}")
                Text("일시: $dateText ${time ?: "-"}")
                Text("좌석: ${if (seats.isEmpty()) "-" else seats.joinToString(", ")}")
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onAgain, modifier = Modifier.height(52.dp)) { Text("다시 홈으로") }
            KioskButton(onClick = onDone, modifier = Modifier.height(52.dp)) { Text("완료") }
        }
    }
}

/* -------------------------------------------------------------
 * 보조
 * ------------------------------------------------------------- */
@Composable
private fun InstructionImageBox(
    drawableName: String,
    height: Dp = 160.dp
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("이미지 자리: $drawableName", color = Color(0xFF6B7280))
    }
}

/* -------------------------------------------------------------
 * 더미 데이터
 * ------------------------------------------------------------- */
@Composable
private fun rememberMovies(): List<MovieItem> {
    return remember {
        listOf(
            MovieItem("m1", "인사이드 아웃 2", "poster_movie1", 96, listOf("10:30", "13:00", "15:40", "18:10")),
            MovieItem("m2", "범죄도시 4", "poster_movie2", 109, listOf("09:50", "12:20", "17:00", "20:10")),
            MovieItem("m3", "듄: 파트2", "poster_movie3", 166, listOf("11:10", "14:30", "18:50", "21:40")),
            MovieItem("m4", "소울메이트", "poster_movie4", 125, listOf("10:00", "12:40", "16:10")),
            MovieItem("m5", "어벤저스: 엔드게임", "poster_movie5", 181, listOf("09:00", "13:10", "19:10")),
            MovieItem("m6", "라라랜드", "poster_movie6", 128, listOf("11:20", "14:00", "20:30")),
            MovieItem("m7", "인터스텔라", "poster_movie7", 169, listOf("08:40", "12:10", "16:40", "21:10")),
            MovieItem("m8", "스파이더맨: 노 웨이 홈", "poster_movie8", 148, listOf("10:10", "13:20", "18:00", "20:50")),
            MovieItem("m9", "탑건: 매버릭", "poster_movie9", 130, listOf("09:30", "12:50", "17:20", "20:00"))
        )
    }
}
