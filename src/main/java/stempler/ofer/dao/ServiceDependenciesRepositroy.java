package stempler.ofer.dao;

import stempler.ofer.model.entities.ServiceDependencies;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceDependenciesRepositroy extends MongoRepository<ServiceDependencies, String> {

	List<ServiceDependencies> findByServiceId(int serviceId);
	
	 ServiceDependencies findByMessageTypeAndServiceId (String messageType, String serviceId);
	 
	 
	 ServiceDependencies findByServiceIdAndMessageType(String serviceId, String messageType);
}
