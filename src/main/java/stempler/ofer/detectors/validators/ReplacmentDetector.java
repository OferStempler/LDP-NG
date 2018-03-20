package stempler.ofer.detectors.validators;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import stempler.ofer.dao.ServiceDependenciesRepositroy;
import stempler.ofer.dao.ServiceReplacemntsRepository;
import stempler.ofer.dao.ServiceRepository;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.NodesContextAndIndex;
import stempler.ofer.model.entities.Service;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.entities.ServiceReplacements;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.utils.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@ValidatorType(priority = 6)
@Component
@Log4j
public class ReplacmentDetector  extends Detector{

	@Autowired
	ServiceReplacemntsRepository serviceReplacementRepo;
	
	@Autowired
	ServiceDependenciesRepositroy serviceDependenciesRepo;
	
	@Autowired
	ServiceRepository serviceRepo;
	
	List<ServiceDependencies> serviceDependenciesList = null;
	
	@Override
	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id,HttpServletRequest servletRequest) {
		boolean isValid = true;
		
		serviceDependenciesList = extendedService.getServiceDependencies();
		for (ServiceDependencies serviceDependency : serviceDependenciesList) {
			if (serviceDependency != null
					&& serviceDependency.getDependencyId() == DetectorTypes.REPLACE_FIELD_CONTENT.code // 7
					&& serviceDependency.getMessageType().equals(messageType)
					&& serviceDependency.getEnabled() == 1) {
				
				List<ServiceReplacements> serviceReplacementList = serviceReplacementRepo.findByServiceId(extendedService.getService().getServiceId());
				for (ServiceReplacements serviceReplacement : serviceReplacementList) {
					
				
				String replacementValue =  (serviceReplacement != null) ? serviceReplacement.getReplacementValue() : null;
				String field			=  (serviceReplacement != null) ? serviceReplacement.getField() : null;
				log.debug("Starting value replacement validation. Replacing field: ["+ field+ "] with value: [" +replacementValue+ "]");
					if (replacementValue != null && !StringUtils.isEmpty(field)) {
						try {
							NodesContextAndIndex nodesContextAndIndex = new NodesContextAndIndex();
							XPath xPath = XPathFactory.newInstance().newXPath();
							String expression = "//*[not(*)]";
							// 1. xml to doc
							Document doc = Utils.buildXML(content.toString());
							NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc,
									XPathConstants.NODESET);
							// 2. find field with xpath
							for (int i = 0; i < nodeList.getLength(); i++) {
//								 System.out.println(Utils.getXPath(nodeList.item(i)));
								if (Utils.getXPath(nodeList.item(i)).endsWith(field)) {
									Node currNode = nodeList.item(i);
									String nodeValue = currNode.getTextContent();
									nodesContextAndIndex.setNodeContextText(nodeValue);
									// 3. check from and to.
									nodesContextAndIndex = setFromAndTo(nodesContextAndIndex, serviceReplacement,content.toString(), currNode.getNodeName());
									// 4. replace string buffer with new value
									isValid = replaceAndUpdate(nodesContextAndIndex, replacementValue, content);
									if (!isValid) {
										log.error("Could not replace String buffer field with new value.");
										return false;
									}
								}
							}
						} catch (Exception e) {
							log.error("Could not replace field's value. " + e);
							return false;
						}
					} else {
					log.error("replacement Value is null, or field value is null or empty.");
					return false;
				}

			}
		}
		}
		
		return isValid;
	}
//--------------------------------------------------------------------------------------------------------------------------------------	
private boolean replaceAndUpdate(NodesContextAndIndex nodesContextAndIndex, String replacementValue,StringBuffer content) {
	//replace the String buffer with new value
	if (nodesContextAndIndex != null) {
		content.replace(
				nodesContextAndIndex.getEndOfNodeContext(),
				nodesContextAndIndex.getEndOfNodeContext() + nodesContextAndIndex.getNodeContextText().length(),
				replacementValue);
		log.debug("Succssffuly replaced StringBuffer with new value");
		return true;
	}
	return false;
}
	
//--------------------------------------------------------------------------------------------------------------------------------------	
	private NodesContextAndIndex setFromAndTo(NodesContextAndIndex nodesContextAndIndex,
			ServiceReplacements serviceReplacement, String contnet, String nodeName) {

		String nodeValue = nodesContextAndIndex.getNodeContextText();
		String from = serviceReplacement.getFrom();
		String to = serviceReplacement.getTo();
		int endOfRequiredNodeContext = 0;
		String nodeAndValue = "<"+nodeName+">" + nodeValue;
		String nodeNameWithTags = "<"+nodeName+">";
		// replace all content
		if (StringUtils.isEmpty(from) && StringUtils.isEmpty(to)) {
			log.debug("Replacing entire node value with new value");
			// replace from 'from' value to the end
		} else if (!StringUtils.isEmpty(from) && StringUtils.isEmpty(to)) {
			log.debug("Replacing node value from [" + from + "], to the end of the node");
			if (nodeValue.contains(from)) {
				nodeValue = nodeValue.substring(nodeValue.indexOf(from), nodeValue.length());
			} else {
				log.error("Could not find starting value to replace. Doing nothing");
			}
			// replace from 'from' value to 'to' valur
		} else if (!StringUtils.isEmpty(from) && !StringUtils.isEmpty(to)) {
			log.debug("Replacing node value from [" + from + "], to [" + to + "]");
			if (nodeValue.contains(from) && nodeValue.contains(to)) {
				String FromNodeValue = nodeValue.substring(0, nodeValue.indexOf(from));
				String replaceValue = nodeValue.substring(nodeValue.indexOf(from), nodeValue.indexOf(to) + to.length());
				endOfRequiredNodeContext = contnet.lastIndexOf(nodeAndValue) + nodeNameWithTags.length() +FromNodeValue.length();
				nodesContextAndIndex.setEndOfNodeContext(endOfRequiredNodeContext);
				nodesContextAndIndex.setNodeContextText(replaceValue);
				return nodesContextAndIndex;
			} else {
				log.error("Could not find 'from' values and/or 'to' values to replace. Doing nothing");
			}
		} else if ((StringUtils.isEmpty(from) && !StringUtils.isEmpty(to))){
			log.debug("Replacing node value from the beginning of the tag to [" + to + "]");
			String replaceValue = nodeValue.substring(0, nodeValue.indexOf(to) + to.length());
			endOfRequiredNodeContext = contnet.lastIndexOf(nodeAndValue) + nodeNameWithTags.length();
			nodesContextAndIndex.setEndOfNodeContext(endOfRequiredNodeContext);
			nodesContextAndIndex.setNodeContextText(replaceValue);
			return nodesContextAndIndex;
		}
		//This is to make sure we get the value from the required node, and we dont replace the same value from a different node
		endOfRequiredNodeContext = contnet.lastIndexOf(nodeAndValue) +nodeAndValue.length() - nodeValue.length();
		nodesContextAndIndex.setEndOfNodeContext(endOfRequiredNodeContext);
		nodesContextAndIndex.setNodeContextText(nodeValue);
		return nodesContextAndIndex;
	}
//--------------------------------------------------------------------------------------------------------------------------------------	

	@Override
	public void init() {
		
		List<ServiceDependencies> serviceDepList = serviceDependenciesRepo.findAll();
		Map<String, Integer> map = new HashMap<>();
		for (ServiceDependencies serviceDependencies : serviceDepList) {
			if (serviceDependencies.getDependencyId() == DetectorTypes.REPLACE_FIELD_CONTENT.code){
				Service service = serviceRepo.findByServiceId(serviceDependencies.getServiceId());
				if (map.containsKey(service.getServiceName())){
					map.put(service.getServiceName(), map.get(service.getServiceName())+ 1 );
				} else {
					map.put(service.getServiceName(), 1);
				}
			}
		}
		if(!map.isEmpty()){
			for (Map.Entry<String , Integer> entry : map.entrySet()) {
				
				log.debug("Will perrform the ReplacementDetector on service [" +entry.getKey()+"] on [" +entry.getValue()+ "] different fields");
			}
		}
	}

}
