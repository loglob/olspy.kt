package olspy.support

/** Alias to avoid confusion with Pat.List */
private typealias L<T> = List<T>

/** Union between Pat and Many */
sealed class MultiPat<T>

/** Support class wrapper for a pattern matching operation */
sealed class Pat<T> : MultiPat<T>()
{
	/** Whether this pattern matches a value
	 *  operator to be used with `when(...){ in ... }`
	 */
	abstract operator fun contains(x : T) : Boolean

	/** Wraps a regular expression as a pattern */
	class Regex(val exp : kotlin.text.Regex) : Pat<String>()
	{
		constructor(exp : String) : this(kotlin.text.Regex(exp))

		override fun contains(x: String) : Boolean
				= exp.matches(x)
	}

	/** Wildcard that always matches */
	class Any<T>() : Pat<T>()
	{
		override fun contains(x: T) : Boolean
				= true
	}

	/** Compares via `==` */
	class Eq<T>(val lit : T) : Pat<T>()
	{
		override fun contains(x: T) : Boolean
				= x == lit
	}

	/** Matches any number of values with `pat` */
	class Many<T>(val pat : Pat<T>) : MultiPat<T>()
	{
		constructor() : this(Any())
	}

	/** A pattern matches against a list
	 * @param pieces Patterns for individual elements, with AT MOST one Many() pattern
	 * */
	class List<T>(vararg pieces : MultiPat<T>) : Pat<L<T>>()
	{
		private val left : L<Pat<T>>
		private val right : L<Pat<T>>
		private val fill : Many<T>?

		init {
			val l = mutableListOf<Pat<T>>()
			val r = mutableListOf<Pat<T>>()
			var fill : Many<T>? = null

			for(p in pieces) {
				if(fill === null)
				{
					when(p)
					{
						is Many -> fill = p
						is Pat -> l.add(p)
					}
				}
				else
				{
					require(p is Pat) { "At most one Many() pattern may be present" }
					r.add(p)
				}
			}

			this.left = l
			this.right = r
			this.fill = fill
		}

		override fun contains(x: L<T>) : Boolean
		{
			if(x.size < left.size + right.size)
				return false

			// xs index corresponding to right[0]
			val rIx = x.size - right.size

			if(x.size > left.size + right.size)
			{
				if(fill === null || (left.size until rIx).any { x[it] !in fill.pat })
					return false
			}

			return left.withIndex().all { (i,l) -> x[i] in l }
				&& right.withIndex().all { (i,r) -> x[rIx + i] in r }
		}
	}
}

