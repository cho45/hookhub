import com.google.appengine.api.datastore._

class DS [T <: DS[T]] () {
	var entity:Entity = null
	val datastore = DatastoreServiceFactory.getDatastoreService

	// def _entity_= (e:Entity) = entity = Entity

	// class method
	def entityName = this.getClass.getName

	def create (args: (Symbol, Any)*) = {
		val ent = new Entity(entityName)
		val ret = this.getClass.newInstance.asInstanceOf[T]
		ret.entity = ent

		for ( (key, value) <- args) {
			ret(key) = value
		}

		ret
	}

	def find_all (args: (Symbol, Any)*) = {
		val query = new Query(entityName)
		for ( (key, value) <- args) {
			query.addFilter(key.name, Query.FilterOperator.EQUAL, value)
		}
		datastore.prepare(query).asIterator
	}

	// instance method
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
