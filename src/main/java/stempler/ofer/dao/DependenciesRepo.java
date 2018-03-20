package stempler.ofer.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import stempler.ofer.model.entities.Dependency;

public interface DependenciesRepo extends MongoRepository<Dependency, String>{

	Dependency findByDependencyId(int dependencyId);
}
