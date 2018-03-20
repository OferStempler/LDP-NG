package stempler.ofer.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.LdpResponse;
import stempler.ofer.model.LdpResponseExtended;
import stempler.ofer.model.ValidationResponse;
import stempler.ofer.service.AuditService;
import stempler.ofer.service.DetectorService;
import stempler.ofer.service.RequestResponseService;
import stempler.ofer.service.RestForwardRequestsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

@Component
@Log4j
public class RestForwardRequestsServiceImpl implements RestForwardRequestsService {


	@Autowired
	AuditService auditService;

	@Autowired
	DetectorService detectorService;
	
	@Autowired
	RequestResponseService requestResponseService; 
	
	private final String MESSAGE_TYPE_RESPONSE = "Response";
	private  final String MESSAGE_TYPE_REQUEST = "Request";


	/**
	 * In case of REST - Http call should be made
	 * @param httpMethod 
	 * @throws Exception 

	 */
	//-----------------------------------------------------------------------------------------------------------------
	@Override
	public LdpResponse restForwardRequest(ExtendedService extendedService,  String content, String guid,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse, String httpMethod)throws Exception {
		LdpResponse ldpResponse	 		= new LdpResponse();
		String origContentResponse      = null;
		String replyString				= null;
		HttpStatus httpStatus 			= null;
		RestTemplate template			= new RestTemplate();
		String destination 				= extendedService.getService().getDestination();
		String method 					= servletRequest.getMethod();
		String contentType 				= extendedService.getService().getContentType();
		String serviceName 				= extendedService.getService().getServiceName();
		
		log.debug("Forwarding a  [" + method + "] requeset");
		log.debug("Sending data to destination [" + destination + "]");
		template.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		//10 Build headers from request
//		HttpHeaders httpHeadersRequest = getHeadersFromRequest(servletRequest);
		HttpHeaders httpHeadersRequest = getHeadersFromRequest(servletRequest, contentType, extendedService);
		log.debug((httpHeadersRequest != null) ? "Successfully added headers: [" +httpHeadersRequest.toString()+ "]" : "Request headers are null");
		HttpEntity<String> httpEntity 	= new HttpEntity<String>(content,httpHeadersRequest);
		
		if(httpMethod.equals("POST")){
		log.debug("Sending request body: [" + content + "]");
		} else {
			log.debug("Sending parameters: [" + content + "]");	
		}
		// 2) Make REST Request
			ResponseEntity<String> responseHttpEntity = template.exchange(destination, HttpMethod.valueOf(method), httpEntity, String.class);
			log.debug("Got response from client. Validating response.");
			httpStatus = responseHttpEntity.getStatusCode();
			replyString = responseHttpEntity.getBody();
			origContentResponse = new String( (replyString != null ? replyString.getBytes() : "".getBytes()), "UTF-8");
		// 3) Validate reply		
//			  check if XML with no SOAP, XML with SOAP or Json (SOAP | XMLNOSOAP | JSON). Than validate structure	
			ValidationResponse contentTypeValidationResponse = requestResponseService.validateContentType(replyString, contentType, serviceName, guid, MESSAGE_TYPE_RESPONSE, extendedService.getServiceConversions());
			  if(!contentTypeValidationResponse.isValid()) {
				  ldpResponse.setResponseCode(HttpStatus.CONFLICT);
				  ldpResponse.setResponseMessage(contentTypeValidationResponse.getContnet());;
				  return ldpResponse;
			  }
			  log.debug("Successfully validated response content type");
			  replyString= contentTypeValidationResponse.getContnet();
			
			// 4) Create headers
			log.debug("Switching  LDP response headers for client headers");
			LinkedMultiValueMap<String, String> responseHeaders = getHeadersFromResponse(responseHttpEntity.getHeaders(), contentType, extendedService);
			log.debug((responseHeaders != null) ? "Successfully switched headers: [" +responseHeaders.toString()+ "]" : "Response headers are null");

	
		LdpResponseExtended ldpResponseExtended = new LdpResponseExtended();
		ldpResponseExtended.setContent(replyString);
		ldpResponseExtended.setOrigContentResponse(origContentResponse);
		ldpResponseExtended.setHeaders(responseHeaders);
		ldpResponseExtended.setResponseCode(httpStatus);
		ldpResponseExtended.setResponseMessage("SUCCESS");
		return ldpResponseExtended;
	}



//-----------------------------------------------------------------------------------------------------------------------------------	
	private LinkedMultiValueMap<String, String> getHeadersFromResponse(HttpHeaders httpHeaders, String contentType,ExtendedService extendedService) {
		LinkedMultiValueMap<String, String> newHeaders = new LinkedMultiValueMap<String, String>();
		if (httpHeaders != null) {
			Set<Entry<String, List<String>>> set = httpHeaders.entrySet();
			for (Entry<String, List<String>> entry : set) {
				for (String val : entry.getValue()) {
					if(entry.getKey().equals("Content-Length")) {
						continue;
					}
					if (entry.getKey().equals("Content-Type") && contentType.equals("COMPOSITE")) {
						String sourceResponseInputType = extendedService.getServiceConversions().getSourceResponseInputType();
						String contentTypeHeader = getContentType(sourceResponseInputType, MESSAGE_TYPE_RESPONSE);
						newHeaders.set(entry.getKey(), contentTypeHeader);
						continue;
						
					}
					newHeaders.set(entry.getKey(), val);
				}
			}

		} else {
			log.debug("No headers were found in response");
		}
		return newHeaders;
	}
//-----------------------------------------------------------------------------------------------------------------------------------	
	private HttpHeaders getHeadersFromRequest(HttpServletRequest request, String contentType,ExtendedService extendedService) {
		log.debug("Building REST headers from HttpServletRequest headers");
		HttpHeaders httpHeaders = new HttpHeaders();
		LinkedMultiValueMap<String, String> newHeaders = new LinkedMultiValueMap<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String key = (String) headerNames.nextElement();
				// ignore content length. Will be set automatically
				if (key.equals("content-length")) {
					continue;
				}
				// set content-type according to end point requirement - Will bechanged from original request only if contnet type is "COMPOSITE"
				if (key.equals("content-type") && contentType.equals("COMPOSITE")) {
					String destinationContentType = extendedService.getServiceConversions().getDestinationRequestInputType();
					String contentTypeHeader = getContentType(destinationContentType, MESSAGE_TYPE_REQUEST);
					newHeaders.add(key, contentTypeHeader);
					continue;
				}
				String value = request.getHeader(key);
				newHeaders.add(key, value);
			}
		} else {
			log.debug("No headers were found in request");
		}

		httpHeaders.putAll(newHeaders);
		return httpHeaders;
	}
//-----------------------------------------------------------------------------------------------------------------------------------	



private String getContentType(String requiredContentType, String type) {
	String message = (type.equals(MESSAGE_TYPE_RESPONSE)) ?
	"Content type is COMPOSITE - changing response content-type header according to serviceConversions.sourceResponseInputType: [" +requiredContentType+ "]" :
	"Content type is COMPOSITE - changing request content-type header according to serviceConversions.destinationContentType: [" +requiredContentType+ "]";
	log.debug(message);
	if (requiredContentType.equals("XMLNOSOAP") || requiredContentType.equals("XML")) {
		log.debug("Changed content-type header to \"text/xml\"");
		 return "text/xml;charset=UTF-8";
	} else if (requiredContentType.equals("JSON")) {
		log.debug("Changed content-type header to \"application/json\"");
		return "application/json;charset=UTF-8";
	} else {
		return null;
	}

}

}

