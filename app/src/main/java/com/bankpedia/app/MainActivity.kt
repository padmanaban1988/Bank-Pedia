package com.bankpedia.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class Entry(
    val abbr: String,
    val fullForm: String,
    val category: String,
    val meaning: String
)

fun loadEntries(context: Context): List<Entry> {
    val text = context.assets.open("bankpedia_abbreviations.json")
        .bufferedReader().use { it.readText() }
    val arr = JSONObject(text).getJSONArray("entries")
    val list = ArrayList<Entry>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
            Entry(
                o.getString("abbr"),
                o.getString("full_form"),
                o.getString("category"),
                o.getString("meaning")
            )
        )
    }
    return list
}

private fun score(e: Entry, t: String): Int {
    val a = e.abbr.lowercase()
    val f = e.fullForm.lowercase()
    val m = e.meaning.lowercase()
    val ac = a.filter { it.isLetterOrDigit() }
    val tc = t.filter { it.isLetterOrDigit() }
    return when {
        a == t -> 1
        tc.isNotEmpty() && ac == tc -> 2
        a.startsWith(t) -> 3
        f.split(Regex("\\W+")).any { it.startsWith(t) } -> 4
        a.contains(t) -> 5
        f.contains(t) -> 6
        m.contains(t) -> 7
        else -> 0
    }
}

fun search(all: List<Entry>, query: String, category: String): List<Entry> {
    val t = query.trim().lowercase()
    val pool = if (category == "All") all else all.filter { it.category == category }
    if (t.isEmpty()) return pool
    return pool
        .map { it to score(it, t) }
        .filter { it.second > 0 }
        .sortedWith(compareBy({ it.second }, { it.first.abbr.length }))
        .map { it.first }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entries = loadEntries(this)
        setContent {
            val dark = isSystemInDarkTheme()
            val scheme = if (dark) {
                darkColorScheme(
                    primary = Color(0xFF5FD0B0),
                    background = Color(0xFF0F1715),
                    surface = Color(0xFF17211E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF0F5C4A),
                    background = Color(0xFFF6F7F4),
                    surface = Color.White
                )
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BankPediaApp(entries)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPediaApp(all: List<Entry>) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = remember(all) { listOf("All") + all.map { it.category }.distinct().sorted() }
    val results = remember(query, category, all) { search(all, query, category) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "BankPedia",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "A ready to refer glossary for bankers",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Type cts, npa, truncation") }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(categories) { c ->
                FilterChip(
                    selected = c == category,
                    onClick = { category = c },
                    label = { Text(c) }
                )
            }
        }
        Text(
            "${results.size} results",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(results.take(100)) { e -> EntryCard(e) }
        }
    }
}

@Composable
fun EntryCard(e: Entry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    e.abbr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    e.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                e.fullForm,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                e.meaning,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
