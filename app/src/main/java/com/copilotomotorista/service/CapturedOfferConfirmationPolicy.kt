package com.copilotomotorista.service

/**
 * Guards manual actions coming from a possibly stale floating offer card.
 *
 * A deleted record must not be recreated by tapping an old overlay. Completed
 * trips must never be downgraded or completed a second time, and an in-progress
 * trip must not go backwards to "accepted". No action here accepts an offer
 * inside the driver app: these are local bookkeeping transitions only.
 */
internal object CapturedOfferConfirmationPolicy {
    fun canApply(existingStatus: String?, requestedStatus: String): Boolean =
        when (requestedStatus) {
            "accepted" -> existingStatus == "captured" || existingStatus == "accepted"
            "completed" -> existingStatus == "accepted" || existingStatus == "in_progress"
            else -> false
        }
}
