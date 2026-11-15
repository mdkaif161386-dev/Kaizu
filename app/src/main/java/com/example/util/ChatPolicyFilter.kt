package com.example.util

data class PolicyCheckResult(
    val isViolation: Boolean,
    val reason: String = ""
)

object ChatPolicyFilter {

    private val academicBiologyWhitelist = listOf(
        "sexual reproduction",
        "reproductive system",
        "reproductive health",
        "gametogenesis",
        "spermatogenesis",
        "oogenesis",
        "menstrual cycle",
        "fertilization",
        "contraception",
        "flowering plants",
        "asexual reproduction",
        "ncert",
        "biology"
    )

    private val adultDomainPatterns = listOf(
        "pornhub",
        "xvideos",
        "xnxx",
        "xhamster",
        "onlyfans",
        "redtube",
        "youporn",
        "chaturbate",
        "brazzers",
        "spankbang",
        "hentai",
        "rule34",
        "erotic",
        "adultvideo",
        "sexvideo"
    )

    private val explicitRegexes = listOf(
        Regex("\\b(porn|porno|pornography|xxx|nsfw|hentai|nude|nudes|naked|boobs|tits|pussy|dick|cock|penis|vagina|blowjob|handjob|masturbat\\w*|horny|sext|sexting|callgirl|escort)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(adult\\s+video|adults\\s+video|sex\\s+video|sexy\\s+video|hot\\s+video|blue\\s+film|bf\\s+video|mms\\s+video|18\\+\\s+video|send\\s+nudes)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(chut|lund|chudai|gaand|randi|bhabhi\\s+sex|desi\\s+sex)\\b", RegexOption.IGNORE_CASE)
    )

    fun evaluateLocal(text: String, attachmentLabel: String = ""): PolicyCheckResult {
        val combined = "$text $attachmentLabel".trim()
        if (combined.isBlank()) return PolicyCheckResult(isViolation = false)

        val lower = combined.lowercase()

        // Block known adult domains & attachment labels unconditionally
        if (adultDomainPatterns.any { lower.contains(it) }) {
            return PolicyCheckResult(
                isViolation = true,
                reason = "Adult/explicit website links or NSFW media are strictly prohibited in Kaizu."
            )
        }

        // Check explicit sexual / adult video terms
        val matchedExplicit = explicitRegexes.any { it.containsMatchIn(combined) }
        if (matchedExplicit) {
            return PolicyCheckResult(
                isViolation = true,
                reason = "Sexual content or adult video references violate Kaizu's Study Chat Policy."
            )
        }

        // If message mentions "sex" or "18+", only allow if clearly academic NCERT/Biology context
        if (Regex("\\b(sex|18\\+)\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            val isAcademic = academicBiologyWhitelist.any { lower.contains(it) }
            if (!isAcademic) {
                return PolicyCheckResult(
                    isViolation = true,
                    reason = "Non-academic sexual content is prohibited in Kaizu Study Chat."
                )
            }
        }

        return PolicyCheckResult(isViolation = false)
    }
}
