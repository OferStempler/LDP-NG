package stempler.ofer.model;

import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Log4j
@Data
@ToString
public class MQldpRequest {
	
	private String serviceName;
	private String xmlString;
	
	public MQldpRequest () {
		serviceName = "";
		xmlString = "";
	}
	
	public MQldpRequest(String serviceName, String xmlString) {
		this.serviceName = serviceName;
		this.xmlString = xmlString;
	}
	

	
	public Document getXmlDocument() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc =  builder.parse(new InputSource(new StringReader(xmlString)));
		return doc;
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
	
	
}
