package com.bjwag.mensaminus.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object HtmlScraper {
    suspend fun fetchOpeningHours(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .get()

            val header = doc.select("h4.card-header").firstOrNull {
                // identifying the section by title since there are no ids
                it.text().contains("Öffnungszeiten", ignoreCase = true)
            }

            val contentContainer = header?.nextElementSibling()?.takeIf {
                it.hasClass("card-body")
            }

            if (contentContainer != null) {
                val table = contentContainer.select("table").firstOrNull()
                if (table != null) {
                    val rows = table.select("tr").filter { !it.hasClass("d-none") }
                    val sb = StringBuilder()
                    
                    for (row in rows) {
                        val headerCell = row.select("th").firstOrNull()?.text()?.trim() ?: ""
                        val dataCells = row.select("td").map { it.text().trim() }.filter { it.isNotEmpty() }
                        
                        if (headerCell.isNotEmpty()) {
                            sb.append(headerCell)
                            sb.append(" \t\t ")
                            sb.append(dataCells.joinToString(" | "))
                            sb.append("\n")
                        } else if (dataCells.isNotEmpty()) {
                            sb.append(dataCells.joinToString(" | "))
                            sb.append("\n")
                        }
                    }
                    return@withContext sb.toString().trim()
                }

                //fallback for non-table content
                contentContainer.select("tr.d-none").remove()
                val text = contentContainer.html()
                    .replace("(?i)<br\\s*/?>".toRegex(), "\n")
                    .replace("(?i)</p>".toRegex(), "\n")
                
                return@withContext Jsoup.parse(text).wholeText()
                    .replace("&nbsp;", " ")
                    .trim()
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
