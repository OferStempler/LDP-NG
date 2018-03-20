package stempler.ofer.detectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import stempler.ofer.dao.SchemaRepository;
import stempler.ofer.dao.ServiceConversionsRepository;
import stempler.ofer.dao.ServiceDependenciesRepositroy;
import stempler.ofer.dao.ServiceRegexRepository;
import stempler.ofer.dao.ServiceReplacemntsRepository;
import stempler.ofer.dao.ServiceRepository;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.Schemas;
import stempler.ofer.model.entities.ServiceConversions;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.entities.ServiceRegularExpressions;
import stempler.ofer.model.entities.ServiceReplacements;
import stempler.ofer.model.entities.Service;
import lombok.extern.log4j.Log4j;


@Component
@Log4j
public class ServiceLoader  {

	
	@Autowired
	private MapsHandler       mapsHandler;
	
	@Autowired	
	private ServiceRepository serviceRepository;
	
	@Autowired
	private SchemaRepository  schemaRepository;
	
	@Autowired
	private ServiceDependenciesRepositroy serviceDependenciesRepositroy;
	
	@Autowired
	private ServiceRegexRepository serviceRegexRepository;
	
	@Autowired
	private ServiceConversionsRepository serviceConversionsRepository;
	
	@Autowired
	private ServiceReplacemntsRepository serviceReplacmnetsRepository;

	private List<Service> serviceList;
	
	private List<Schemas> schemasList;
	
	private List<ServiceDependencies> serviceDependenciesList;
	
	private List<ServiceRegularExpressions> serviceRegexList;
	
	private List<ServiceConversions> serviceConversionsList;
	
	private List<ServiceReplacements> serviceReplacementsList;
	
	//-----------------------------------------------------------------------------------------------------------------
    // creates the extended Services and url map
	@PostConstruct
	public void init() {

		String serviceUri= "";
		int counter = 0;

		Optional.ofNullable( serviceRepository             ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - serviceRepository is null"));
		Optional.ofNullable( schemaRepository              ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - schemaRepository is null"));
		Optional.ofNullable( serviceDependenciesRepositroy ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - serviceDependenciesRepositroy is null"));
		Optional.ofNullable( serviceRegexRepository        ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - serviceRegexRepository is null"));
		Optional.ofNullable( serviceConversionsRepository  ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - serviceConversionsRepo is null"));
		Optional.ofNullable( serviceReplacmnetsRepository  ).orElseThrow( ()-> new RuntimeException("ServiceLoader.init() - FATAL - serviceReplacmnetsRepo is null"));


		
		serviceList             = serviceRepository.findAll();
		schemasList             = schemaRepository.findAll();
		serviceDependenciesList = serviceDependenciesRepositroy.findAll();
		serviceRegexList        = serviceRegexRepository.findAll();
		serviceConversionsList  = serviceConversionsRepository.findAll();
		serviceReplacementsList = serviceReplacmnetsRepository.findAll();
		
		Set<Schemas> schemasSet                           = null;
		List<ServiceDependencies> serviceDepList          = null;
		List<ServiceRegularExpressions> serviceRegexList2 = null;
		List<ServiceReplacements> serviceReplacemntsList2  = null;
		
		
		if ( serviceList == null || serviceList.size() == 0 ){
			throw new RuntimeException("ServiceLoader.init() - FATAL - Global serviceList is null or empty! Should not occur.");
		}
		
		for ( Service service : serviceList ) {
			 log.trace("Scanning service:[" + service + "]");
			 
			 schemasSet        			= new HashSet<Schemas>();
			 serviceDepList    			= new ArrayList<ServiceDependencies>();
			 serviceRegexList2 			= new ArrayList<ServiceRegularExpressions>();
			 serviceReplacemntsList2	= new ArrayList<ServiceReplacements>();
			
			 if ( service != null ){
				ExtendedService extendedService = new ExtendedService();
				serviceUri                      = service.getUri();
				
				log.trace("Scanning for schema matching service.getServiceId():[" + service.getServiceId() + "]");
				for (Schemas schema : schemasList) {
					if (schema.getServiceId() != 0 && schema.getServiceId() == service.getServiceId() ){
						log.trace("schema matching service.getServiceId():[" + service.getServiceId() + "] Was found:[" + schema + "]");
						schemasSet.add(schema);
					}
				}
				
				log.trace("Scanning for ServiceRegularExpressions matching service.getServiceId():[" + service.getServiceId() + "]");
				for (ServiceRegularExpressions serviceRegularExpression : serviceRegexList) {
					if (serviceRegularExpression.getServiceId() != 0 && serviceRegularExpression.getServiceId() == service.getServiceId()){
						log.trace("serviceRegularExpression matching service.getServiceId():[" + service.getServiceId() + "] Was found:[" + serviceRegularExpression + "]");
						serviceRegexList2.add(serviceRegularExpression);
					}
				}

				log.trace("Scanning for serviceDependency matching service.getServiceId():[" + service.getServiceId() + "]");
				for (ServiceDependencies serviceDependency : serviceDependenciesList) {
					if(serviceDependency.getServiceId() != 0 && serviceDependency.getServiceId() == service.getServiceId()){
						log.trace("serviceDependency matching service.getServiceId():[" + service.getServiceId() + "] Was found:[" + serviceDependency + "]");
						serviceDepList.add(serviceDependency);
					}
				}
				log.trace("Scanning for serviceConversions matching service.getServiceId():[" + service.getServiceId() + "]");
				for (ServiceConversions serviceConversion : serviceConversionsList) {
					if(serviceConversion.getServiceId() != 0 && serviceConversion.getServiceId() == service.getServiceId()){
						log.trace("serviceConversion matching service.getServiceId():[" + service.getServiceId() + "] Was found:[" + serviceConversion + "]");
						extendedService.setServiceConversions(serviceConversion);
					}
				}
				log.trace("Scanning for serviceReplaments matching service.getServiceId():[" + service.getServiceId() + "]");
				for (ServiceReplacements serviceReplacments : serviceReplacementsList) {
					if(serviceReplacments.getServiceId() != 0 && serviceReplacments.getServiceId() == service.getServiceId()){
						log.trace("serviceReplaments matching service.getServiceId():[" + service.getServiceId() + "] Was found:[" + serviceReplacments + "]");
						serviceReplacemntsList2.add(serviceReplacments);
					}
				}
				
				extendedService.setShemas(schemasSet);	
				extendedService.setService(service);
				extendedService.setServiceDependencies(serviceDepList);
				extendedService.setServiceRegex(serviceRegexList2);
				extendedService.setServiceReplacments(serviceReplacemntsList2);
				
				mapsHandler.getServiceUriMap().put(serviceUri, extendedService);
				counter++;
			}//if
		}//for (Services service : serviceList) 
		log.debug("Service loader successfully loaded [" + counter + "] services");
	}
	//-----------------------------------------------------------------------------------------------------------------
	public ServiceRepository getServiceRepo() {
		return serviceRepository;
	}

	public void setServiceRepo(ServiceRepository serviceRepo) {
		this.serviceRepository = serviceRepo;
	}

	public List<Service> getServiceList() {
		return serviceList;
	}

	public void setServiceList(List<Service> serviceList) {
		this.serviceList = serviceList;
	}

	public MapsHandler getServiceUriMap() {
		return mapsHandler;
	}

	public void setServiceUriMap(MapsHandler serviceUriMap) {
		this.mapsHandler = serviceUriMap;
	}

	public MapsHandler getMapsHandler() {
		return mapsHandler;
	}

	public void setMapsHandler(MapsHandler mapsHandler) {
		this.mapsHandler = mapsHandler;
	}

	public SchemaRepository getSchemasRepo() {
		return schemaRepository;
	}

	public void setSchemasRepo(SchemaRepository schemasRepo) {
		this.schemaRepository = schemasRepo;
	}
	//-----------------------------------------------------------------------------------------------------------------
}
