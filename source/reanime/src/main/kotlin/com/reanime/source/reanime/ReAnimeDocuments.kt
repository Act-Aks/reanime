package com.reanime.source.reanime

import com.fleeksoft.ksoup.nodes.Document

internal fun Document.isAccessChallenge(): Boolean = title().contains("Just a moment", ignoreCase = true) ||
    selectFirst("#challenge-form, #cf-challenge-running") != null
