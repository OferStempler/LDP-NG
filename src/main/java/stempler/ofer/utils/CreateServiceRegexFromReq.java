package stempler.ofer.utils;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import stempler.ofer.dao.ServiceRegularExpressionsRepo;
import stempler.ofer.model.entities.ServiceRegularExpressions;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreateServiceRegexFromReq {

	private static Logger log  = Logger.getLogger(CreateServiceRegexFromReq.class);

	
@Autowired
ServiceRegularExpressionsRepo serviceRegexRepo;
	
	public boolean getSchema(String xml, int serviceId, String type) throws Exception{
		boolean added = true;
		log.debug("Creating ServiceRegex for [" +type+"]");
		int counter = 0;
		
		try {
		//make a list of all the elements already exists for this service
		List<String> elementList = new ArrayList<String>();
		List<ServiceRegularExpressions> serviceRegexList =   serviceRegexRepo.findByServiceId(serviceId);
		if (serviceRegexList.size() != 0 && serviceRegexList!= null){
		for (ServiceRegularExpressions serviceRegularExpressions : serviceRegexList) {
			elementList.add(serviceRegularExpressions.getElement());
		}
		}
		//make Xpath for all the nodes in the request and put them into a list
		String fixed = "";
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "//*[not(*)]";
		Document doc = Utils.buildXML(xml);
		List<String> xPathList = new ArrayList<String>();
		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			System.out.println(getXPath(nodeList.item(i)));
			xPathList.add(getXPath(nodeList.item(i)));
		}
		
		//Take from the list only the required Xpath.
		// only Strings that do not already exists in the db will be saved.
		
		
		for (String current : xPathList) {
			//for Request
			if(type.equals("Request")){
			if(current.contains("RequestChannel.Channel")){
			String newString = current.substring(current.lastIndexOf("RequestChannel.Channel") );
			if(!elementList.contains(newString)){
				
			ServiceRegularExpressions serviceRegex = new ServiceRegularExpressions();
			serviceRegex.setElement(newString);
			serviceRegex.setServiceId(serviceId);
			serviceRegex.setMessageType("Request");
			serviceRegexRepo.save(serviceRegex);
			counter++;
				
//			System.out.println(newString);
			
					} else {
						log.debug("elemnt [ " + newString+ " ] already exists in db");
					}
				}
			} else if (type.equals("Response")){
			if(current.contains("ResponseChannel.Channel")){
			String newString = current.substring(current.lastIndexOf("ResponseChannel.Channel") );
			if(!elementList.contains(newString)){
				
				ServiceRegularExpressions serviceRegex = new ServiceRegularExpressions();
				serviceRegex.setElement(newString);
					serviceRegex.setServiceId(serviceId);
					serviceRegex.setMessageType("Response");
					serviceRegexRepo.save(serviceRegex);
					counter++;
					// System.out.println(newString);
				} else {
					log.debug("elemnt [ " +newString+ " ] already exists in db");
					}
				}
			}
		}
			log.debug("succssefully created [" + counter	+ "] ServiceRegex, for [" + type+ "] message ");
			return added;
		} catch (Exception e){
			log.debug("Could not create serviceRegexes" + e);
			return !added;
		}
	}
	

	public String getXPath(Node node){
		Node parent = node.getParentNode();
		if(parent ==null){
			return node.getNodeName();
		}
		return getXPath(parent) + "." +node.getNodeName() ;
	}
	

	
	
}
