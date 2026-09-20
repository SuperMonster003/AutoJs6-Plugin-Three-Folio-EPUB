package io.github.supermonster003.autojs6.plugin.readium.epub.reader.service

import android.os.Bundle
import org.autojs.plugin.epub.api.EpubContract
import org.autojs.plugin.epub.api.EpubErrorCodes
import org.autojs.plugin.epub.api.IEpubReaderSession

/**
 * `IEpubReaderSession` of one [ReaderSession] (roadmap P5.3): every call passes the caller guard
 * and the bounds checks of appendix B.5 before it reaches the session; `getState` and
 * `getBookmarks` answer a closed session with `SESSION_CLOSED` in the Bundle, the other methods
 * throw it as the contract's exception form.
 */
internal class ReaderSessionBinder(
    private val session: ReaderSession,
    private val guard: CallerGuard,
) : IEpubReaderSession.Stub() {

    override fun getState(): Bundle {
        guard.check()
        if (session.closed) return Answers.error(EpubErrorCodes.SESSION_CLOSED, CLOSED_DETAIL)
        return session.stateBundle()
    }

    override fun goTo(target: Bundle?) {
        guard.check()
        answer {
            requireOpen()
            Limits.optionsBytes(DescriptorIo.parcelSize(target))
            val parsed = SessionTarget.parse(
                target?.getString(EpubContract.KEY_LOCATOR),
                target?.getString(EpubContract.KEY_HREF),
                target?.takeIf { it.containsKey(EpubContract.KEY_PROGRESSION) }?.getDouble(EpubContract.KEY_PROGRESSION),
            ) ?: throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "locator, href or progression is required")
            session.validateTarget(parsed)
            session.requestGoTo(parsed)
        }
    }

    override fun navigate(direction: Int) {
        guard.check()
        answer {
            requireOpen()
            if (direction !in EpubContract.DIRECTIONS) throw ContractViolation(EpubErrorCodes.INVALID_ARGUMENT, "unknown direction: $direction")
            if (!session.claimed) throw ContractViolation(EpubErrorCodes.READER_NOT_VISIBLE, "the reader has not claimed the session yet")
            session.requestNavigate(direction)
        }
    }

    override fun setPreferences(preferences: Bundle?) {
        guard.check()
        answer {
            requireOpen()
            Limits.optionsBytes(DescriptorIo.parcelSize(preferences))
            session.requestPreferences(ReaderPreferencesJson.parse(preferences?.getString(EpubContract.KEY_PREFERENCES)))
        }
    }

    override fun getBookmarks(): Bundle {
        guard.check()
        if (session.closed) return Answers.error(EpubErrorCodes.SESSION_CLOSED, CLOSED_DETAIL)
        return session.bookmarksBundle()
    }

    override fun close(options: Bundle?) {
        guard.check()
        session.close(EpubContract.REASON_HOST, finish = options?.getBoolean(EpubContract.KEY_FINISH, false) ?: false)
    }

    private fun requireOpen() {
        if (session.closed) throw ContractViolation(EpubErrorCodes.SESSION_CLOSED, CLOSED_DETAIL)
    }

    private inline fun answer(block: () -> Unit) {
        try {
            block()
        } catch (e: ContractViolation) {
            throw Answers.failure(e)
        }
    }

    private companion object {
        const val CLOSED_DETAIL = "the reader session is closed"
    }
}
