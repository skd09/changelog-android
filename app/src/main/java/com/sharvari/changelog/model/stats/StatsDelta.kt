package com.sharvari.changelog.model.stats

data class StatsDelta(
    var reads:          Int = 0,
    var skips:          Int = 0,
    var sessions:       Int = 0,
    var readSeconds:    Int = 0,
    var sessionSeconds: Int = 0,
) {
    val isEmpty: Boolean
        get() = reads == 0 && skips == 0 && sessions == 0 &&
                readSeconds == 0 && sessionSeconds == 0
}
