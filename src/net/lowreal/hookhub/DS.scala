package net.lowreal.skirts

import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.FetchOptions.Builder._
import com.google.appengine.api.memcache._
import scala.collection.jcl.Conversions._

object Cache {
	val memcache  = MemcacheServiceFactory.getMemcacheService

	def get[T](key:Object):Option[T] = {
		val ret = memcache.get(key).asInstanceOf[T]
		if (ret == null) return None
		Some(ret)
	}

	def put (key:Object, value:Object) = {
		memcache.put(key, value)
	}

	def delete (key:Object):Boolean = {
		memcache.delete(key)
	}

	def key[T](key:Object)(els: => T):T = {
		get[T](key).getOrElse {
			val ret = els
			memcache.put(key, els)
			els
		}
	}
}

class DS [T <: DS[T]] () extends java.io.Serializable {
	def entityName = this.getClass.getName

	var entity:Entity = new Entity(entityName)
	def datastore = DatastoreServiceFactory.getDatastoreService

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

	def find (id:Long):Option[T] = {
		val key = KeyFactory.createKey(entityName, id)
		try {
			Some(this.getClass.newInstance.asInstanceOf[T].setEntity(datastore.get(key)))
		} catch {
			case _ => None
		}
	}

	// find or create with first value
	def ensureIf (args: (Symbol, Any)*)(ifcreate: T => Unit):T = find(args:_*) match {
		case None      => {
			val ret = create(args:_*)
			ifcreate(ret)
			ret.save
			ret
		}
		case Some(ret) => ret
	}

	def ensure (args: (Symbol, Any)*):T = ensureIf(args:_*) { _ => null}

	// instance method
	def setEntity (e:Entity):T = {
		entity = e
		this.asInstanceOf[T]
	}

	def key = entity.getKey
	def id  = key.getId

	protected def update (key:Symbol, value:Any):Unit = {
		entity.setProperty(key.name, value)
	}

	protected def apply[U](key:Symbol, ifnone: => U):U = {
		val ret = entity.getProperty(key.name).asInstanceOf[U]
		if (ret == null) return ifnone
		ret
	}

	protected def apply[U](key:Symbol):Option[U] = {
		val ret = entity.getProperty(key.name).asInstanceOf[U]
		if (ret == null) return None
		Some(ret)
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
