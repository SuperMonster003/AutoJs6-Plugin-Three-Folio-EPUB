package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes

/**
 * Bundle answers of the EPUB contract (roadmap P5.2): every answer carries the contract version,
 * a failed one carries the error code and a bounded detail instead of result keys.
 */
internal object Answers {

    fun ok(build: Bundle.() -> Unit): Bundle = Bundle().apply {
        putInt(EpubContract.KEY_CONTRACT_VERSION, EpubContract.CONTRACT_VERSION)
        build()
    }

    fun error(code: String, detail: String?): Bundle = Bundle().apply {
        putInt(EpubContract.KEY_CONTRACT_VERSION, EpubContract.CONTRACT_VERSION)
        putString(EpubContract.KEY_ERROR_CODE, if (EpubErrorCodes.isKnown(code)) code else EpubErrorCodes.INTERNAL)
        putString(EpubContract.KEY_ERROR_MESSAGE, Limits.errorDetail(detail))
    }

    fun error(violation: ContractViolation): Bundle = error(violation.code, violation.detail)

    /** The exception form of a failure, for calls that cannot answer with a Bundle. */
    fun failure(violation: ContractViolation): RuntimeException = when (violation.code) {
        EpubErrorCodes.INVALID_ARGUMENT, EpubErrorCodes.RESOURCE_NOT_FOUND, EpubErrorCodes.NOT_EPUB ->
            IllegalArgumentException(violation.message)
        else -> IllegalStateException(violation.message)
    }
}
