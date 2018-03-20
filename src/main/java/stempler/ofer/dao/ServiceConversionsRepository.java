package stempler.ofer.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import stempler.ofer.model.entities.ServiceConversions;

public interface ServiceConversionsRepository extends MongoRepository<ServiceConversions, String>{

	ServiceConversions findByServiceId(int serviceId);

}
