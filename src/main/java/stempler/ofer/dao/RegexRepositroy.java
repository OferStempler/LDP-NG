package stempler.ofer.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import stempler.ofer.model.entities.RegularExpressions;


public interface RegexRepositroy  extends MongoRepository<RegularExpressions, String>   {

	RegularExpressions findByRegexId(int regexId);
	
}
