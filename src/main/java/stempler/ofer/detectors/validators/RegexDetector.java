package stempler.ofer.detectors.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import stempler.ofer.dao.RegexRepositroy;
import stempler.ofer.dao.ServiceConversionsRepository;
import stempler.ofer.dao.ServiceRegexRepository;
import stempler.ofer.dao.ServiceRepository;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.MapsHandler;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ElementRegex;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.RegularExpressions;
import stempler.ofer.model.entities.Service;
import stempler.ofer.model.entities.ServiceConversions;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.entities.ServiceRegularExpressions;
import stempler.ofer.model.enums.ContentTypes;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.utils.Utils;
import lombok.extern.log4j.Log4j;

@ValidatorType(priority = 3)
@Component
@Log4j
public class RegexDetector extends Detector {

//	@Autowired
//	Utils utils;

	@Autowired
	MapsHandler mapsHandler;

	@Autowired
	ServiceRepository servicesRepos;

	@Autowired
	private RegexRepositroy regexRepo;

	@Autowired
	private ServiceRegexRepository serviceRegexRepo;
	
	@Autowired
	private ServiceConversionsRepository serviceConversionRepo;

	private List<ServiceDependencies>       serviceDependenciesList = new ArrayList<ServiceDependencies>();
	private List<RegularExpressions>        regexList;
//	private List<ServiceRegularExpressions> serviceRexesList;
	private Map<String, List<ElementRegex>> serviceElementMap;
	private List<Service>                  serviceList;

	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest) {
		boolean isValid 					 = true;
		final String ELEMENT_SEP 			 = ".";
		final String ELEMENT_NS_SEP 		 = ":";
		List<ElementRegex>  regExElementList = new ArrayList<ElementRegex>();
		serviceDependenciesList 			 = extendedService.getServiceDependencies();

		 
		 
		 
		for (ServiceDependencies serviceDependencies : serviceDependenciesList) {
			int enabled = serviceDependencies.getEnabled();
			// check that the service requires regex check, (1), matches the message type, and that service is enabled;
			if (serviceDependencies.getDependencyId() == DetectorTypes.REGEX_VALIDATION.code // 1
				&& serviceDependencies.getMessageType().equals(messageType) && enabled == 1) {

				regExElementList = mapsHandler.getServiceElementMap().get(extendedService.getService().getServiceName() + "_" + messageType);
				if (regExElementList != null && regExElementList.size() > 0) {
					log.debug("Found [" + regExElementList.size() + "] regexes for service ["+ extendedService.getService().getServiceName() + "]. Starting regex validation.");
					Document doc;

					try {
						doc = Utils.buildXML(content.toString());
						} catch (Exception e) {
							isValid = false;
							String error = "Could not build XML doc for validate regex. servcice ["+ extendedService.getService().getServiceName() + "]" + e;
							log.error(error);
							auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
							break;	
						}
					
					
					for (int i = 0; i < regExElementList.size() && isValid; i++) {

						ElementRegex currElement = regExElementList.get(i);
						// check if element is enabled or blocked
						if (currElement.getEnabled() != 1) {
							continue;
						}

						int lastElementIndex = currElement.getElement().lastIndexOf(ELEMENT_SEP);
						String elementName = currElement.getElement().substring(lastElementIndex + 1);


								// NodeList elementsInXML =
								// doc.getElementsByTagNameNS(ns,elementName);
								NodeList elementsInXML = doc.getElementsByTagName(elementName);
								for (int j = 0; j < elementsInXML.getLength() && isValid ; j++) {

									Node currNode = elementsInXML.item(j);
									String currNodeName = currNode.getNodeName();
									String nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName);
									Node parentNode = currNode.getParentNode();


									// building node full name
									while (!(parentNode instanceof Document)) {
										currNodeName = parentNode.getNodeName();
										nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName) + ELEMENT_SEP + nodeFullName;
										parentNode = parentNode.getParentNode();
									}

									// we found the element! hurrah!
									if (nodeFullName.equals(currElement.getElement())) {
										if (!currNode.getTextContent().matches(currElement.getRegex())) {
											isValid = false;
											String error = ("Element [" + elementName + "] with value ["
													+ currNode.getTextContent() + "] in service ["
													+ extendedService.getService().getServiceName() + "] MessageType [" + messageType
													+ "] is not valid for regexName [" + currElement.getRegexName() + "] regex: ["
													+ currElement.getRegex()+"]");
											log.error(error);
											auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
											break;
										}
									}
								}
							}					
					}// if regExElementList is null or empty				
				else {
					isValid = false;
				String error = "No regexes were found in ServiceRegularExpressions table for service [ " +extendedService.getService().getServiceName()+ "]. Enter regexes for this service or disable regex check under serviceDependencies";
				log.error(error);
				auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
				break;
				}
			}

		}
		if (isValid) {
			log.debug("Successfully validated Regex for [" + messageType + "] message");
		} else {
			log.error("Failed Regex Validation for [" + messageType + "] message");
			
		}
		return isValid;
	}

	@Override
	public void init() {

		serviceElementMap = mapsHandler.getServiceElementMap();
		serviceList = servicesRepos.findAll();
		regexList = regexRepo.findAll();
//		serviceRexesList = serviceRegexRepo.findAll();
		
		String currentService = "";

		for (Service service : serviceList) {
			
			List<ElementRegex> regExElementList = new ArrayList<ElementRegex>();
			int serviceId = service.getServiceId();
			currentService = service.getServiceName();
			ServiceConversions serviceConversions = serviceConversionRepo.findByServiceId(service.getServiceId());
			// loads the serviceElementMap with serviceName+request/reply, and
			// an Array of its elementRegex
			//check if message is Json and add root element for it so
			String addRootElementTo = checkContentTypeIsJSON(service, serviceConversions);
			
			//-----
			List<ServiceRegularExpressions> serviceRegexList =  serviceRegexRepo.findByServiceId(serviceId);
			for (ServiceRegularExpressions serviceRexes : serviceRegexList) {
				//avoid loading undefined serviceRegexes:
				if (serviceRexes.getRegexId() != 0){
					
				
				String messageType = serviceRexes.getMessageType();
				String element = serviceRexes.getElement();
				String newElement = "";
				switch (addRootElementTo) {
				case "Both":
					newElement = messageType + "." + element;
					break;
				case "Request":
					if (messageType.equals("Request")) {
						newElement = "Request." + element;
					} else {
						newElement = element;
					}
					break;
				case "Respponse":
					if (messageType.equals("Response")) {
						newElement = "Response." + element;
					} else {
						newElement = element;
					}
					break;
				case "None":
					newElement = element;
					break;
				}
				
				regExElementList.add(new ElementRegex
						(newElement, //new element with "Request." or "Response" root element for JSON content
						regexRepo.findByRegexId(serviceRexes.getRegexId()).getName(), //get regex name
						regexRepo.findByRegexId(serviceRexes.getRegexId()).getValue(), //get regex value
						serviceRexes.getEnabled() //get enabled
								));
				serviceElementMap.put(currentService + "_"+ messageType, regExElementList); // MessageType={Request | Response}
			} else {
				log.debug("serviceRexes with serviceElement [" +serviceRexes.getElement()+"] is not defined with a valid Regex. Not loading to serviceElementMap");
			}
			}
			}
			//-----
			// old loads REQEUSTS
//			for (ServiceRegularExpressions serviceRexes : serviceRexesList) {
//				if (serviceId != 0 && serviceId == serviceRexes.getServiceId()
//						&& serviceRexes.getMessageType().toLowerCase()
//								.equals("request")) {
//
//					for (RegularExpressions regularExpression : regexList) {
//						if (regularExpression.getRegexId() != 0 && regularExpression.getRegexId() == serviceRexes.getRegexId()) {
//
//							{
//							regExElementList.add(new ElementRegex(serviceRexes
//									.getElement(),regularExpression.getName(),
//									regularExpression.getValue(), serviceRexes
//											.getEnabled()));
//
//						}
//					}
//
//					// System.out.println(currentService +
//					// "_"+serviceRexes.getMessageType());
//					serviceElementMap.put(
//							currentService + "_"
//									+ serviceRexes.getMessageType(), regExElementList); // MessageType={Request | Response}
//				}
//
//				// loads RESPONSE
//				if (serviceId != 0 && serviceId == serviceRexes.getServiceId()
//						&& serviceRexes.getMessageType() != null 
//						&& serviceRexes.getMessageType()
//						               .toLowerCase()
//								       .equals("response")) {
//
//					for (RegularExpressions regularExpression : regexList) {
//						if (regularExpression.getRegexId() != 0 
//							&& regularExpression.getRegexId() == serviceRexes.getRegexId())  {
//							   regExElementList.add(new ElementRegex(serviceRexes.getElement(), 
//									                                 regularExpression.getName(),
//									                                 regularExpression.getValue(), 
//									                                 serviceRexes.getEnabled()) );
//						}
//					}//for regexList
//					serviceElementMap.put(currentService + "_"+ serviceRexes.getMessageType(), regExElementList);
//				}
//				}
//			}//for eache service regex 
//		}//for each service

	}

	private String checkContentTypeIsJSON(Service service, ServiceConversions serviceConversions) {
		if (service.getContentType().equals(ContentTypes.JSON.synonym)) {
			return "Both";
		}
		if (service.getContentType().equals(ContentTypes.COMPOSITE.synonym)
				&& serviceConversions.getSourceRequestInputType().equals(ContentTypes.JSON.synonym)
				&& serviceConversions.getDestinationResponseInputType().equals(ContentTypes.JSON.synonym)) 
				 {
			return "Both";
		}
		if (service.getContentType().equals(ContentTypes.COMPOSITE.synonym)
				&& serviceConversions.getSourceRequestInputType().equals(ContentTypes.JSON.synonym)) {
			return "Request";
		}
		if (service.getContentType().equals(ContentTypes.COMPOSITE.synonym)
				&& serviceConversions.getDestinationResponseInputType().equals(ContentTypes.JSON.synonym)) {
			return "Response";
		}
		return "None";
	}



	public RegexRepositroy getRegexRepo() {
		return regexRepo;
	}

	public void setRegexRepo(RegexRepositroy regexRepo) {
		this.regexRepo = regexRepo;
	}

	public List<RegularExpressions> getRegexList() {
		return regexList;
	}

	public void setRegexList(List<RegularExpressions> regexList) {
		this.regexList = regexList;
	}

}
