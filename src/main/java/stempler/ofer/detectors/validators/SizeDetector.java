package stempler.ofer.detectors.validators;

import stempler.ofer.dao.SchemaRepository;
import stempler.ofer.dao.ServiceDependenciesRepositroy;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.MapsHandler;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.service.AuditService;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.log4j.Log4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ValidatorType(priority = 2)
@Component
@Log4j
public class SizeDetector extends Detector {


	@Autowired
	MapsHandler mapsHandler;

	@Autowired
	SchemaRepository schemasRepo;
	
	@Autowired
	AuditService auditService;
	
	@Autowired
	ServiceDependenciesRepositroy serviceDependencyRepo;

	private List<ServiceDependencies> serviceDependenciesList = new ArrayList<ServiceDependencies>();
	
	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest) {


		 boolean didValidate = false;
		 boolean isValid = true;

		 if (content == null) {
			 return isValid;
		 }
		serviceDependenciesList = extendedService.getServiceDependencies();

		for (ServiceDependencies serviceDependencies : serviceDependenciesList) {
			int enabled = serviceDependencies.getEnabled();
			//check that the service requires xml Sieze check (3), matches the message type, and that service is enabled; 
			if(serviceDependencies.getDependencyId() == DetectorTypes.SIZE_VALIDATION.code //3 
			   && serviceDependencies.getMessageType().equals(messageType) && enabled == 1){
				if(serviceDependencies.getDependencyValue()!=null ||!serviceDependencies.getDependencyValue().equals("")){
					
					
					didValidate = true;
					int maxSize = 0;
					log.debug("Starting validation for message max Size");
					try {
				 maxSize = Integer.parseInt(serviceDependencies.getDependencyValue());
					} catch (Exception e){
						log.error("Could not parse dependency value to Integer. Make sure for this dependency values are only numbers");
						return false;
					}
					log.debug("Max size value from db is ["+maxSize+"]");
				if (maxSize == 0 ){
					String error = "No max size is  defined for service";
					log.error(error);
					auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
					isValid = false;
					return isValid;
				}
				byte[] currentSizeBytes = content.toString().getBytes();
				int currentSize = currentSizeBytes.length;
				log.debug("Message XML byt size is ["+currentSize+"] bytes");
				if(currentSize >maxSize){
					isValid =false;			
					String error = "Content size is too big. Max size: ["
							+ maxSize + "]. Current size: ["
							+ currentSize + "].";
					log.error(error);
					auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
					return isValid;
				} else {
					log.debug("Message byte size is smaller than the Max Size Value");
				}
					
				}
			}
		}
		if(isValid && didValidate)
		log.debug("successfully validated xml Size for [" + messageType + "] message");
		return isValid;
	}

	@Override
	public void init() {}
}
