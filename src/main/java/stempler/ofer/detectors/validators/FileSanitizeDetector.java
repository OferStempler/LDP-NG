package stempler.ofer.detectors.validators;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.tika.Tika;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;

import stempler.ofer.configuration.LdpConfig;
import stempler.ofer.dao.ServiceRepository;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.MapsHandler;
import stempler.ofer.detectors.ValidatorType;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.K300Response;
import stempler.ofer.model.NodesContextAndIndex;
import stempler.ofer.model.entities.ServiceDependencies;
import stempler.ofer.model.enums.DetectorTypes;
import stempler.ofer.utils.Utils;
import lombok.extern.log4j.Log4j;

@ValidatorType(priority = 6)
@Component
@Log4j
public class FileSanitizeDetector extends Detector {

//	@Autowired
//	Utils utils;

	@Autowired
	MapsHandler mapsHandler;

	@Autowired
	ServiceRepository servicesRepos;
	
	@Autowired
	LdpConfig config;

//	private final String ELEMENT_SEP =".";
//	private final String ELEMENT_NS_SEP = ":";
	private final String COMMON_CHANNELNAME = "common.channelName";
	private final String COMMON_CLIENTIP = "common.clientIP";
	private final String LDP = "LDP";
	private final String IMAGE = "image";

	
	private List<ServiceDependencies>       serviceDependenciesList = null;
	private Map<String, String> mimeTypeMap = new HashMap<String, String>();

	public boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest) {
		boolean isValid = true;
	
		serviceDependenciesList = extendedService.getServiceDependencies();
		for (ServiceDependencies serviceDependency : serviceDependenciesList) {
			if (serviceDependency != null && serviceDependency.getDependencyId() == DetectorTypes.FILESANITIZE_VALIDATION.code // 6
					&& serviceDependency.getMessageType().equals(messageType)
					&& !StringUtils.isEmpty(serviceDependency.getDependencyValue())
					&& serviceDependency.getEnabled() == 1) {

				log.debug("Starting file sanitation validation. Base64 field: ["+ serviceDependency.getDependencyValue() + "]");

				try {
					// 1) Get fileGuid
					HttpEntity<MultiValueMap<String, String>> requestEntity = createGetGuidPostReqest(servletRequest, content.toString());
					RestTemplate rt = new RestTemplate();
					// TODO Needs to decide from where get k300 url. For now in yml
					String getGuidUrl = config.getK300GetFileGuidUrl();
					log.debug("Sending message to [ " + getGuidUrl + "]");
					ResponseEntity<String> responseHttpEntity = rt.exchange(getGuidUrl, HttpMethod.POST, requestEntity,String.class);
					log.debug("Got response from k300 getFileGuid. Validating response.");
					String replyString = responseHttpEntity.getBody();
					K300Response k300GetGuidResponse = (K300Response) mapStringToObject(replyString, K300Response.class);
					log.debug("K300 response: [" + k300GetGuidResponse + "]");
					String fileGuid = k300GetGuidResponse.getGuid();

					// 2) get the base64 as String
					NodesContextAndIndex nodesContextAndIndex = getWantedNodeContent(serviceDependency.getDependencyValue(), content.toString());
					byte[] base64Bytes = getBase64AndConvertToByteArray(nodesContextAndIndex);
					
					// 3) Insert k300 guid into the String Buffer, and also return a new JSON String to send as extraInfo
					String extraInfo = changeStringBufferAndParseToJson(nodesContextAndIndex, fileGuid, content);
					if (extraInfo == null) {
						log.error("Could not parse extraInfo to JSON");
						return false;
					}
					// 4) make FileUpload request
					HttpEntity<MultiValueMap<String, Object>> requestEntity2 = createUploadFilePostReqest(k300GetGuidResponse, servletRequest, base64Bytes, extraInfo);

					String uploadFileUrl = config.getK300UploadFileUrl();
					log.debug("Sending message to [ " + uploadFileUrl + "]");
					ResponseEntity<String> responseHttpEntity2 = rt.exchange(uploadFileUrl, HttpMethod.POST,requestEntity2, String.class);
					String replyString2 = responseHttpEntity2.getBody();
					log.debug("Got reply from K300 upload File: [" + replyString2 + "]");
					K300Response k300UploadFileResponse = (K300Response) mapStringToObject(replyString2,K300Response.class);
					// 5) validate response
					isValid = k300UploadFileResponse.getIsSuccess().booleanValue() ? true : false;
					if (isValid) {
						log.debug("Succseffully sent file to K300");
					} else {
						log.error("Could not send file to k300");
					}
				} catch (Exception e) {
					log.error("Could not send to K300 ", e);
					return false;
				}
			}

		}//for
//		6) Return true as a validator.
		return isValid;
	}																			
//--------------------------------------------------------------------------------------------------------------------------------------	
private NodesContextAndIndex getWantedNodeContent(String dependencyValue, String content) throws Exception {
	log.debug("Looking for value: [" +dependencyValue+"]");
	NodesContextAndIndex nodesContextAndIndex = new NodesContextAndIndex();
	XPath xPath = XPathFactory.newInstance().newXPath();
	String expression = "//*[not(*)]";
	Document doc = Utils.buildXML(content);
	NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
	
	for (int i = 0; i < nodeList.getLength(); i++) {
//		System.out.println(getXPath(nodeList.item(i)));
		if (Utils.getXPath(nodeList.item(i)).endsWith(dependencyValue)){
			Node currNode = nodeList.item(i);
			String base64 = currNode.getTextContent();
			log.debug("Successfully found value");
			int endOfRequiredNodeContext = content.lastIndexOf(base64);
			nodesContextAndIndex.setEndOfNodeContext(endOfRequiredNodeContext);
			nodesContextAndIndex.setNodeContextText(base64);
			return nodesContextAndIndex;			
		}
	}
	log.error("Did not find matching node. Returning null");
		return null;
	}
//--------------------------------------------------------------------------------------------------------------------------------------	
//public String getXPath(Node node){
//	Node parent = node.getParentNode();
//	if(parent ==null){
//		return node.getNodeName();
//	}
//	return getXPath(parent) + "." +node.getNodeName() ;
//}
//--------------------------------------------------------------------------------------------------------------------------------------	
	private String changeStringBufferAndParseToJson(NodesContextAndIndex nodesContextAndIndex, String guid, StringBuffer content) {
		//replace the String buffer base64 for the fileGuid
		if (nodesContextAndIndex != null) {
			content.replace(
					nodesContextAndIndex.getEndOfNodeContext(),
					nodesContextAndIndex.getEndOfNodeContext() + nodesContextAndIndex.getNodeContextText().length(),
					guid);
			log.debug("Succssffuly replaced StringBuffer base64 with k300 file Guid");
		}
		//create a string like the stringBuffer to send as extraInfo
		JSONObject o = XML.toJSONObject(content.toString());
		log.debug("Succssffuly parsed XML string JSON");

		return o.toString();
	}

//--------------------------------------------------------------------------------------------------------------------------------------	
	private byte[] getBase64AndConvertToByteArray(NodesContextAndIndex nodesContextAndIndex)  {
		try {
		if (nodesContextAndIndex != null) {
			return DatatypeConverter.parseBase64Binary(nodesContextAndIndex.getNodeContextText());			
		}
		}catch (Exception e){
			log.error("Could not parse String to base 64");
			return null;
		}
		return null;
	}
//--------------------------------------------------------------------------------------------------------------------------------------
	private HttpEntity<MultiValueMap<String, Object>> createUploadFilePostReqest(K300Response k300GetGuidResponse, HttpServletRequest servletRequest, byte[] base64, String extraInfo) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		InputStream is = new ByteArrayInputStream(base64);
		log.debug("Getting fileType");
//		String name = getBase64FromValueField(???);
		Tika tika = new Tika();
		String fileType = tika.detect(base64);
		fileType = changeFileType(fileType);		
		File tempFile = File.createTempFile("LDP", "." +fileType);
		log.debug("About to save bytes as file in local temp folder: [" +tempFile.toString()+"]");
		FileCopyUtils.copy(is, new FileOutputStream(tempFile));
		log.debug("Successfully saved");

		 map.add("attachmentFile", new FileSystemResource(tempFile) );
		 map.add("ip",servletRequest.getRemoteAddr());
		 map.add("fileGuid", k300GetGuidResponse.getGuid());
		 map.add("extraInfo", extraInfo );

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>( map, headers);
		return request;
	}	
//--------------------------------------------------------------------------------------------------------------------------------------	
	private HttpEntity<MultiValueMap<String, String>> createGetGuidPostReqest(HttpServletRequest servletRequest, String content) throws Exception {	
		log.debug("Creating getFileGuid Post request");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		
		NodesContextAndIndex nodesContextAndIndex =  getWantedNodeContent(COMMON_CHANNELNAME , content.toString());
		String channelRequestName = nodesContextAndIndex.getNodeContextText();
		if (channelRequestName == null) {
			log.error("Could not find common.channelName in request. Putting \"LDP\"");
			channelRequestName = LDP;
		}
		nodesContextAndIndex  =  getWantedNodeContent(COMMON_CLIENTIP, content.toString());
		String clientIP = nodesContextAndIndex.getNodeContextText();
		if (clientIP == null) {
			log.error("Could not find common.clientIP in request. Getting ip from remoteAdress");
			clientIP = servletRequest.getRemoteAddr();
		}
		log.debug("Building k300 request for getUploadFile. Clientip: [" +clientIP+ "] channelRequestName [ " +channelRequestName+ "]");
		 map.add("ip", clientIP);
		 map.add("channelRequestName", channelRequestName);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>( map, headers);
		return request;
	}
//--------------------------------------------------------------------------------------------------------------------------------------	
	private Object mapStringToObject(String stringToMap,  Class <?> mapToObjectClass) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		Object mapToObject =  mapper.readValue(stringToMap, mapToObjectClass);
		return mapToObject;
	}	

//--------------------------------------------------------------------------------------------------------------------------------------
//	private void findFileExtenssion(byte[] base64) throws IOException{
//		
//		Tika tika = new Tika();
//		String fileType = tika.detect(base64);
//		fileType = changeFileType(fileType);
//		System.out.println(fileType);
//		
//		
//		File tempFile = File.createTempFile("MyFile", "." +fileType);
//	}
//	
//--------------------------------------------------------------------------------------------------------------------------------------
	// All mime-types supported are printed into log when the app boots up
	private String changeFileType(String mimeType) {
		
		log.debug("Getting file type from mimeType ["+mimeType+"]");
		String fileType = mimeTypeMap.get(mimeType);
		if (fileType == null) {
		log.debug("Could not find file Type for mimeType [" +mimeType+"]. returning file type default of .txt");
		fileType = "txt";
		} else {
			log.debug("Got file type by mimeType. File type is: ["+fileType+"]");	
		}
		return fileType;
	}
		
//		boolean cahnged = false;
//		if (fileType.subSequence(0, 5).equals(IMAGE)) {
//			fileType = fileType.substring(6, fileType.length());
//		}
//		if (fileType.contains("pdf")) {
//			fileType = "pdf";
//		}
//		if (fileType.contains("msword")) {
//			fileType = "doc";
//		}
//		if (fileType.contains("wordprocessingml.document")) {
//			fileType = "docx";
//		}
//		if (fileType.contains("vnd.ms-excel")) {
//			fileType = "xls";
//		}
//		if (fileType.contains("spreadsheetml.sheet")) {
//			fileType = "xls";
//		}
//		if (fileType.contains("vnd.ms-powerpoint")) {
//			fileType = "ppt";
//		}
//		if (fileType.contains("presentationml")) {
//			fileType = "pptx";
//		}
//		if (fileType.contains("text/plain")) {
//			fileType = "txt";
//		}
//		if (cahnged){
//			log.debug("Changed fileType to: ["+fileType+"]");
//		} else {
//			log.debug("Could not find file Type.");
//		}
//		return fileType;
//	}
//--------------------------------------------------------------------------------------------------------------------------------------
	

//	private NodesContextAndIndex getWantedNodeContentXXX(String dependencyValue, String content) {
//		boolean found   = false;
//		// check if element is enabled or blocked
//	
//		int lastElementIndex = dependencyValue.lastIndexOf(ELEMENT_SEP);
//		String elementName = dependencyValue.substring(lastElementIndex + 1);
//		Document doc;
//		try {
//			doc = Utils.buildXML(content);
//			if (doc == null) {
//				log.error("Could not build XML document");
//				return null;
//			} 
//
//				NodeList elementsInXML = doc.getElementsByTagName(elementName);
//				for (int j = 0; j < elementsInXML.getLength() && !found; j++) {
//					//find the right node
//					Node currNode = elementsInXML.item(j);
//					String currNodeName = currNode.getNodeName();
//					String nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName);
//					Node parentNode = currNode.getParentNode();
//					//make sure nodes Name is the right one according to its entire parents values. 
//
//					while (!(parentNode instanceof Document)) {
//						currNodeName = parentNode.getNodeName();
//						nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName) + ELEMENT_SEP + nodeFullName;
//						parentNode = parentNode.getParentNode();
//					}
//
//					if (nodeFullName.equals(dependencyValue)) {
//						log.debug("Successfully found base64 content in dependencyValue's field");
//						NodesContextAndIndex nodesContextAndIndex = new NodesContextAndIndex();
//						String base64 = currNode.getTextContent() ;
//						int endOfRequiredNodeContext = content.lastIndexOf(base64);
//						nodesContextAndIndex.setEndOfNodeContext(endOfRequiredNodeContext);
//						nodesContextAndIndex.setNodeContextText(base64);
//						return nodesContextAndIndex;
//					}
//				}
//			
//		} catch (Exception e) {
//			log.error("Could not get base 64 from dependencyValue. " + e);
//			return null;
//		}
//		return null;
//	}
//--------------------------------------------------------------------------------------------------------------------------------------		
//	private String getCommons(String dependencyValue, String content) {
//		boolean found   = false;
//		// check if element is enabled or blocked
//	
//		int lastElementIndex = dependencyValue.lastIndexOf(ELEMENT_SEP);
//		String elementName = dependencyValue.substring(lastElementIndex + 1);
//		Document doc;
//		try {
//			doc = Utils.buildXML(content);
//			if (doc == null) {
//				log.error("Could not build XML document");
//				return null;
//			} 
//
//				NodeList elementsInXML = doc.getElementsByTagName(elementName);
//				for (int j = 0; j < elementsInXML.getLength() && !found; j++) {
//					//find the right node
//					Node currNode = elementsInXML.item(j);
//					String currNodeName = currNode.getNodeName();
//					String nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName);
//					Node parentNode = currNode.getParentNode();
//					//make sure nodes Name is the right one according to its entire parents values. 
//
//					while (!(parentNode instanceof Document)) {
//						currNodeName = parentNode.getNodeName();
//						nodeFullName = (currNodeName.indexOf(ELEMENT_NS_SEP) != -1? currNodeName.substring(currNodeName.indexOf(ELEMENT_NS_SEP) + 1): currNodeName) + ELEMENT_SEP + nodeFullName;
//						parentNode = parentNode.getParentNode();
//					}
//
//					if (nodeFullName.endsWith(dependencyValue)) {
//						log.debug("Successfully found value [" +dependencyValue+"]");
//						String requestValue = currNode.getTextContent() ;					
//						return requestValue;
//					}
//				}
//			
//		} catch (Exception e) {
//			log.error("Could not find common value. " + e);
//			return null;
//		}
//		return null;
//	}

//-----------------------
	@Override
	public void init() {
		
		log.debug("Loading mimeType map for Sanitize Validation ");
		
		mimeTypeMap.put("application/pdf", "pdf");
		mimeTypeMap.put("application/msword", "doc");
		mimeTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
		mimeTypeMap.put("pplication/vnd.ms-excel", "xls");
		mimeTypeMap.put("vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
		mimeTypeMap.put("vnd.ms-powerpoint", "ppt");
		mimeTypeMap.put("vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
		mimeTypeMap.put("text/plain", "txt");
		mimeTypeMap.put("image/tiff", "tiff");
		mimeTypeMap.put("image/png", "png");
		mimeTypeMap.put("image/svg+xml", "svg");
		mimeTypeMap.put("image/webp", "webp");
		mimeTypeMap.put("image/gif", "gif");
		mimeTypeMap.put("image/bmp", "bmp");
		mimeTypeMap.put("image/jpeg", "jpeg");

		log.debug("Successfully loaded mimeType map [" +mimeTypeMap.toString()+"]");

		
	}


}
