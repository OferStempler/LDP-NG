package stempler.ofer.detectors.validators;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import stempler.ofer.dao.SchemaRepository;
import stempler.ofer.dao.ServiceDependenciesRepositroy;
import stempler.ofer.dao.ServiceRepository;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.MapsHandler;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.SchemaType;
import stempler.ofer.model.entities.Schemas;
import stempler.ofer.model.entities.Service;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.enums.ContentTypes;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.model.enums.MessageTypes;
import stempler.ofer.model.enums.XSDType;
import stempler.ofer.service.AuditService;
import stempler.ofer.utils.Utils;
import lombok.extern.log4j.Log4j;

@ValidatorType(priority = 4)
@Component
@Log4j
public class SchemaValidationDetector extends Detector{


	@Autowired
	MapsHandler mapsHandler;

	@Autowired
	SchemaRepository schemaReop;

	@Autowired
	ServiceRepository servcieRepo;

	@Autowired
	AuditService auditService;

	@Autowired
	ServiceDependenciesRepositroy serviceDependencyRepo;

	private List<ServiceDependencies> serviceDepList        = new ArrayList<ServiceDependencies>();
	private Set<Schemas>              serviceSchemas        = new HashSet<Schemas>();
	private List<SchemaType>          schemas               = new ArrayList<SchemaType>();
	private final String              SCHEMATYPE_XSD        = "XSD";
	private final String              W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

	public Map<String, List<SchemaType>> schemaTypeMap      = new HashMap<>();

	//-----------------------------------------------------------------------------------------------------------------
	@Override
	public void init() {

		String ServiceName = "";
		List<Schemas> schemaList = schemaReop.findAll();
		List<Service> serviceList = servcieRepo.findAll();
		schemaTypeMap = mapsHandler.getSchemaTypeMap();

		for (Service services : serviceList) {
			
			
			ServiceName = services.getServiceName();
			ArrayList<SchemaType> schemaTypeList = new ArrayList<SchemaType>();
			
			// create the schemaType arrayList for each service;
			for (Schemas schema : schemaList) {
			
				if (schema.getServiceId() != 0 && schema.getServiceId() == services.getServiceId()) {
					SchemaType schemaType = new SchemaType(XSDType.getEnumBySynonym.apply(schema.getSchemaType() ).code, schema.getSchema());
					schemaTypeList.add(schemaType);
				}
			}
				List<SchemaType> sorted = schemaTypeList.stream().sorted((a,b)->a.getSeq().compareTo(b.getSeq())).collect(Collectors.toList());
				// load the schemaType arrayList on on the schemaTypeMap;
				if (schemaTypeList.size() > 0){
				   schemaTypeMap.put(ServiceName, sorted); //schemaTypeList);
				}
		}

	}
	//-----------------------------------------------------------------------------------------------------------------
	@Override
	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest) {

		boolean isValid = true;
		boolean didValidate = false;
		boolean inputIsJson = false;
		serviceSchemas = extendedService.getShemas();
		String serviceName = extendedService.getService().getServiceName();

		serviceDepList = extendedService.getServiceDependencies();
		for (ServiceDependencies serviceDependencies : serviceDepList) {
			
			// check whether dependency is enabled..
			int enabled = serviceDependencies.getEnabled();
			
			// For dependencies that require schema validation check (4), and match the message type (Request/Response)
			if (    serviceDependencies.getDependencyId() == DetectorTypes.SCHEMA_VALIDATION.code //4
				 && serviceDependencies.getMessageType().equals(messageType)
				 && enabled == 1) {
				 log.debug("Starting schema validation");
				 didValidate = true;

				 ContentTypes contentTypeEnum = ContentTypes.getEnumBySynonym.apply(extendedService.getService().getContentType());
					
				 MessageTypes messageTypeEnum = MessageTypes.getEnumBySynonym.apply(messageType);
				 
				 
				 if (contentTypeEnum.equals(ContentTypes.JSON) || contentTypeEnum.equals( ContentTypes.COMPOSITE)){
						inputIsJson = true;
						log.debug("contentType is JSON, inputIsJson = true");

				 }
//				   boolean  conversionRequired = false;
//					if (contentTypeEnum.equals( ContentTypes.COMPOSITE ) ) {
//						log.debug("ContentType is COMPOSITE, MessageType: [" +messageType+"]");
//						inputIsJson = true;
//						log.debug("inputIsJson = true");
//						switch( messageTypeEnum ){
//						case REQUEST:
//							log.debug("sourceRequestInp: ["+ extendedService.getServiceConversions().getSourceRequestInputType()+"]. destinationRequestInp: ["+extendedService.getServiceConversions().getDestinationRequestInputType()+"]");
//							if ( ContentTypes.requiresAnyConversion.test( extendedService.getServiceConversions().getSourceRequestInputType(), 
//									                                      extendedService.getServiceConversions().getDestinationRequestInputType() ) ) {
//								Utils.inititializeStrBuffToPaddedContent( content, messageType );
////								conversionRequired = true;
//							}
//							break;
//						case RESPONSE:
//							log.debug("sourceResponsetInp: ["+ extendedService.getServiceConversions().getDestinationResponseInputType()+"]. destinationResponseInp: ["+extendedService.getServiceConversions().getSourceResponseInputType()+"]");
//							if ( ContentTypes.requiresAnyConversion.test( extendedService.getServiceConversions().getDestinationResponseInputType(), 
//                                    									  extendedService.getServiceConversions().getSourceResponseInputType() ) ) {
//                                    Utils.inititializeStrBuffToPaddedContent( content, messageType );
////    								conversionRequired = true;
//                            }
//							break;
//						}//switch
//					}//COMPOSITE
						
					try {
						if (serviceSchemas.isEmpty()) {
							String error = "No schemas to validate against for " + serviceName;
							log.error(error);
							auditService.requestResponseBuildAudit(extendedService, content.toString(),error, id);
							isValid = false;
						} else {
							
							Source xmlSource = new StreamSource( new StringReader(content.toString()));

							schemas = mapsHandler.getSchemaTypeMap().get( serviceName);
							Source[] allSchemasSource = new Source[schemas.size()];

							for (int j = 0; j < schemas.size(); j++) {
//								System.out.println(schemas.get(j).getSeq());
//								System.out.println(schemas.get(j).getSchema().subSequence(70, 150));
								allSchemasSource[j] = new StreamSource( new StringReader(schemas.get(j).getSchema()) );
//								if (SCHEMATYPE_XSD.equals(schemas.get(j).getType())) {
//								}
							}

							SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
							Schema schema = factory.newSchema(allSchemasSource);
							Validator validator = schema.newValidator();
							validator.validate(xmlSource);
						}
					} catch (Exception e) {
						String error = "ERROR! - Cannot validate XML against its schemas: [" + e.getMessage() + "]";
						log.error(error, e);
						auditService.requestResponseBuildAudit(extendedService, content.toString(), error, id);
						isValid = false;
					}

					// If basic validation succeeded than check for extra
					// validation.
					// The schemas are not written correctly (have referenced
					// elements) which passes inner elements as valid.
					// So we're just gonna check what is the root element in the
					// xml (and its ns) and same with the top xsd and compare
					// those 2.
					if (isValid) {

						try {
							isValid = false;

							// Notice that parsing errors may occur
							String xsdLower = schemas.get(schemas.size() - 1)
									.getSchema().toLowerCase();
							String xmlLower = content.toString().toLowerCase();
							xmlLower = xmlLower.replace("![cdata[", "![CDATA[");
							Document docXSD = Utils.buildXML( xsdLower );
							Document docXML = Utils.buildXML( xmlLower );

							// Let's strip down the XSD and get what we want
							ArrayList<String> listXsdElementNames = new ArrayList<String>();
							Element xsdDocElement = docXSD.getDocumentElement();
							NodeList listXsdElements = xsdDocElement.getChildNodes();
							String xsdNs = "";
							
							log.debug("Checking 'targetnamespace' attribute. This attribute is checked only for non json input.");
						if (!inputIsJson) {
							log.debug("input type is not json - checking targetnamespace");
							 xsdNs = xsdDocElement.getAttribute("targetnamespace");
							if (xsdNs == null || ("").equals(xsdNs)) {
								throw new Exception("Top XSD not formatted correctly. Cannot find attribute targetnamespace");
							}
							log.debug("successfully found targetnamespace");
						} else {
							log.debug("inputIsJson is true - skipping targetnamespace check");
						}
							// Check all child elements of the doc element
							for (int i = 0; i < listXsdElements.getLength(); i++) {

								Node node = listXsdElements.item(i);

								if (node.getNodeName().contains("element")) {
									listXsdElementNames.add(((Element) node).getAttribute("name"));
								}
							}

							// What about the XML ha? don't forget about it 
							Element xmlDocElement = docXML.getDocumentElement();
							String xmlNS = "";
							String xmlRootName = xmlDocElement.getNodeName();

							// Let's compare XML with XSD

							// Default ns in xml
							if (!xmlRootName.contains(":")) {

								xmlNS = xmlDocElement.getAttribute("xmlns");
								xmlNS = xmlNS == null ? "" : xmlNS;

								// No namespace for neither
								if (("").equals(xsdNs) && ("").equals(xmlNS)) {

									// Look if xml's root element is in the
									// xsd's list of elements
									for (int j = 0; (j < listXsdElementNames
											.size()) && (!isValid); j++) {
										if (listXsdElementNames.get(j).equals(
												xmlRootName)) {
											isValid = true;
										}
									}

								} else if ((xsdNs).equals(xmlNS)) {

									// Look if xml's root element is in the
									// xsd's list of elements
									for (int j = 0; (j < listXsdElementNames
											.size()) && (!isValid); j++) {
										if (listXsdElementNames.get(j).equals(
												xmlRootName)) {
											isValid = true;
										}
									}
								}

							} else {

								String shortNS = xmlRootName.substring(0,
										xmlRootName.indexOf(":"));
								xmlNS = xmlDocElement.getAttribute("xmlns:"
										+ shortNS);
								xmlRootName = xmlRootName.substring(xmlRootName
										.indexOf(":") + 1);

								// has to be namespace in xsd
								if ((xsdNs).equals(xmlNS)) {

									// Look if xml's root element is in the
									// xsd's list of elements
									for (int j = 0; (j < listXsdElementNames
											.size()) && (!isValid); j++) {
										if (listXsdElementNames.get(j).equals(
												xmlRootName)) {
											isValid = true;
										}
									}
								}
							}

						if (!isValid) {
							throw new Exception("XML root element (" + xmlNS + ":" + xmlRootName+ ") is not an element in the top schema");
						}

						} catch (SAXParseException se) {
							String error = ("Cannot validate XML against its schemas. XML not in correct XML format: " + se
									.getMessage());
							log.error(error);
							auditService.requestResponseBuildAudit(extendedService, content.toString(),
									error, id);
						} catch (Exception e) {
							String error = "Cannot validate XML against its schemas: "
									+ e.getMessage();
							log.error(error);
							auditService.requestResponseBuildAudit(extendedService, content.toString(),
									error, id);
						}
					}
			}//if
		}//for
		if (isValid && didValidate) {
			log.debug("successfully validated Schemas for [" + messageType
					+ "] message");
		}
		return isValid;
	}

	//-----------------------------------------------------------------------------------------------------------------
//	public int getNumOfXSDs(String serviceName, List<SchemaType> schemas) {
//
//		int num = 0;
//
//		for (int i = 0; i < schemas.size(); i++) {
//			if (SCHEMATYPE_XSD.equals(schemas.get(i).getType())) {
//				num++;
//			}
//		}
//
//		return num;
//	}
	//-----------------------------------------------------------------------------------------------------------------
}
