package com.example.kusinakode.domain.model

/**
 * Cultural content for one dish, mirroring the shared Level contract fields
 * (history, region, ingredients[], procedure, tools[]). Served locally as
 * preview data until Module 4's expert-validated dataset (Cosmos DB) lands.
 */
data class DishContent(
    val levelId: Int,
    val history: String,
    val ingredients: List<String>,
    val procedure: List<CookingStep>,
    val tools: List<KitchenTool>,
    /** False while the content is unvalidated preview material. */
    val isValidated: Boolean
)

data class CookingStep(val title: String, val description: String)

data class KitchenTool(val name: String, val description: String)
