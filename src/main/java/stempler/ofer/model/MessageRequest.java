package stempler.ofer.model;

import stempler.ofer.utils.Utils;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

@Log4j
@Data
@ToString
public class MessageRequest {
	private static Logger log  = Logger.getLogger(MessageRequest.class);

	private String serviceName;
	private String xmlString;
	private int xmlSize;
	private Document xmlDocument;
	private boolean wasBuilt;
	
	public MessageRequest() {
		serviceName="";
		xmlString="";
		xmlDocument=null;
		wasBuilt=false;
	}
	
	public MessageRequest(String serviceName, String xmlString, int xmlSize) {
		setServiceName(serviceName);
		setXmlString(xmlString);
		setXmlSize(xmlSize);
	}
	
	public MessageRequest(String serviceName, String xmlString) {
		setServiceName(serviceName);
		setXmlString(xmlString);
		setXmlSize(xmlString.length());
	}
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getXmlString() {
		return xmlString;
	}
	public void setXmlString(String xmlString) {
		this.xmlString = xmlString;
	}
	public int getXmlSize() {
		return xmlSize;
	}
	public void setXmlSize(int xmlSize) {
		this.xmlSize = xmlSize;
	}

	public Document getXmlDocument() {
		
		if (!wasBuilt) { 
			wasBuilt = true;
			try {
				xmlDocument = Utils.buildXML(xmlString);
			} catch (Exception e){
				log.error("XML is not valid for service "+getServiceName()+": "+e.getMessage());
			}
		}
		
		return xmlDocument;
	}
	
}
