package stempler.ofer.dao;

import stempler.ofer.model.entities.ServiceReplacements;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceReplacemntsRepository extends MongoRepository<ServiceReplacements, String> {

	List<ServiceReplacements> findByServiceId(int serviceId);
}
