package stempler.ofer.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;

import stempler.ofer.model.entities.Audit;

public interface AuditRepository extends MongoRepository<Audit, String> {

	Audit findByGuid(String guid);
	
	@Query (value = "{date : ?0}")
	public Long  deleteAuditByDate(String date);
	
	@Query (value = "{date : ?0}")
	public Long  deleteAuditByDateBefore(String date);
	
//	Long deleteAuditByDate(String date);
	
	 @Query (value = "{id : ?0}")
	  public Long deleteAuditBy_id(String id);
	
}
