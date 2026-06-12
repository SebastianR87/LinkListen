package com.UTP.linklisten.data

import com.UTP.linklisten.model.ArticleContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object ArticleExtractor {
    suspend fun extract(rawUrl: String): Result<ArticleContent> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = normalizeUrl(rawUrl)
            val document = Jsoup
                .connect(normalizedUrl)
                .userAgent(DEFAULT_USER_AGENT)
                .referrer("https://www.google.com")
                .timeout(15_000)
                .get()

            cleanDocument(document)

            val container = bestContentContainer(document)
            val bodyParagraphs = extractCandidateParagraphs(document.body())
            val contentParagraphs = extractCandidateParagraphs(container)

            if (isLikelyListingPage(normalizedUrl, document, container, bodyParagraphs, contentParagraphs)) {
                throw IllegalArgumentException(
                    "Ese enlace parece una portada o un listado. Pega el enlace de una noticia especifica."
                )
            }

            val title = extractTitle(document, container)
            val sourceName = extractSourceName(document, normalizedUrl)
            val publishedAt = extractPublishedAt(document)
            val summary = extractSummary(document, container, contentParagraphs)
            val body = extractBody(contentParagraphs)

            if (body.length < 180) {
                throw IllegalStateException("No se pudo identificar suficiente contenido principal.")
            }

            ArticleContent(
                sourceUrl = normalizedUrl,
                sourceName = sourceName,
                title = title,
                summary = if (summary.isBlank()) buildSummaryFromBody(body) else summary,
                body = body,
                publishedAt = publishedAt
            )
        }
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Ingresa un enlace para continuar.")
        }

        val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        try {
            val uri = URI(url)
            if (uri.host.isNullOrBlank()) {
                throw IllegalArgumentException("El enlace no parece valido.")
            }
        } catch (_: URISyntaxException) {
            throw IllegalArgumentException("El enlace no parece valido.")
        }

        return url
    }

    private fun cleanDocument(document: Document) {
        document.select(
            """
            script, style, noscript, iframe, svg, canvas, form, nav, footer, header, aside,
            .advertisement, .ads, .ad, .banner, .cookie, .newsletter, .subscribe, .social-share,
            .related, .recommended, .comments, [role=dialog], [aria-hidden=true]
            """.trimIndent()
        ).remove()
    }

    private fun extractTitle(document: Document, container: Element): String {
        val candidates = listOf(
            document.selectFirst("meta[property=og:title]")?.attr("content"),
            document.selectFirst("meta[name=twitter:title]")?.attr("content"),
            container.selectFirst("h1")?.text(),
            document.selectFirst("article h1")?.text(),
            document.selectFirst("main h1")?.text(),
            document.selectFirst("h1")?.text(),
            document.title()
        )

        return candidates
            .map { cleanText(it.orEmpty()) }
            .firstOrNull { it.isNotBlank() }
            ?: "Articulo sin titulo"
    }

    private fun extractSourceName(document: Document, url: String): String {
        val metaSiteName = cleanText(
            document.selectFirst("meta[property=og:site_name]")?.attr("content").orEmpty()
        )
        if (metaSiteName.isNotBlank()) {
            return metaSiteName
        }

        return try {
            val host = URI(url).host.orEmpty().removePrefix("www.")
            host.split(".")
                .firstOrNull()
                ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                .orEmpty()
                .ifBlank { "Fuente desconocida" }
        } catch (_: Exception) {
            "Fuente desconocida"
        }
    }

    private fun extractPublishedAt(document: Document): String? {
        val dateCandidates = listOf(
            document.selectFirst("meta[property=article:published_time]")?.attr("content"),
            document.selectFirst("meta[name=pubdate]")?.attr("content"),
            document.selectFirst("time[datetime]")?.attr("datetime"),
            document.selectFirst("[itemprop=datePublished]")?.attr("content"),
            document.selectFirst("[itemprop=datePublished]")?.text()
        )

        return dateCandidates
            .map { cleanText(it.orEmpty()) }
            .firstOrNull { it.isNotBlank() }
    }

    private fun extractSummary(
        document: Document,
        container: Element,
        contentParagraphs: List<String>
    ): String {
        val metaDescription = cleanText(
            document.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        )
        if (metaDescription.isNotBlank()) {
            return metaDescription
        }

        val leadingParagraph = contentParagraphs.ifEmpty {
            extractCandidateParagraphs(container)
        }
            .firstOrNull()
            .orEmpty()

        return buildSummaryFromBody(leadingParagraph)
    }

    private fun extractBody(paragraphs: List<String>): String {
        if (paragraphs.isEmpty()) {
            throw IllegalStateException("No se encontro texto principal en la pagina.")
        }

        return paragraphs.joinToString("\n\n")
    }

    private fun bestContentContainer(document: Document): Element {
        val directCandidates = listOfNotNull(
            document.selectFirst("article"),
            document.selectFirst("main"),
            document.selectFirst("[role=main]"),
            document.selectFirst(".post-content"),
            document.selectFirst(".entry-content"),
            document.selectFirst(".article-content"),
            document.selectFirst(".story-body"),
            document.selectFirst(".article-body"),
            document.selectFirst("#content")
        )

        val weightedCandidate = directCandidates.maxByOrNull(::scoreContainer)
        if (weightedCandidate != null && scoreContainer(weightedCandidate) > 120) {
            return weightedCandidate
        }

        return document.body()
            .select("div, section, article, main")
            .maxByOrNull(::scoreContainer)
            ?: document.body()
    }

    private fun scoreContainer(element: Element): Int {
        val paragraphs = extractCandidateParagraphs(element)
        val paragraphScore = paragraphs.sumOf { it.length }
        val paragraphCountBonus = paragraphs.size * 40
        val articleClassBonus = if (hasArticleClassName(element)) 220 else 0
        val h1Bonus = if (element.selectFirst("h1") != null) 140 else 0
        val articleTagBonus = if (element.tagName() == "article") 160 else 0
        val linkPenalty = (linkDensity(element) * 600).toInt()
        val headingPenalty = element.select("h2, h3, h4").size * 12

        return paragraphScore +
            paragraphCountBonus +
            articleClassBonus +
            h1Bonus +
            articleTagBonus -
            linkPenalty -
            headingPenalty
    }

    private fun extractCandidateParagraphs(element: Element): List<String> {
        val selectors = element.select("p, blockquote")
        val paragraphs = selectors
            .map { cleanText(it.text()) }
            .filter { paragraph ->
                paragraph.length >= 55 &&
                    looksLikeParagraph(paragraph) &&
                    !shouldDropParagraph(paragraph) &&
                    !looksLikeCaption(paragraph)
            }
            .distinct()

        return trimTrailingBoilerplate(paragraphs)
    }

    private fun isLikelyListingPage(
        url: String,
        document: Document,
        container: Element,
        bodyParagraphs: List<String>,
        contentParagraphs: List<String>
    ): Boolean {
        val rootLikeUrl = isHomepageOrSectionUrl(url)
        val articleSignals = articleSignalCount(document, container)
        val bodyLength = bodyParagraphs.sumOf { it.length }.coerceAtLeast(1)
        val contentLength = contentParagraphs.sumOf { it.length }
        val dominanceRatio = contentLength.toDouble() / bodyLength.toDouble()
        val highLinkDensity = linkDensity(container) > 0.45
        val tooManyParagraphs = bodyParagraphs.size >= 12

        if (rootLikeUrl && articleSignals < 2) {
            return true
        }

        if (contentParagraphs.size < 4 && articleSignals < 3) {
            return true
        }

        if (tooManyParagraphs && dominanceRatio < 0.55 && articleSignals < 3) {
            return true
        }

        if (highLinkDensity && articleSignals < 3) {
            return true
        }

        return false
    }

    private fun articleSignalCount(document: Document, container: Element): Int {
        var signals = 0
        val ogType = cleanText(
            document.selectFirst("meta[property=og:type]")?.attr("content").orEmpty()
        )

        if (ogType.contains("article", ignoreCase = true)) signals++
        if (document.selectFirst("meta[property=article:published_time]") != null) signals++
        if (document.selectFirst("time[datetime]") != null) signals++
        if (document.selectFirst("[itemprop=datePublished]") != null) signals++
        if (document.selectFirst("article") != null) signals++
        if (container.selectFirst("h1") != null) signals++
        if (hasArticleClassName(container)) signals++

        return signals
    }

    private fun isHomepageOrSectionUrl(url: String): Boolean {
        val path = try {
            URI(url).path.orEmpty().trim('/')
        } catch (_: Exception) {
            ""
        }

        if (path.isBlank()) {
            return true
        }

        val parts = path.split("/").filter { it.isNotBlank() }
        if (parts.size == 1 && !parts.first().contains("-")) {
            return true
        }

        return false
    }

    private fun hasArticleClassName(element: Element): Boolean {
        val descriptor = "${element.id()} ${element.className()}".lowercase(Locale.getDefault())
        return listOf(
            "article",
            "story",
            "post",
            "entry",
            "content-body",
            "article-body",
            "story-body",
            "article-content"
        ).any { descriptor.contains(it) }
    }

    private fun linkDensity(element: Element): Double {
        val textLength = cleanText(element.text()).length.coerceAtLeast(1)
        val linkTextLength = cleanText(element.select("a").text()).length
        return linkTextLength.toDouble() / textLength.toDouble()
    }

    private fun looksLikeParagraph(text: String): Boolean {
        val hasSentencePunctuation = text.contains('.') || text.contains(':') || text.contains(';')
        val wordCount = text.split(" ").count { it.isNotBlank() }
        return hasSentencePunctuation || wordCount >= 12
    }

    private fun looksLikeCaption(text: String): Boolean {
        val lowered = text.lowercase(Locale.getDefault())
        return lowered.startsWith("foto:") ||
            lowered.startsWith("imagen:") ||
            lowered.startsWith("video:") ||
            lowered.startsWith("credito:")
    }

    private fun shouldDropParagraph(text: String): Boolean {
        if (isBoilerplate(text)) {
            return true
        }

        val lowered = text.lowercase(Locale.getDefault())
        return promoTerms.any { lowered.contains(it) }
    }

    private fun isBoilerplate(text: String): Boolean {
        val lowered = text.lowercase(Locale.getDefault())
        return boilerplateTerms.any { lowered.contains(it) }
    }

    private fun trimTrailingBoilerplate(paragraphs: List<String>): List<String> {
        if (paragraphs.isEmpty()) return paragraphs

        var lastUsefulIndex = paragraphs.lastIndex
        while (lastUsefulIndex >= 0 && isClosingParagraph(paragraphs[lastUsefulIndex])) {
            lastUsefulIndex--
        }

        return if (lastUsefulIndex < 0) emptyList() else paragraphs.subList(0, lastUsefulIndex + 1)
    }

    private fun isClosingParagraph(text: String): Boolean {
        val lowered = text.lowercase(Locale.getDefault())
        return closingTerms.any { lowered.contains(it) }
    }

    private fun buildSummaryFromBody(body: String): String {
        val sentences = body
            .split(Regex("(?<=[.!?])\\s+"))
            .map(::cleanText)
            .filter { it.length >= 30 }
            .take(2)

        return sentences.joinToString(" ").take(220).trim()
    }

    private fun cleanText(text: String): String =
        text.replace(Regex("\\s+"), " ")
            .replace('\u00A0', ' ')
            .trim()

    private val boilerplateTerms = listOf(
        "suscribete",
        "suscríbete",
        "leer tambien",
        "leer también",
        "tambien te puede interesar",
        "también te puede interesar",
        "publicidad",
        "anuncio",
        "copyright",
        "todos los derechos reservados",
        "comparte esta noticia",
        "también puedes seguirnos",
        "tambien puedes seguirnos",
        "y recuerda que puedes",
        "descarga la ultima version",
        "descarga la última versión",
        "nuestro nuevo canal de whatsapp",
        "canal de whatsapp",
        "recibir notificaciones en nuestra app",
        "puedes recibir notificaciones en nuestra app",
        "haz clic aqui",
        "haz clic aquí"
    )

    private val promoTerms = listOf(
        "podcast",
        "youtube",
        "instagram",
        "tiktok",
        "facebook",
        "x, facebook",
        "canal de whatsapp",
        "descarga la última versión",
        "descarga la ultima version",
        "nuestra app",
        "síguenos en",
        "siguenos en",
        "newsletter"
    )

    private val closingTerms = listOf(
        "también puedes seguirnos",
        "tambien puedes seguirnos",
        "y recuerda que puedes",
        "descarga la última versión",
        "descarga la ultima version",
        "nuestro nuevo canal de whatsapp",
        "puedes recibir notificaciones en nuestra app",
        "recibir notificaciones en nuestra app"
    )

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; LinkListen) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
}
