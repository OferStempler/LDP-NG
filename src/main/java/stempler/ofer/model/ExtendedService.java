package stempler.ofer.model;

import stempler.ofer.model.entities.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExtendedService {
	
	private Service                         service;
	private Set<Schemas>                    schemas;
	private List<ServiceRegularExpressions> serviceRegex;
	private List<ServiceDependencies>       serviceDependencies;
	private ServiceConversions              serviceConversions;
	private Map<String , String[]>          parametersMap;
	private List<ServiceReplacements>        serviceReplacments;

	public ExtendedService() {	}

	public Service getService() {
		return service;
	}
	public void setService(Service service) {
		this.service = service;
	}
	public Set<Schemas> getShemas() {
		return schemas;
	}
	public void setShemas(Set<Schemas> shemas) {
		this.schemas = shemas;
	}
	public List<ServiceRegularExpressions> getServiceRegex() {
		return serviceRegex;
	}
	public void setServiceRegex(List<ServiceRegularExpressions> serviceRegex) {
		this.serviceRegex = serviceRegex;
	}
	public List<ServiceDependencies> getServiceDependencies() {
		return serviceDependencies;
	}
	public void setServiceDependencies(List<ServiceDependencies> serviceDependencies) {
		this.serviceDependencies = serviceDependencies;
	}
	public Set<Schemas> getSchemas() {
		return schemas;
	}
	public void setSchemas(Set<Schemas> schemas) {
		this.schemas = schemas;
	}
	public Map<String, String[]> getParametersMap() {
		return parametersMap;
	}
	public void setParametersMap(Map<String, String[]> parametersMap) {
		this.parametersMap = parametersMap;
	}
	public ServiceConversions getServiceConversions() {
		return serviceConversions;
	}
	public void setServiceConversions(ServiceConversions serviceConversions) {
		this.serviceConversions = serviceConversions;
	}


	public List<ServiceReplacements> getServiceReplacments() {
		return serviceReplacments;
	}

	public void setServiceReplacments(List<ServiceReplacements> serviceReplacments) {
		this.serviceReplacments = serviceReplacments;
	}

	@Override
	public String toString() {
		return "ExtendedService [service=" + service + ", schemas=" + schemas + ", serviceRegex=" + serviceRegex
				+ ", serviceDependencies=" + serviceDependencies + ", serviceConversions=" + serviceConversions
				+ ", parametersMap=" + parametersMap + ", serviceReplacments=" + serviceReplacments + "]";
	}
	
	
	
}
