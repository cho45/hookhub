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
		datastore.prepare(query).asIterator.asInstanceOf[Iterator[Entity]].map[T] {
			this.getClass.newInstance.asInstanceOf[T].setEntity(_)
		}
	}

	def find (args: (Symbol, Any)*):T = select(args:_*).next

	// find or create with first value
	def instantiate (arg: (Symbol, Any)) = find(arg) match {
		case null    => create(arg)
		case ret @ _ => ret
	}

	// instance method
	def setEntity (e:Entity):T = {
		entity = e
		this.asInstanceOf[T]
	}

	def update (key:Symbol, value:Any):Unit = {
		entity.setProperty(key.name, value)
	}

	def apply (key:Symbol):Any = {
		entity.getProperty(key.name)
	}

	def save ():Unit = {
		datastore.put(entity)
	}
}
