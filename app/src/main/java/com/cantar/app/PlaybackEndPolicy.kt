package com.cantar.app

/**
 * Prevents a completed player from finalizing a setup screen or a round
 * that has already been finalized by a manual action.
 */
internal fun shouldAutoFinalizePlayback(
    isSinging: Boolean,
    playbackEnded: Boolean,
    roundFinalizing: Boolean
): Boolean = isSinging && playbackEnded && !roundFinalizing
