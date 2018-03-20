package stempler.ofer.detectors.validators;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import stempler.ofer.dao.SchemaRepository;
import stempler.ofer.dao.ServiceDependenciesRepositroy;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.MapsHandler;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.service.AuditService;
import stempler.ofer.utils.Utils;
import lombok.extern.log4j.Log4j;

@ValidatorType(priority = 1)
@Component
@Log4j
public class DepthDetector extends Detector {

	@Autowired
	MapsHandler mapsHandler;

	@Autowired
	SchemaRepository schemasRepo;

	@Autowired
	AuditService auditService;

	@Autowired
	ServiceDependenciesRepositroy serviceDependencyrepo;

	private List<ServiceDependencies> serviceDepList = new ArrayList<ServiceDependencies>();

	@Override
	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest) {

		boolean isValid = true;
		boolean didValidate = false;

		// check if dependency is enabled
		serviceDepList = extendedService.getServiceDependencies();

		for (ServiceDependencies serviceDependencies : serviceDepList) {

			int enabled = serviceDependencies.getEnabled();
			// check that the service requires depth check (2), matches the
			// message type, and that service is enabled;
			if (serviceDependencies.getDependencyId() == DetectorTypes.DEPTH_VALIDATION.code // 2
					&& serviceDependencies.getMessageType().equals(messageType) && enabled == 1) {

				log.debug("Starting validation for Message max Depth");
				int serviceValue = 0;
				try {
					serviceValue = Integer.parseInt(serviceDependencies.getDependencyValue());
				} catch (Exception e) {
					log.error("Could not parse dependency value to Integer. Make sure for this dependency values are only numbers " + e);
					return false;
				}
				log.debug("Max depth value from db is ["+serviceValue+"]");

				didValidate = true;
				Integer currentXmlDepth = null;
				Document doc;

				try {
					doc = Utils.buildXML(content.toString());
					if (doc == null) {
						isValid = false;
					} else {
						Node root = doc.getFirstChild();
								int depth = serviceValue;
								if (depth == 0) {
									String error = "no max depth is  defined for service";
									log.error(error);
									auditService.requestResponseBuildAudit(extendedService, content.toString(), error,id);
									isValid = false;
									return isValid;
								}
								log.debug("Calculating message depth");
								currentXmlDepth = Utils.maxDepth(root);
								log.debug("Message current depth is [" +currentXmlDepth+ "]");
								if (currentXmlDepth > depth) {
									isValid = false;
									String error = "Message XML's depth is too big. Max depth: [" + depth + "]. Current depth: ["+ currentXmlDepth + "].";
									log.error(error);
									auditService.requestResponseBuildAudit(extendedService, content.toString(), error,id);
									return isValid;
								} else {
									log.debug("Message depth size is smaller than the Max Depth Value");
								}
					}

				} catch (Exception e) {
					String error = ("Could not build doc for one of the servives Schemas [" + extendedService +" ]" + e);
					log.error(error);
					auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
					isValid = false;
					return isValid;
				}
			}
		}
		if (isValid && didValidate) {
			log.debug("Successfully validated message depth for [" + messageType + "] message");
		}
		return isValid;
	}

	@Override
	public void init() {

	}

}
