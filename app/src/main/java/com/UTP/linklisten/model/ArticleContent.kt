package com.UTP.linklisten.model

data class ArticleContent(
    val sourceUrl: String,
    val sourceName: String,
    val title: String,
    val summary: String,
    val body: String,
    val publishedAt: String? = null
) {
    val fullText: String
        get() = buildString {
            append(title)
            if (summary.isNotBlank() && !body.startsWith(summary)) {
                append(".\n\n")
                append(summary)
            }
            if (body.isNotBlank()) {
                append(".\n\n")
                append(body)
            }
        }

    val bodyParagraphs: List<String>
        get() = body.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    val readingSegments: List<String>
        get() = buildList {
            add(title)
            if (summary.isNotBlank() && !body.startsWith(summary)) {
                add(summary)
            }
            addAll(bodyParagraphs)
        }.filter { it.isNotBlank() }
}
