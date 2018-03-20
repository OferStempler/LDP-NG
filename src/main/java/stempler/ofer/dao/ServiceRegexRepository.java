package stempler.ofer.dao;

import stempler.ofer.model.entities.ServiceRegularExpressions;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceRegexRepository extends MongoRepository<ServiceRegularExpressions, String>{

	List<ServiceRegularExpressions> findByServiceId(int serviceId);
}
