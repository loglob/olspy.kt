package olspy.support

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** A MRSW register that may be written to at most once */
@OptIn(ExperimentalAtomicApi::class)
class WriteOnce<T>
{
	/** Used for awaiting set() */
	private val sem = Semaphore(1,1)
	private val value : AtomicReference<T?> = AtomicReference(null)

	/** Writes to the register
	 * @param x The value to write
	 * @throws IllegalStateException If it was already written
	 */
	fun set(x : T)
	{
		if(! value.compareAndSet(null, x))
			throw IllegalStateException("WriteOnce already set")

		sem.release()
	}

	/** Reads from the register.
	 * If no value is present yet, wait for set() to be called
	 */
	suspend fun get() : T
		= value.load() ?: sem.withPermit { value.load()!! }

	/** Reads the current contents of the register without waiting */
	fun getOrNull() : T?
		= value.load()
}