package com.sharvari.changelog.model.article

data class ArticlesQuery(
    val category: String? = null,
    val exclude:  List<String> = emptyList(),
    val limit:    Int = 20,
    val offset:   Int = 0,
)
