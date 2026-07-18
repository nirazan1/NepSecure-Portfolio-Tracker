package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class MarketCandle(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class MarketNewsItem(
    val id: Int,
    val title: String,
    val symbol: String,
    val sector: String,
    val message: String,
    val date: String,
    val clickUrl: String,
    val category: String,
    val pinned: Boolean
)

object MarketDataClient {
    private val client = OkHttpClient()

    private val baseUrl: String
        get() = BuildConfig.MARKET_DATA_BASE_URL.let { url ->
            if (url.endsWith("/")) url else "$url/"
        }

    fun fetchCandles(symbol: String): List<MarketCandle> {
        val cleanSymbol = symbol.uppercase().trim()
        val url = "${baseUrl}api/data/adjhistorydata/?symbol=$cleanSymbol"
        Log.d("MarketDataClient", "Fetching candle data from: $url")
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("MarketDataClient", "API error response: ${response.code}")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val jsonArray = JSONArray(body)
                val list = mutableListOf<MarketCandle>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        MarketCandle(
                            date = obj.optString("date", ""),
                            open = obj.optDouble("open", 0.0),
                            high = obj.optDouble("high", 0.0),
                            low = obj.optDouble("low", 0.0),
                            close = obj.optDouble("close", 0.0),
                            volume = obj.optDouble("volume", 0.0)
                        )
                    )
                }
                // Reverse to get chronological order (oldest to newest)
                return list.reversed()
            }
        } catch (e: Exception) {
            Log.e("MarketDataClient", "Error fetching candles for $symbol", e)
            return emptyList()
        }
    }

    fun fetchNews(page: Int = 1, size: Int = 20): List<MarketNewsItem> {
        val url = "${baseUrl}api/report/v2/?page=$page&size=$size"
        Log.d("MarketDataClient", "Fetching news from: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("MarketDataClient", "News API error response: ${response.code}")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val jsonObject = JSONObject(body)
                val resultsArray = jsonObject.optJSONArray("results") ?: return emptyList()
                
                val list = mutableListOf<MarketNewsItem>()
                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    list.add(
                        MarketNewsItem(
                            id = obj.optInt("id", 0),
                            title = obj.optString("title", ""),
                            symbol = obj.optString("symbol", ""),
                            sector = obj.optString("sector", ""),
                            message = obj.optString("message", ""),
                            date = obj.optString("date", ""),
                            clickUrl = obj.optString("click_url", ""),
                            category = obj.optString("category", ""),
                            pinned = obj.optBoolean("pinned", false)
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e("MarketDataClient", "Error fetching news", e)
            return emptyList()
        }
    }
}
