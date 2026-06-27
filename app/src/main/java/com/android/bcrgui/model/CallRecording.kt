package com.android.bcrgui.model

import android.net.Uri

data class CallRecording(
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val lastModified: Long,
    val date: String?,
    val direction: String?,
    val simSlot: Int?,
    val phoneNumber: String?,
    val contactName: String?,
    val callerName: String?,
    val callLogName: String?,
    val durationMs: Long,
    val hasMetadataJson: Boolean,
    val packageName: String? = null
) {
    /**
     * Resolves the primary name to display. 
     * Priority: Contact Name -> Call Log Name -> Caller Name -> Phone Number -> "Private Number/Unknown"
     */
    val resolvedName: String
        get() = when {
            !contactName.isNullOrBlank() -> contactName
            !callLogName.isNullOrBlank() -> callLogName
            !callerName.isNullOrBlank() -> callerName
            !phoneNumber.isNullOrBlank() -> phoneNumber
            else -> "Unknown/Private"
        }

    /**
     * Resolves subtext to display below the primary name (e.g. phone number if name was shown).
     */
    val resolvedSubtext: String?
        get() = when {
            !contactName.isNullOrBlank() || !callLogName.isNullOrBlank() || !callerName.isNullOrBlank() -> phoneNumber
            else -> null
        }
}

data class RecycledFile(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val contactName: String? = null,
    val phoneNumber: String? = null
) {
    val resolvedName: String
        get() = when {
            !contactName.isNullOrBlank() -> contactName
            !phoneNumber.isNullOrBlank() -> phoneNumber
            else -> name
        }

    val resolvedSubtext: String?
        get() = if (!contactName.isNullOrBlank()) {
            if (!phoneNumber.isNullOrBlank()) "$phoneNumber • $name" else name
        } else {
            null
        }
}
