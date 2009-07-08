package net.lowreal.skirts

import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.FetchOptions.Builder._
import scala.collection.jcl.Conversions._

class DS [T <: DS[T]] () {
	def entityName = this.getClass.getName

	var entity:Entity = new Entity(entityName)
	val datastore = DatastoreServiceFactory.getDatastoreService

	// def _entity_= (e:Entity) = entity = Entity

	// class method
	def create ():T = {
		this.getClass.newInstance.asInstanceOf[T]
	}

	def create (args: (Symbol, Any)*):T = {
		val ret = create()
		for ( (key, value) <- args) {
			ret(key) = value
		}
		ret
	}

	def select (args: (Symbol, Any)*):Iterator[T] = {
		val query = new Query(entityName)
		val fopts = withChunkSize(FetchOptions.DEFAULT_CHUNK_SIZE)
		for ( (key, value) <- args) {
			(key, value) match {
				case ('order, key:Symbol) => {
					query.addSort(key.name)
				}
				case ('order, (key:Symbol, sort:Symbol)) => {
					query.addSort(key.name, if (sort == 'desc) Query.SortDirection.DESCENDING else Query.SortDirection.ASCENDING )
				}
				case ('limit, n:Int) => {
					fopts.limit(n)
				}
				case ('offset, n:Int) => {
					fopts.offset(n)
				}
				case ('chunkSize, n:Int) => {
					fopts.chunkSize(n)
				}
				case _ => {
					query.addFilter(key.name, Query.FilterOperator.EQUAL, value)
				}
			}
		}
		val i = datastore.prepare(query).asIterator
		val self = this
		new Iterator[T] {
			def hasNext: Boolean = i.hasNext
			def next   : T       = self.getClass.newInstance.asInstanceOf[T].setEntity(i.next)
		}
	}

	def find (args: (Symbol, Any)*):Option[T] = {
		val ret = select(args:_*)
		if (ret.hasNext) Some(ret.next) else None
	}

	def find (id:Int):Option[T] = {
		val key = KeyFactory.createKey(entityName, id)
		try {
			Some(this.getClass.newInstance.asInstanceOf[T].setEntity(datastore.get(key)))
		} catch {
			case _ => None
		}
	}

	// find or create with first value
	def ensure (args: (Symbol, Any)*) = find(args:_*) match {
		case None      => create(args:_*)
		case Some(ret) => ret
	}

	// instance method
	def setEntity (e:Entity):T = {
		entity = e
		this.asInstanceOf[T]
	}

	def key = entity.getKey

	protected def update (key:Symbol, value:Any):Unit = {
		entity.setProperty(key.name, value)
	}

	protected def apply (key:Symbol, ifnone: => Any):Any = {
		val ret = entity.getProperty(key.name)
		if (ret == null) return ifnone
		ret
	}

	protected def param (key:String, ifnone: => Any):Any = {
		apply(Symbol(key), ifnone)
	}

	def save ():T = {
		datastore.put(entity)
		this.asInstanceOf[T]
	}

	def delete ():T = {
		datastore.delete(entity.getKey)
		this.asInstanceOf[T]
	}

}
