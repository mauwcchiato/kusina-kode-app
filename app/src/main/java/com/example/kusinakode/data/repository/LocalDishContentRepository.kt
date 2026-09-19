package com.example.kusinakode.data.repository

import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.model.CookingStep
import com.example.kusinakode.domain.model.DishContent
import com.example.kusinakode.domain.model.KitchenTool
import com.example.kusinakode.domain.repository.DishContentRepository

/**
 * Serves the bundled dish dataset.
 *
 * Every dish now carries a real origin story, ingredient list, procedure and
 * equipment list from the research spreadsheet — the hand-written samples and
 * "curation pending" shells this used to return are gone.
 *
 * isValidated is true: Chef Gicana has signed the content off, so the UI no
 * longer shows the PREVIEW notice. Flip this back to false for any dish whose
 * content is ever revised and awaiting re-endorsement.
 */
class LocalDishContentRepository : DishContentRepository {

    override suspend fun dishContent(levelId: Int): Result<DishContent> {
        val dish = LevelProvider.forLevel(levelId).dish
        return Result.success(
            DishContent(
                levelId = levelId,
                history = dish.story,
                ingredients = dish.ingredients,
                procedure = dish.steps.map { it.toCookingStep() },
                tools = dish.tools.map { KitchenTool(it, "") },
                isValidated = true
            )
        )
    }
}

/**
 * Splits a numbered instruction into a short title and its detail.
 *
 * The sheet writes steps as "3. Simmer for 40 minutes until tender." — the
 * leading number is presentation the UI already provides, and a step reads
 * better with its first clause as a heading.
 */
private fun String.toCookingStep(): CookingStep {
    // Strip the sheet's own numbering ("3. Simmer...") — the UI numbers steps
    // itself, so keeping it would print the number twice.
    val body = trim()
        .removePrefix("-")
        .trimStart()
        .dropWhile { it.isDigit() }
        .removePrefix(".")
        .removePrefix(")")
        .trim()

    val cut = body.indexOfFirst { it == '.' || it == ',' || it == ';' }
    if (cut < 8 || cut > 48) {
        return CookingStep(body.take(40).trimEnd(), body)
    }
    return CookingStep(body.substring(0, cut).trim(), body)
}
