package stempler.ofer.service.impl;

import il.co.boj.wsConnector.service.WsConnectorService;
import lombok.extern.log4j.Log4j;
import org.apache.http.Header;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import stempler.ofer.configuration.LdpConfig;
import stempler.ofer.detectors.*;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.LdpResponse;
import stempler.ofer.model.LdpResponseExtended;
import stempler.ofer.model.ValidationResponse;
import stempler.ofer.model.entities.Service;
import stempler.ofer.model.entities.ServiceConversions;
import stempler.ofer.model.enums.ContentTypes;
import stempler.ofer.model.enums.DestinationTypes;
import stempler.ofer.model.enums.MessageTypes;
import stempler.ofer.service.AuditService;
import stempler.ofer.service.RequestResponseService;
import stempler.ofer.service.RestForwardRequestsService;
import stempler.ofer.utils.Utils;
import stempler.ofer.utils.XmlToJson;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@Log4j
public class RequestResponseServiceImpl implements RequestResponseService {
	
	@Autowired
	XmlToJson xmlToJson;
	
	@Autowired
	LdpConfig ldpconfig;
	
	@Autowired
	MapsHandler mapHandler;
	
	@Autowired
	DetectorListHandler detectorListHandler;
	
	@Autowired
	DetectorServiceImpl detectoreService;

	@Autowired
	ServiceLoader serviceLoader;

	@Autowired
	MQActionServiceImpl mqActions;

	@Autowired
	RestForwardRequestsService restForwardRequestsService;

	@Autowired
	AuditService auditService;

	@Autowired
	WsConnectorService wsConnectorService;
	
	private final String CLIENT_IP = "ClientIP";
	private final String TAG_CLIENTIP = "ClientIP";
	private final String MESSAGE_TYPE_REQUEST = "Request";
	private final String MESSAGE_TYPE_RESPONSE = "Response";
	private final static String SOAP_SUFFIX = "</soap:Body></soap:Envelope>";
	private final static String SOAP_PREFIX = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><soap:Body>";	

//-------------------------------------------------------------------------------------------------------
	private LdpResponse handleErrorResponseAndAudit(String guid, String errMessage, ExtendedService extendedService){
		return handleErrorResponseAndAudit(guid, errMessage, extendedService, null/*exception*/, null/*requestContent*/);
	}
	//-------------------------------------------------------------------------------------------------------
	private LdpResponse handleErrorResponseAndAudit(String guid, String errMessage, ExtendedService extendedService, Exception exception, String requestContent){
     String LDPError = "LDP-NG vialition number: " + guid;
	 LdpResponse ldpResponse = new LdpResponse();

	 if ( exception == null ){
		 log.error(errMessage);
	 }else{
		 log.error(errMessage, exception);
	 }
   	 auditService.requestResponseBuildAudit(extendedService, requestContent, errMessage, guid);
   	 ldpResponse.setResponseCode(HttpStatus.CONFLICT);
   	 ldpResponse.setResponseMessage(LDPError);
   	
	 return ldpResponse;
	}
	//-------------------------------------------------------------------------------------------------------
	public LdpResponse startValidation(HttpServletRequest request, HttpServletResponse response,  MultipartFile[] attachmentFiles, String messageBody){
			
		     long startTime = System.currentTimeMillis();
//		     1) generate a guid for the request
		     String             guid              = Utils.StringGenerator();
		     String             contentType = "";   
		     boolean            isValid           = false;
		     LdpResponse        ldpResponse       = new LdpResponse();
	    	 String             requestContent    = null;
	    	 ContentTypes       contentTypeEnum   = null;

//		     2) validate parameters
	    	String errorMsg = validateInitialParamters(request);
	    	 if (errorMsg != null){
	    	   return handleErrorResponseAndAudit(guid, errorMsg, null);	 
	    	 }
	    	 String uri                               = request.getRequestURI();
	    	 Map<String, ExtendedService> servicesMap = mapHandler.getServiceUriMap();
	    	 ExtendedService extendedService          = servicesMap.get(uri);
	    	 stempler.ofer.model.entities.Service service = extendedService.getService();
		     String serviceName                       = service.getServiceName();

	    	 log.debug("uri from Request: [" +uri+ "]");
		     log.debug("Service: [" + serviceName + "] has been found." );
//		     3) get message content
		     //--------------
		     boolean getContentFromRequest = checkContentTypeAndAttachments(request, attachmentFiles);
		     if(getContentFromRequest) {
		    	 // Get content from HttpServletRequest
			 requestContent = getMessageContent(request, guid);
		     } else {
		    	 // Get content from form-data field
		    	 requestContent = messageBody; 
		     }
		     //set attachment files to extended service
		     if(attachmentFiles != null && attachmentFiles.length > 0 ){
//		    	 extendedService.setAttachmentFiles(attachmentFiles);
		    	 //If ever needed, send attachmentFiles to perform()
		    	 log.debug("Found [" +attachmentFiles.length+"] files attached to this request");
		     }
		     //--------------
		     
		     //4) Get ContentType as Enum
		     try {
//				origContent = new String( (requestContent != null ? requestContent.getBytes() : "".getBytes()), "UTF-8");
				contentType = service.getContentType();
			    log.debug("contentType string:[" + contentType + "], converting to ContentTypes enum. " );
				if ( contentType != null ) {
					contentTypeEnum = ContentTypes.getEnumBySynonym.apply( contentType );
				}
			    log.debug("contentType as ENUM:[" + contentTypeEnum + "] " );
			} catch (Exception e) {
				throw new RuntimeException("Service: [" + serviceName + "] got Exception " + e.getMessage());
			}
		     if ( requestContent == null || requestContent.equals("") || requestContent.length() == 0 ){
		    	return handleErrorResponseAndAudit(guid, "Request Content is empty", extendedService);
		     }
		     
//		     5) check if service is enabled.. 
			  boolean isEnabled = this.isEnabled(extendedService, guid);
			  if (!isEnabled) {
				return handleErrorResponseAndAudit(guid, "Service is not enabled", extendedService);
			  }
		     
//		     6) check if XML with no SOAP, XML with SOAP or Json (SOAP | XMLNOSOAP | JSON). Then validate structure
			  contentType = (contentType == null || contentType.length() == 0 ? extendedService.getService().getContentType() : contentType );  
			  ValidationResponse contentTypeValidationResponse = validateContentType(requestContent,  contentType,  serviceName,  guid, MESSAGE_TYPE_REQUEST, extendedService.getServiceConversions());
		  if (!contentTypeValidationResponse.isValid()) {
		     return handleErrorResponseAndAudit(guid, contentTypeValidationResponse.getContnet(), extendedService);  
		  }
		  requestContent= contentTypeValidationResponse.getContnet(); // at this stage - requestContent is XML 
		  
//		  7) Determine which IP to forward - Get client ip or x-forwarded-for - insert into XML <TAG_CLIENTIP> field .. 
		  requestContent = this.forwardClientIP(requestContent, service, request, guid);
		  if (requestContent == null) {
			return handleErrorResponseAndAudit(guid, "Could not get IP from request or from remote", extendedService);
		  }
		  
//		 8) add Root element if message is json JSON 
		  StringBuffer requestContentStrBuff = new StringBuffer(requestContent);
		  AddRootElement(extendedService, MessageTypes.REQUEST, requestContentStrBuff);
		  
//		  9) Validate request...check detectors via perform()
		  log.debug("Starting detectorsService.perform() validations for request");
		  isValid = detectoreService.perform(extendedService, requestContentStrBuff, MESSAGE_TYPE_REQUEST, guid, request );
		  if (!isValid) {
			return handleErrorResponseAndAudit(guid, "Validation has failed..", extendedService);
		  }
		  requestContent = requestContentStrBuff.toString(); // in case StrBuff has changed - retrieve back content 
		  
//		  10) Check for COMPOSITE content Type
		  requestContent = handleCompositeContentType(extendedService, requestContentStrBuff, MESSAGE_TYPE_REQUEST, contentTypeEnum);
		  
//		  10) handle destination:
		  ldpResponse = sendToDestination(extendedService, guid, requestContent, /*origContent,*/request,  response);
		  
		  if (ldpResponse ==null || !(ldpResponse instanceof LdpResponseExtended)) {
			  return handleErrorResponseAndAudit(guid, ldpResponse.getResponseMessage(), extendedService);
			}
		  
		  //11) handle response
		  ldpResponse = handleResponse(extendedService, ldpResponse, guid, contentTypeEnum);
		  if (ldpResponse ==null || !(ldpResponse instanceof LdpResponseExtended)) {
			  return handleErrorResponseAndAudit(guid, ldpResponse.getResponseMessage(), extendedService);
			}
		  	
		  //12) return success to client
			long endTime= System.currentTimeMillis();
			ldpResponse.setResponseMessage("Success");
			 auditService.requestResponseBuildAudit(extendedService, requestContent, "", guid);
			log.debug("Total elapsed time = [" + (endTime- startTime) + "] ms");
			return ldpResponse; // ldpResponse;
	}
//---------------------------------------------------------------------------------------------------------------------
	private String handleCompositeContentType(ExtendedService extendedService, StringBuffer content, String messageType, ContentTypes contentType) {
		
		String requestContent                 = content.toString();
		
		if (contentType.equals(ContentTypes.COMPOSITE)){
		log.debug("Content type is COMPOSITE. Handleing content for messageType [" + messageType + "]");
		ContentTypes contentTypeEnum          = null;
		String convertingTo                   = null;
		ServiceConversions serviceConversions = extendedService.getServiceConversions();
		boolean validConversions = validateConversions (serviceConversions);
		if (validConversions){
		try {	
			
			// get the input type for by messageType -Req or Res
			if (serviceConversions != null && messageType.equals(MESSAGE_TYPE_REQUEST)) {
				contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceConversions.getSourceRequestInputType());
				convertingTo = serviceConversions.getDestinationRequestInputType();
			} else {
				contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceConversions.getDestinationResponseInputType());
				convertingTo = serviceConversions.getSourceResponseInputType();
			}
			// converting Json to xml or vice a versa
			log.debug(" messageType is ["+messageType+"]: converting from ["+contentTypeEnum+"], to -> ["+convertingTo+"]");
			if (contentTypeEnum.equals(ContentTypes.JSON)) {
				log.debug("Content was already parsed to XML, doing nothing.");
//			JSONObject jsonObject = new JSONObject(requestContent);
//			requestContent = XML.toString(jsonObject);
			} else {
//			log.debug("Converting from XML to JSON");
				JSONObject json = XML.toJSONObject(requestContent);
				log.debug("Successfully converted content");
				if(!json.has("Response")) {
					throw new Exception("Json tag 'Response' does not exists in esb response. Check that esb response is valid. esb response: [" +requestContent+"]");
				}
				log.debug("Removing xmlns:soapenv tag from json Response");
					json.getJSONObject("Response").remove("xmlns:soapenv");
					requestContent = json.toString();
					log.debug("Successfully removed tag");

					// requestContent =
					// XML.toJSONObject(requestContent).toString();
				}
				return requestContent;
		
			} catch (Exception e) {
				log.error("Could not handle conversions for content Type - COMPOSITE: " + e);
				return null;
			}
		}
		}
		return requestContent;
	}
	//-----------------------------------------------------------------------------------------------------------		

	private boolean validateConversions(ServiceConversions serviceConversions) {
		
		if (serviceConversions.getDestinationRequestInputType().equals(serviceConversions.getDestinationResponseInputType()) &&
			serviceConversions.getDestinationRequestInputType().equals(serviceConversions.getSourceRequestInputType()) &&
			serviceConversions.getDestinationRequestInputType().equals(serviceConversions.getSourceResponseInputType())) {
			
			log.error("All COMPOSITE Content Types are same: [" +serviceConversions.getDestinationRequestInputType()+ "]. This case should be avoided in the UI level."
					+ "Ignoring and doing nothing.");
			return false;
		}
		
		
		return true;
	}
	//-----------------------------------------------------------------------------------------------------------		
    private void AddRootElement(ExtendedService extendedService, MessageTypes messageTypeEnum, StringBuffer content) {
    	 ContentTypes contentTypeEnum = ContentTypes.getEnumBySynonym.apply(extendedService.getService().getContentType());

    	 if(contentTypeEnum.equals( ContentTypes.JSON )){
    		 Utils.inititializeStrBuffToPaddedContent( content, messageTypeEnum.synonym ); 
    		 return;
    	 }
    	 
		if (contentTypeEnum.equals( ContentTypes.COMPOSITE ) ) {
		log.debug("ContentType is COMPOSITE, MessageType: [" +messageTypeEnum.toString()+"]");
//		inputIsJson = true;
//		log.debug("inputIsJson = true");
		switch( messageTypeEnum ){
		case REQUEST:
			log.debug("sourceRequestInp: ["+ extendedService.getServiceConversions().getSourceRequestInputType()+"]. destinationRequestInp: ["+extendedService.getServiceConversions().getDestinationRequestInputType()+"]");
			if ( ContentTypes.requiresAnyConversion.test( extendedService.getServiceConversions().getSourceRequestInputType(), 
					                                      extendedService.getServiceConversions().getDestinationRequestInputType() ) ) {
				Utils.inititializeStrBuffToPaddedContent( content, messageTypeEnum.synonym );
//				conversionRequired = true;
			}
			break;
		case RESPONSE:
			log.debug("sourceResponsetInp: ["+ extendedService.getServiceConversions().getDestinationResponseInputType()+"]. destinationResponseInp: ["+extendedService.getServiceConversions().getSourceResponseInputType()+"]");
			if ( ContentTypes.requiresAnyConversion.test( extendedService.getServiceConversions().getDestinationResponseInputType(), 
                    									  extendedService.getServiceConversions().getSourceResponseInputType() ) ) {
                    Utils.inititializeStrBuffToPaddedContent( content, messageTypeEnum.synonym );
//					conversionRequired = true;
            }
			break;
		}//switch
	}//COMPOSITE
		
	}
	//-----------------------------------------------------------------------------------------------------------		
	private boolean checkContentTypeAndAttachments(HttpServletRequest request, MultipartFile[] attachmentFiles) {
		String contentType = request.getContentType();
		log.debug("Content Type: [" +contentType+"]");
		if (contentType.contains("application/json") || contentType.contains("application/xml") ||contentType.contains("text/xml") ||contentType.contains("text/plain")){
			log.debug("Getting request body from HttpServletRequest");
			return true;
		} else {
			log.debug("Getting request body from String messageBody");
			return false;
		}
	}
   //-----------------------------------------------------------------------------------------------------------
	private String validateInitialParamters(HttpServletRequest request) {

		
		String uri = request.getRequestURI();
		if (uri == null) {
			String msg = "FATAL! URI from Request is null, returning failure.";
			log.error(msg);
			return msg;
		}

		Map<String, ExtendedService> servicesMap = mapHandler.getServiceUriMap();
		if (servicesMap == null) {
			String msg = "FATAL! Ldp system failure. servicesMap is null - should not occur. Returning failure";
			log.error(msg);
			return msg;
		}

		ExtendedService extendedService = servicesMap.get(uri);
		if (extendedService == null) {
			String msg = "No Extended Service found for uri:[" + uri + "]. Returning failure";
			log.debug(msg);
			return msg;
		}
	Service service = extendedService.getService(); // .getServiceName();
		if (service == null) {
			String msg = "No service found for uri:[" + uri + "]. Returning failure";
			log.debug(msg);
			return msg;
		}

		String serviceName = service.getServiceName();
		if (serviceName == null || serviceName.equals("")) {
			String msg = "Service name is null or empty. Returning failure ";
			log.debug(msg);
			return msg;
		}
		log.debug("Successfully validated initial parameters");
		return null;
	}
	//-----------------------------------------------------------------------------------------------------------
	private LdpResponse handleResponse(ExtendedService extendedService, LdpResponse ldpResponse, String guid, ContentTypes contentTypeEnum ) {
		
		String destinationTypeStr 			 	= extendedService.getService().getDestinationType();
		DestinationTypes destinationTypeEnum 	= DestinationTypes.getEnumBySynonym.apply(destinationTypeStr);
		LdpResponseExtended ldpExtendedResponse = new LdpResponseExtended();
		ldpExtendedResponse 					= (LdpResponseExtended) ldpResponse;
		ldpResponse 							= new LdpResponse();
		
		log.debug("Starting Response handling");
		StringBuffer replyStringStrBuff = new StringBuffer(ldpExtendedResponse.getContent());
		
		//remove SOAP envelope for WS Responses before sending to perform()
		if (destinationTypeEnum.equals(DestinationTypes.WS)){
			try{
			log.debug("DestinationType is WS - Removing SOAP envelope");
			String replyNoSoap = Utils.retrieveSoapBody(ldpExtendedResponse.getContent());
			replyStringStrBuff.setLength(0);
			replyStringStrBuff.append(replyNoSoap);
			log.debug("Successfully removed SOAP evelope and appended to reply String buffer");
			}catch (Exception e) {
				String error = "Failed removing SOAP envelope. " + e;
				log.error(error);
				ldpResponse.setResponseCode(HttpStatus.CONFLICT);
				ldpResponse.setResponseMessage(error);
				return ldpResponse;	
			}

		}
		log.debug("Starting detectorsService.perform() validations for RESPONSE");
		boolean isValid = detectoreService.perform(extendedService, replyStringStrBuff, MESSAGE_TYPE_RESPONSE, guid, null);
		String replyString = replyStringStrBuff.toString();

		if (!isValid) {
			String error = "Validation failed for meesageType REPLY from ["+ extendedService.getService().getDestination() + "]";
			log.error(error);
			ldpResponse.setResponseCode(HttpStatus.CONFLICT);
			ldpResponse.setResponseMessage(error);
			return ldpResponse;
		}
		
		replyString = handleCompositeContentType(extendedService, replyStringStrBuff, MESSAGE_TYPE_RESPONSE, contentTypeEnum);
		//Respone will return as JSON if ContentTypes wanted is Json.
		switch (contentTypeEnum) {
		case COMPOSITE:
			if (extendedService.getServiceConversions().getDestinationResponseInputType().equals(ContentTypes.XMLNOSOAP) &&
					extendedService.getServiceConversions().getSourceResponseInputType().equals(ContentTypes.JSON)	){
				replyString = (XML.toJSONObject(replyString)).toString();
			}
			break;
		
		case JSON:
			if (extendedService.getService().getContentType().equals(String.valueOf(ContentTypes.JSON))){
				replyString = (XML.toJSONObject(replyString)).toString();	
			}
			break;
		}
		
		switch (destinationTypeEnum) {
		case MQ:
		case WS:	
			if (contentTypeEnum.equals(ContentTypes.SOAP)){
			log.debug("Appending SOAP envelope before returning response.");
			replyString = addSoapPrefixAndSuffix(replyString);
			log.debug("Successfully appended SOAP envelope to response.");
			} else {
				String error = "Can not add SOAP envelope to ContentType other than SOAP.  Make sure Content type is SOAP for MQ or WS destinations. "
						+ "Current contentType: [" +contentTypeEnum+ "]";
				log.error(error);
				ldpResponse.setResponseCode(HttpStatus.CONFLICT);
				ldpResponse.setResponseMessage(error);
				return ldpResponse;		
			}
			break;
		}
		
		ldpExtendedResponse.setContent(replyString);
		log.debug("Successfully validated reply: [" + replyString + "]");

		return ldpExtendedResponse;
	}
	//--------------------------------------------------------------------------------------------------------
		private String addSoapPrefixAndSuffix(String replyString) {
			int xmlPreLoc = replyString.indexOf("<?"); // we're building our own header
			if (xmlPreLoc != -1) {
				int xmlSufLoc = replyString.indexOf(">", xmlPreLoc);
				replyString = replyString.substring(xmlSufLoc + 1);
			}
			replyString = SOAP_PREFIX + replyString + SOAP_SUFFIX;
		return replyString;
	}
	//-----------------------------------------------------------------------------------------------------------
	private LdpResponse sendToDestination(ExtendedService extendedService, String guid, String requestContent,/*String origContent, */HttpServletRequest request, HttpServletResponse response) {
		LdpResponse  ldpResponse                 = new LdpResponse();
		Service service = extendedService.getService();
		String serviceName                       = extendedService.getService().getServiceName();
		ContentTypes contentTypeEnum             = ContentTypes.getEnumBySynonym.apply(extendedService.getService().getContentType());
		
		if (service.getDestinationType() == null) {
			String errorMsg = "No destination type is defined for this service";
			return handleErrorResponseAndAudit(guid, errorMsg, extendedService);
		}
		
		String destinationTypeStr = service.getDestinationType();
		DestinationTypes destinationTypeEnum = DestinationTypes.getEnumBySynonym.apply(destinationTypeStr);
		if (contentTypeEnum.equals( ContentTypes.COMPOSITE ) ) {
			if (extendedService.getServiceConversions().getSourceRequestInputType().equals("JSON") && 
				extendedService.getServiceConversions().getDestinationRequestInputType().equals("XMLNOSOAP")) {
				log.debug("Checking if Request containes root elements before sendting to destination");
				requestContent = Utils.addTagsToContent(requestContent, MESSAGE_TYPE_REQUEST);
			}
		}
			
		if (contentTypeEnum.equals(ContentTypes.JSON)){
		   log.debug("JSON contentType - setting requestContent back to origContent to preserve JSON content before converted to XML");
//		   parse to Json
		   requestContent = (XML.toJSONObject(requestContent.toString())).toString();
		}
		switch (destinationTypeEnum) {
		  case MQ:
			
			log.debug("Starting MQ transition");
			try {
				return mqActions.sendRequestToMQ(extendedService, requestContent, guid);
			} catch (Exception e) {
				return handleErrorResponseAndAudit(guid, "Could not send to MQ, Exception:[" + e.getMessage() + "]", extendedService, e, requestContent);
			}
			
		  case WS:
			log.debug("Starting WS transition");
			//building the SOAP envelope
			requestContent = addSoapPrefixAndSuffix(requestContent);
			String targetUrl = extendedService.getService().getDestination();
			if ( targetUrl == null || targetUrl.length() == 0 ){
				throw new RuntimeException("FATAL !: targetUrl is null in service:[" + serviceName + "]. Check service DB configuration Service.Destination");
			}
			log.debug("WS targetUrl:[" + targetUrl + "]");
			
//			switch( contentTypeEnum ){
//			  case JSON: // if contentType = JSON, preserve origin content backup. 
//					log.debug("JSON contentType - setting requestContent back to origContent to preserve JSON content before converted to XML");
//		            requestContent = origContent; // origContent is the Original (JSON) content before transfered to XML in clause 4) validateContentType()
//		            break;
//			  case SOAP:
//			  case XMLNOSOAP:
//		            break; // do nothing
//		      default: 
//			}//switch
			 il.co.boj.wsConnector.model.LdpResponse ldp_Response = wsConnectorService.doAction(targetUrl , request.getScheme(), request, response, requestContent/*origContent*/); //requestContent);
			 if (ldp_Response instanceof il.co.boj.wsConnector.model.LdpResponseExtended) {
//				 HttpHeaders httpHeaders = new HttpHeaders();
				LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<String , String>();

				 if ( ((il.co.boj.wsConnector.model.LdpResponseExtended)ldp_Response).getHeaders() != null ){
					 for ( Header header : ((il.co.boj.wsConnector.model.LdpResponseExtended)ldp_Response).getHeaders() ){
						 if(header.getName().equals("Content-Length")) {
							 continue;
						 } else {
						 headers.add(header.getName(), header.getValue() );
						 }
					 }
					 
				 }
				 ldpResponse = new LdpResponseExtended(((il.co.boj.wsConnector.model.LdpResponseExtended) ldp_Response).getHttpStatus(), 
						                               ldp_Response.getResponseMessage(), 
						                               ((il.co.boj.wsConnector.model.LdpResponseExtended) ldp_Response).getContent(), headers );
				 return ldpResponse;
			 } else {
			  return handleErrorResponseAndAudit(guid, "wsConnector did not responded with instance of il.co.boj.wsConnector.model.LdpResponseExtended", extendedService,null, requestContent);

			 }					
		  case REST:
			log.debug("Starting REST transition");
			try {
				return restForwardRequestsService.restForwardRequest(extendedService, requestContent, guid, request, response, request.getMethod());
			} catch (Exception e) {
				log.error("Could not send to destination ", e);
				return handleErrorResponseAndAudit(guid, "Could not send to destination, Exception:[" + e.getMessage() + "]", extendedService, e, requestContent);
			}
			// break;
		}// switch
		return null;
	}
 
//---------------------------------------------------------------------------------------------------------
//	private String convertServiceContent(String requestContent, ServiceConversions serviceSourceConversions, String messageType) throws Exception {
//		ContentTypes contentTypeEnum = null;
//		// get the input type for by messageType -Req or Res
//		if (messageType.equals(MESSAGE_TYPE_REQUEST)){
//			contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceSourceConversions.getSourceRequestInputType());	
//		} else {
//			contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceSourceConversions.getDestinationResponseInputType());	
//		}
//		// converting Json to xml or vice a versa		
//				if(contentTypeEnum.equals(ContentTypes.JSON)){
//					JSONObject jsonObject = new JSONObject(requestContent);
//					requestContent = XML.toString(jsonObject);
//				} else {
//					requestContent = XML.toJSONObject(requestContent).toString();					
//				}
//		return requestContent;
//	}
	//---------------------------------------------------------------------------------------------------------
	/**
	 * called by MainController.doGET() - TBD
	 */
	@Override
	public LdpResponse getParameters(HttpServletRequest request, HttpServletResponse response){
		 boolean isValid         		= false;
		 String  serviceName     		= "";
		 long startTime         		= System.currentTimeMillis();
	     String guid             		= Utils.StringGenerator();
	     String contentType     		= "";
	     ContentTypes contentTypeEnum   = null;
	     LdpResponse ldpResponse 		= new LdpResponse();
    	 String       httpMethod        = request.getMethod();

	     
	     ExtendedService extendedService = new ExtendedService();
	     
	     //get Uri
	     String uri = request.getRequestURI();
	     if (uri != null)
	    	 log.debug("uri from Request: [" +uri+ "]");
	   //get ExtendedService
	     try{
	    	 extendedService = mapHandler.getServiceUriMap().get(uri);
	    	 serviceName = extendedService.getService().getServiceName();
	    	 if (serviceName == null){
	    	 }}catch (Exception e){
	    		 return handleErrorResponseAndAudit(guid, "could not get service by uri:[" +uri+ "] Uri is not in db: " + e.getMessage(), extendedService, e, null);
	    	 }
	     //get service
	     Service service = extendedService.getService(); //.getServiceName();
	     if ( service == null ){
	    	 String msg = "No service found for uri:[" + uri + "].";
	    	 log.debug(msg);
	    	return handleErrorResponseAndAudit(guid, msg, extendedService);
	     }
	     //get serviceName
	     serviceName = service.getServiceName();
	     if (serviceName == null){
	    	return handleErrorResponseAndAudit(guid, "Could not get service by uri:[" + uri + "]. Check db configuration", extendedService);
	     }
	     log.debug("Service: [" + serviceName + "] has been found." );
	   
	  // check if service is enabled
			boolean isEnabled = this.isEnabled(extendedService, guid);
			if (!isEnabled) {
	    		 return handleErrorResponseAndAudit(guid, "Service uri:[" +uri+ "] is disabled", extendedService);
			}
	     
		// //get parameters
		String urlParameters = request.getQueryString();
		if (urlParameters != null){
			extendedService.setParametersMap(request.getParameterMap()); 
		}

		//get contentType
		   contentType = service.getContentType() != null ? service.getContentType().toUpperCase() : ""; 	     
		if (contentType != null) {
			contentTypeEnum = ContentTypes.getEnumBySynonym.apply(contentType);
		}
		// Validate request
		isValid = detectoreService.perform(extendedService, null, MESSAGE_TYPE_REQUEST, guid, request);
		if (!isValid) {
   		    return handleErrorResponseAndAudit(guid, "Service uri:[" +uri+ "] has failed validation", extendedService);
		}
		
		// Destination to forward the message: MQ or URL

		if (extendedService.getService() == null || extendedService.getService().getDestinationType() == null) {
		     String error = "No destinationType is defined for this service";
   		     return handleErrorResponseAndAudit(guid, error, extendedService);
		}
		// !!!!! No GET messages handling for MQ at the moment !!!!! this is why this next block is out
		//if (extendedService.getService().getDestination().equals("MQ")) {
//		if ( DestinationTypes.MQ.equals( DestinationTypes.getEnumBySynonym.apply(extendedService.getService().getDestinationType() ) ) ) {	
//			log.debug("Starting MQ transition");
//			try {
//				mqActions.sendRequestToMQ(extendedService, requestContent, response, guid);
//			} catch (Exception e) {
//  		        return handleErrorResponseAndAudit(guid, "Service uri:[" +uri + "] Failed. Could not send to MQ. Exception:[" + e.getMessage() + "]", extendedService, e, requestContent);
//			}
//			
//		} else {
			try {
				ldpResponse = restForwardRequestsService.restForwardRequest(extendedService, urlParameters, guid, request, response, httpMethod);
					if ( ldpResponse instanceof LdpResponseExtended){
			    	 long endTime= System.currentTimeMillis();
			    	 log.debug("total time = [" + (endTime- startTime) + "] ms");
			    	 return ldpResponse;
	    		} else {
	    			 return handleErrorResponseAndAudit(guid, ldpResponse.getResponseMessage(), extendedService);
	    		}
				
		} catch (Exception e) {
			log.error("error makeing get request. " + e.getMessage(), e);
		}
		long endTime = System.currentTimeMillis();
		log.debug("total time = [" + (endTime - startTime) + "] ms");
		return ldpResponse;
	}
//---------------------------------------------------------------------------------------------------------
	@Override
	public String getMessageContent(HttpServletRequest request, String guid) {
	
		String requestContent = null;
		
		//   Retrieve the input data 
		try {

			ServletInputStream sis = request.getInputStream();

			BufferedReader b = new BufferedReader(new InputStreamReader(sis));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = b.readLine()) != null) {
				builder.append(line);
			}
			requestContent = builder.toString();
//			System.out.println(requestContent);
			sis.close();
		} catch (Exception e) {
			String error = "Unable to acquire request input data" + e.getMessage();
			log.error(error);
			auditService.requestResponseBuildAudit(null, requestContent, error, guid);
			return null;
		}
		return requestContent;
	}
	//---------------------------------------------------------------------------------------------------------
	@Override
	public String getXmlNoSOAP(String message, String serviceName, String guid) {

		String xml = message;
        try {
            xml = Utils.retrieveSoapBody(xml);
        } catch (StringIndexOutOfBoundsException e) {
        	String error = "problem with removing SOAP for service [" + serviceName+"] : "+e.getMessage();
        	log.error(error);
        	auditService.requestResponseBuildAudit(null, null, error, guid);		
        	
        	return null;
        }
		return xml;
	}
//---------------------------------------------------------------------------------------------------------
	public ValidationResponse validateContentType(String content, String contentType, String serviceName, String guid, String messageType, ServiceConversions serviceSourceConversions ) {
		contentType          = contentType != null ? contentType.toUpperCase() : ""; 
		String firstChar     = content.trim().substring(0, 1);
		String returnMessage = "";
		boolean isValid		 = true;
		ValidationResponse contentTypeValidationResponse = new ValidationResponse();
		
		if (contentType != null && firstChar != null) {
			ContentTypes contentTypeEnum = ContentTypes.getEnumBySynonym.apply(contentType);
			
			if (contentTypeEnum.equals(ContentTypes.COMPOSITE)){
				log.debug("For contentType = COMPOSITE, Getting new contentType from serviceConversion. For messageType [" +messageType+"]");
				if (messageType.equals(MESSAGE_TYPE_REQUEST)){
					 contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceSourceConversions.getSourceRequestInputType());	
					} else {
					 contentTypeEnum = ContentTypes.getEnumBySynonym.apply(serviceSourceConversions.getDestinationResponseInputType());	
					}
				log.debug("contentType from service Conversion is: ["+contentTypeEnum+"]");
			}
			switch (contentTypeEnum) {
								
			case SOAP: // COMMUNICATION_TYPE_SOAP: validate and remove Soap// envelope.
				if (firstChar.equals("<")) {
					log.debug("Removing SOAP envelope");
					content = this.getXmlNoSOAP(content, serviceName, guid);
					log.debug("Successfully removed SOAP envelope");
					if (content == null) {
						returnMessage =  "Could not retrieve SOAP body";
						isValid= false;
					}
					break;
				} else {
					returnMessage =  "contentType of type SOAP does not start with '<' and is not a valid xml. message first char is: "+ "[" + firstChar + "]";
					isValid= false;

				}
			case XMLNOSOAP: // COMMUNICATION_TYPE_XmlNoSoap: validate
				if (firstChar.equals("<")) {
					break;
				} else {
					returnMessage = "contentType of type XMLNoSOAP does not start with '<' and is not a valid xml. message first char is: ["+ firstChar + "]";
					isValid= false;
					break;
				}
			case JSON: // COMMUNICATION_TYPE_JSON: validate and parse to XML
				if (firstChar.equals("{")) {
					boolean isJson = Utils.isJSONValid(content);
					if (!isJson) {
						returnMessage = "Json is not valid";
						isValid= false;
						break;
					} else {
						log.debug("Parsing valid Json to XML");
						JSONObject o = new JSONObject(content);
						content = XML.toString(o);
						break;
					}
				} else {
					isValid= false;
					returnMessage = "contentType of type JSON does not start with '{' and is not a valid Json. message first char is: ["+ firstChar + "]";
					break;
				}
			
			default:
				returnMessage = "No valid communication type was found. check db for right type. message type is: ["+ contentType + "]";
				isValid= false;

			}// switch
		} else { //contentType is null
			returnMessage =  "contentType type:[" +contentType+ "] is null OR first Char in message content:[" + firstChar + "] is null.";
			isValid= false;
		}
		if (!isValid){
			log.error("Failed validating contentType: " + returnMessage);
			contentTypeValidationResponse.setContnet(returnMessage);
			contentTypeValidationResponse.setValid(isValid);
			return contentTypeValidationResponse;
		} else{
			log.debug("Successfully validated contentType");
			contentTypeValidationResponse.setContnet(content);
			contentTypeValidationResponse.setValid(isValid);
			return contentTypeValidationResponse;
		}
	}
	//---------------------------------------------------------------------------------------------------------
	public String forwardClientIP (String xml, Service service, HttpServletRequest request, String guid){
		
		  // Should forward client ipAdress
        String ldpConfigVar = ldpconfig.getIpFromReq().trim();
        int service_checkForwardIPCheck = service.getForwardClientIp();
        String clientIP = null;                     
        
        if (ldpConfigVar == null){
           log.debug("Service param for ForwardClientIP is null - doing nothing.");
        } else {
        if ( "1".equals( ldpConfigVar ) ) {
        	log.debug("ipFormatReq Flag variant is: [" + ldpConfigVar + "]");                                   
            //SET ipFROMREQUEST                 
        	//ldpConfigVar = ldpConfigVar.trim();
        	if(service_checkForwardIPCheck == 1) {
        		clientIP = xmlToJson.getSpecificJson(xml, CLIENT_IP);
    			log.debug("clientIP from XML is :[" + clientIP + "]");
//                       clientIP = request.getHeader("X-FORWARDED-FOR");
        		if (clientIP == null){
        			// SHOULD BE - log.debug("client IP from header is empty, getting client IP from X-FORWARDED-FOR");
        			log.debug("clientIP from XML is null, Geting client IP from request.getRemoteAddr()");
        			clientIP = request.getRemoteAddr();                                  
        			log.debug("ClientIP recieved from Remote Address:["+clientIP+"]");                                  
        		}
        	}else{// if "0"
        		log.debug("service.ForwardClientIP is 0 - getting IP from request.getRemoteAddr()");
        		clientIP = request.getRemoteAddr();
    			log.debug("ClientIP recieved from Remote Address:["+clientIP+"]");                                  
        		// log.debug("client IP from header is empty, getting client IP from X-FORWARDED-FOR");
        		// Should be ->  clientIP = request.getHeader("X-FORWARDED-FOR");
        	}
            if (ldpConfigVar == null || !ldpConfigVar.equals("1") ) {
                log.debug("ipFormatReq Flag variant  is not 1, or it is null. Check apllication.yml ");
                clientIP = request.getRemoteAddr();                   
                    log.debug("ClientIP recieved from Remote Address: ["+clientIP+"]");                           
            }
            if (clientIP == null || ("").equals(clientIP)) {
                log.error("ClientIP is empty. Couldn't forward it");
                xml = null;
                return xml;

            } else {
                xml = removeClientIPTagFromHeader(xml, clientIP);
                if (("").equals(xml)) {
                    log.error("Could not find mandatory ClientIP tag to forward it");
                    xml = null;
                    return xml;
                }
            }
        }else{  // else: When "0".equals( ldpConfigVar )
            log.debug("ldpConfigVar (from config file) is 0 - Doing nothing.");
         }
        }
	
		return xml;
	}
	//---------------------------------------------------------------------------------------------------------
	public String removeClientIPTagFromHeader(String xml,String clientIP){
		String newXml = "";
        int ipStartPos = xml.indexOf(TAG_CLIENTIP);
        // Well well, looks like we have a clientIP tag. DESTROY IT!
        if (ipStartPos != -1) {
            int nextOpenTagPos = xml.indexOf("<",ipStartPos);
            String beforeClientIP = xml.substring(0, ipStartPos+TAG_CLIENTIP.length()+1);
            String afterClientIP = xml.substring(nextOpenTagPos);
            newXml = beforeClientIP+clientIP+afterClientIP;
        } else {
        	return xml;
        }
        return newXml;
    }
	//---------------------------------------------------------------------------------------------------------
	public boolean isEnabled(ExtendedService extendedService, String guid){
		boolean isEnabled = true;
		Service service = extendedService.getService();
		if ( service.getEnabled() == 0 ){
			isEnabled = false;
			String error = "service [" + service.getServiceName() + "] Enabled field is 0, service is disabled";
			log.error(error);
   		    auditService.requestResponseBuildAudit(extendedService, null, error, guid);		
   		    return false;
		}
		Integer enabled = service.getEnabled();
		if ( enabled == null || enabled == 0 ) {
			isEnabled = false;
			String error = "service [" +service.getServiceName()+"] is disabled";
			log.error(error);
   		    auditService.requestResponseBuildAudit(extendedService, null, error, guid);		
   		    return false;
		}
		
		return isEnabled;
	}
	//---------------------------------------------------------------------------------------------------------
	// Reloading database again after changes.
	@Override
	public boolean reload() {
		List<Detector> detectorReloadingList   = new ArrayList<Detector>();
		boolean reloaded = true;
		  serviceLoader.init();
	      detectorReloadingList = detectorListHandler.getDetectors();
	     for (Detector detector : detectorReloadingList) {
	     try {
				Annotation[] annotations = detector.getClass().getAnnotations();
				for (Annotation annotation : annotations) {
					if (annotation.annotationType().equals(ValidatorType.class)) {
						ValidatorType type = (ValidatorType) annotation;
						Integer place = type.priority();
						detector.setPriority(place);
						detector.init();
						log.debug("initialized Detector [ " +detector.getClass().getSimpleName()+ "]");
//						listHandler.getDetectors().add(detector);
					}
					}
	     }
	     	catch (Exception e) {
			log.debug("Could not reload services from db " + e.getMessage());
			reloaded =false;
			}
	     }
	     if (reloaded){
	     Collections.sort(detectorListHandler.getDetectors());
	     log.debug("All services were reloaded from db");
	     return reloaded;
			}
		return reloaded;
		
	}
	
}
