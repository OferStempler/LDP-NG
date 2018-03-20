package stempler.ofer.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import stempler.ofer.model.entities.Service;


public interface ServiceRepository extends MongoRepository<Service, String> {
	
	Service findByServiceId(int serviceId);
	Service findByServiceName(String servicename);
}
