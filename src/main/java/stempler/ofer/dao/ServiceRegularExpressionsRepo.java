package stempler.ofer.dao;

import org.springframework.data.mongodb.repository.MongoRepository;
import stempler.ofer.model.entities.ServiceRegularExpressions;

import java.util.List;

public interface ServiceRegularExpressionsRepo extends MongoRepository<ServiceRegularExpressions, String>{

//	ServiceRegularExpressions findByServiceId(int serviceId);
	
	public List<ServiceRegularExpressions> findByServiceId(int serviceId);
	
	public ServiceRegularExpressions findTopByOrderByRegexIdDesc();

	
}
