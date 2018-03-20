package stempler.ofer.dao;

import stempler.ofer.model.entities.Schemas;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SchemaRepository extends MongoRepository<Schemas, String> {

	List<Schemas> findByServiceId(int serviceId);
	
	//gets the the schema with the highest id value
	public Schemas findTopByOrderByIdDesc();
	
//	public List<Schemas> findById(int serviceId);
}
