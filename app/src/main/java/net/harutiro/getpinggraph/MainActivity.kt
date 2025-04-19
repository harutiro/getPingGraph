package net.harutiro.getpinggraph

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.harutiro.getpinggraph.ui.theme.GetPingGraphTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class pingData(
    val ping: Double,
    val time: Long,
)


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GetPingGraphTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var isStartFlag by remember { mutableStateOf(false) }
                    var pingResult by remember { mutableStateOf("") }
                    val pingList = remember { mutableStateListOf<pingData>() }

                    var pingDataText by remember { mutableStateOf("") }

                    LaunchedEffect(isStartFlag) {
                        // 1秒に一回呼び出し
                        while (isStartFlag) {
                            val server = "8.8.8.8"
                            pingResult = pingServer(server)
                            val pingTime = extractPingTime(pingResult)
                            if (pingTime != null) {
                                pingList.add(pingData(pingTime.toDouble(), System.currentTimeMillis()))
                                pingDataText = pingTime
                            }

                            // 1秒待機
                            Thread.sleep(1000)
                        }
                        Log.d("pingList", pingList.toString())
                        pingList.clear()
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ){
                        Switch(
                            modifier = Modifier.padding(top = 32.dp),
                            checked = isStartFlag,
                            onCheckedChange = { isStartFlag = it }
                        )

//                        Text(text = pingResult, modifier = Modifier.padding(innerPadding))

                        Text(
                            text = "$pingDataText ms",
                            fontSize = 24.sp
                        )

                        // デバッグ用に結果を画面に表示
                        GraphScreen(pingList = pingList)


                    }
                }
            }
        }
    }

    // pingで疎通確認をする関数
    suspend fun pingServer(server: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // ping コマンドを実行
                val process = Runtime.getRuntime().exec("ping -c 1 $server")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val result = StringBuilder()
                var line: String?

                // 結果を読み込む
                while (reader.readLine().also { line = it } != null) {
                    result.append(line).append("\n")
                }

                // プロセスが終了するまで待機
                process.waitFor()

                result.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                "Ping failed: ${e.message}"
            }
        }
    }

    fun extractPingTime(output: String): String? {
        // 正規表現パターンを定義
        val pattern = Regex("time=(\\d+\\.?\\d*) ms")

        // パターンにマッチする部分を検索
        val matchResult = pattern.find(output)

        // マッチした部分があればその文字列を返す
        return matchResult?.groupValues?.get(1)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GraphScreen(
    pingList: List<pingData>
) {
    // pingListのpingだけを取り出す
    var pingListPing = pingList.map { it.ping }
    pingListPing.reversed()

    // 末尾7件を取り出す
    pingListPing = pingListPing.takeLast(7)


    val chartEntryModel = entryModelOf(*pingListPing.toTypedArray())

    val pingListDate = pingList.map { it.time }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Chart(
            chart = lineChart(),
            model = chartEntryModel,
            startAxis = rememberStartAxis(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GetPingGraphTheme {
//        GraphScreen()
    }
}
