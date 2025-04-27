package olspy.support

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** A concurrency primitive register that may be written at most once */
@OptIn(ExperimentalAtomicApi::class)
class WriteOnce<T>
{
	private val sem = Semaphore(1,1)
	private val value : AtomicReference<T?> = AtomicReference(null)

	fun set(x : T)
	{
		if(! value.compareAndSet(null, x))
			throw IllegalStateException("WriteOnce already set")

		sem.release()
	}

	suspend fun get() : T
		= value.load() ?: sem.withPermit { value.load()!! }

	fun getOrNull() : T?
		= value.load()
}