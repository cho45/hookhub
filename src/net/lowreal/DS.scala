package net.lowreal.skirts

import com.google.appengine.api.datastore._
import scala.collection.jcl.Conversions._

class DS [T <: DS[T]] () {
	var entity:Entity = null
	val datastore = DatastoreServiceFactory.getDatastoreService

	// def _entity_= (e:Entity) = entity = Entity

	// class method
	def entityName = this.getClass.getName

	def create (args: (Symbol, Any)*) = {
		val ent = new Entity(entityName)
		val ret = this.getClass.newInstance.asInstanceOf[T].setEntity(ent)

		for ( (key, value) <- args) {
			ret(key) = value
		}

		ret
	}

	def select (args: (Symbol, Any)*):Iterator[T] = {
		val query = new Query(entityName)
		for ( (key, value) <- args) {
			query.addFilter(key.name, Query.FilterOperator.EQUAL, value)
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
	def instantiate (arg: (Symbol, Any)) = find(arg) match {
		case None      => create(arg)
		case Some(ret) => ret
	}

	// instance method
	def setEntity (e:Entity):T = {
		entity = e
		this.asInstanceOf[T]
	}

	def key = entity.getKey

	def update (key:Symbol, value:Any):Unit = {
		entity.setProperty(key.name, value)
	}

	def apply (key:Symbol):Any = {
		entity.getProperty(key.name)
	}

	def param(key:String):Any = {
		apply(Symbol(key))
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
