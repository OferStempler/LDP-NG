package stempler.ofer.controller;

import stempler.ofer.model.ImportExportResponse;
import stempler.ofer.model.JsonRequestAndResponse;
import stempler.ofer.model.LdpResponse;
import stempler.ofer.model.LdpResponseExtended;
import stempler.ofer.service.GetSchemasFromWSDL;
import stempler.ofer.service.RequestResponseService;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.log4j.Log4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
@Log4j
public class MainController {

@Autowired
private GetSchemasFromWSDL getSchamsService;

@Autowired
private RequestResponseService requestResponseService;

//---------------------------------------------------------------------------------------------------------
@PostConstruct
public void init() throws ServletException {
	
	//Check JVM started with UTF-8 encoding
	String systemEncoding = System.getProperty("file.encoding");
	log.debug("System Encoding is: " + systemEncoding);
	if (!systemEncoding.equals("UTF-8")){
		log.error("!!!!!!!ERROR STARTING restLDP!!!!!!!!");
		log.error("Check your .bat file specifying the JVM encoding is UTF-8. If not, set your bat file to: java -Dfile.encoding=UTF-8 -jar LDP-NG.jar");		
		System.exit(0);
	}
	log.debug("Loading MQConnectionPool");
 }
 //---------------------------------------------------------------------------------------------------------
 @RequestMapping(method = RequestMethod.POST,  produces = {"application/json"})
 public @ResponseBody ResponseEntity<String> doPost( HttpServletRequest request, HttpServletResponse response,  MultipartFile[] attachmentFiles, String messageBody) throws IOException  {
	 log.info("===============================");
     log.info("POST");
     LdpResponse ldpResponse = requestResponseService.startValidation(request, response, attachmentFiles, messageBody);
      
     HttpHeaders httpHeaders = new HttpHeaders();
	 if ( ldpResponse instanceof LdpResponseExtended){
		 LinkedMultiValueMap<String, String> newHeaders = ((LdpResponseExtended)ldpResponse).getHeaders();
		 httpHeaders.putAll(newHeaders);
	     String  content       = ((LdpResponseExtended)ldpResponse).getContent();
	     HttpStatus httpStatus = ((LdpResponseExtended)ldpResponse).getResponseCode();

    	 httpHeaders.forEach((k,v)->{ log.debug("Returning header - key:[" + k + "], value:" + v + ""); } );

    	 log.debug("Returning valid LDP-response: code:[" + ldpResponse.getResponseCode() + "], response-messgae:[" + ldpResponse.getResponseMessage() + "], "
    				+ "content.length:["
    				+ ( content == null ? "NULL" : content.length() )
    				+ "]");
    		
    		// TRY response.getWriter().print(replyString);
	     return new ResponseEntity<String> //("{\"test\": \"jsonResponseExample\"}", httpHeaders, HttpStatus.OK);
	                   (content, httpHeaders, httpStatus);
	 }else{
	 	return new ResponseEntity<>(ldpResponse.getResponseMessage(), httpHeaders,/*HttpStatus.INTERNAL_SERVER_ERROR*/ldpResponse.getResponseCode());
	 }
 }
 
 
//---------------------------------------------------------------------------------------------------------
  // TODO - this controller deals with all GET request, EXCEPT for those who have the two params WsdlURL and serviceName.
 // GET REQUESTS are not yes supported, need to implemented and tested
//  @RequestMapping(method = RequestMethod.GET,  produces = {"text/plain"},  params  = {"!WsdlURL", "!serviceId"})
//  public  ResponseEntity<?>  doGET( HttpServletRequest request, HttpServletResponse response) throws IOException  {
//	 log.info("===============================");
//     log.info("GET");
//     response.setStatus(HttpServletResponse.SC_OK);
//     HttpHeaders httpHeaders= new HttpHeaders();
//  
//     LdpResponse ldpResponse = requestResponseService.getParameters(request, response);
//     if(ldpResponse.getResponseMessage() != null && ldpResponse instanceof LdpResponseExtended){
//    		String     content    = ((LdpResponseExtended)ldpResponse).getContent();
//    		HttpStatus httpStatus = ((LdpResponseExtended)ldpResponse).getResponseCode();
//    		LinkedMultiValueMap<String, String> newHeaders = ((LdpResponseExtended)ldpResponse).getHeaders();
//    	    httpHeaders.putAll(newHeaders);
//    		
//    		
//			log.debug("Got response-code:[" + ldpResponse.getResponseCode() + "], response-messgae:[" + ldpResponse.getResponseMessage() + "], "
//	    	 		+ "content.length:["
//	    			+ ( content == null ? "NULL" : content.length() )
//	    			+ "]");
//	    return new ResponseEntity<String> //("{\"test\": \"jsonResponseExample\"}", httpHeaders, HttpStatus.OK);
//	                   (content, httpHeaders, httpStatus);
//     } else {
//     	 return new ResponseEntity<>(ldpResponse.getResponseMessage(), httpHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
//     }
// }

  
//---------------------------------------------------------------------------------------------------------

@RequestMapping(path = "/reload",  method = RequestMethod.GET)
public @ResponseBody ImportExportResponse reload() throws IOException  {
		log.info("===============================");
		log.info("Reloading services and dependencies");
		ImportExportResponse response = new ImportExportResponse();
		boolean reload = requestResponseService.reload();
		if (reload) {
			log.debug("Successfully reloaded all services and dependencies");
			response.setSuccess(true);
			response.setResponseMessage("Succsess");
			return response;
		} else {
			log.debug("Failed reloading all services and dependencies. Check LDP logs for more details");
			response.setSuccess(false);
			response.setResponseMessage("failed");
			return response;
		}
		
}
//---------------------------------------------------------------------------------------------------------
/*  test adding schema:   http://localhost:8080/addXSD?WsdlURL=http://192.22.10.18:7001/JerusalemBank/ADA_Services/proxy_services/ArchiveData?wsdl&serviceId=888
*/	
	@RequestMapping(value = "/addXSD", params  = {"WsdlURL", "serviceId"},  method = RequestMethod.GET,  produces = {"text/plain"})
 public @ResponseBody ResponseEntity<String> addToSchemas(@RequestParam("WsdlURL") String wsdlUri, @RequestParam("serviceId") int serviceId, HttpServletResponse response)    {
	log.debug("===============================");
	log.debug("Building XSDs and Regex from wsdl");
	LdpResponse ldpResponse = getSchamsService.getSchemasFromWSDL(wsdlUri, serviceId);
//	   LdpResponse ldpResponse = getSchamsService.getSchemasFromWSDL(wsdlUri, serviceId);
   return new ResponseEntity<String>(ldpResponse.getResponseMessage(), ldpResponse.getResponseCode());
 }
//---------------------------------------------------------------------------------------------------------

 	@RequestMapping(value = "/buildRegexFromJSON", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<String> buildRegexFromJSON( @RequestBody JsonRequestAndResponse requestAndResponse)    {
   	log.debug("===============================");
   	log.debug("Starting process Json Request and Response to Service Regex: [" + requestAndResponse +"]");
   	LdpResponse ldpResponse =getSchamsService.buildRegexFromJson(requestAndResponse);
      return new ResponseEntity<String>(ldpResponse.getResponseMessage(), ldpResponse.getResponseCode());
    }
//---------------------------------------------------------------------------------------------------------
	/*  test adding serviceRegex:   http://localhost:8080/addXSD?WsdlURL=http://192.22.10.18:7001/JerusalemBank/ADA_Services/proxy_services/ArchiveData?wsdl&serviceId=888
	*/	
//		@RequestMapping(value = "/addServiceRegex", params  = {"WsdlURL", "serviceId"},  method = RequestMethod.GET,  produces = {"text/plain"})
//	 public  @ResponseBody ResponseEntity<String> addServiceRegex(@RequestParam("WsdlURL") String wsdlUri, @RequestParam("serviceId") int serviceId, HttpServletResponse response)    {
//		log.debug("===============================");
//		log.debug("Building ServiceRegex and Regex from wsdl");
//		   LdpResponse ldpResponse = getSchamsService.createServiceRegexes(wsdlUri, serviceId);
//		   return new ResponseEntity<String>(ldpResponse.getResponseMessage(), ldpResponse.getResponseCode());
//		}
// 
}
