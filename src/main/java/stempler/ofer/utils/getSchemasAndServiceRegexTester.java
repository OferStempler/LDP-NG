//package il.co.boj.utils;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.net.URLConnection;
//import java.util.HashSet;
//import java.util.Set;
//
//import org.springframework.stereotype.Component;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//
//import com.eviware.soapui.impl.wsdl.WsdlInterface;
//import com.eviware.soapui.impl.wsdl.WsdlOperation;
//import com.eviware.soapui.impl.wsdl.WsdlProject;
//import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlImporter;
//import com.eviware.soapui.model.iface.Operation;
///*
//This is a stand alone tester with main for retrieving  all xsds and wsdl from a given url, or to 
//get the given ServiesRegex from a url;
//*/
//@Component
//public class getSchemasAndServiceRegexTester {
//
//	static Set<String> schemaList = new HashSet<String>();
//	
//	
//	public String getUrlContent(String sourceUrl){
//	
//		String xml = null;
//	try {
//		
//		// get URL content
//		URL url = new URL(sourceUrl);
//		URLConnection conn = url.openConnection();
//
//		// open the stream and put it into BufferedReader
//		BufferedReader br = new BufferedReader(
//                           new InputStreamReader(conn.getInputStream()));
//
//		// build the xml as a string
//		StringBuilder builder = new StringBuilder();
//		String line = null;
//		while ((line = br.readLine()) != null) {
//			builder.append(line);
//		}
//		xml = builder.toString();
//		br.close();
//
//		schemaList.add(xml);
////		System.out.println(xml);
//		this.getXmlDocument(xml);
//
//	} catch (Exception e) {
//		e.printStackTrace();
//	} 
//	return xml;
//	}
//
//	public void getServiceRegexes(String url){
//		
//		CreateServiceRegexFromReq schema = new CreateServiceRegexFromReq();
//		WsdlProject project;
//		try {
//			project = new WsdlProject();
//			WsdlInterface[] wsdls = WsdlImporter.importWsdl(project, url);
//			WsdlInterface wsdl = wsdls[0];
//			for (Operation operation : wsdl.getOperationList()) {
//				WsdlOperation wsdlOperation = (WsdlOperation) operation;
//				
//				String request = wsdlOperation.createRequest(true);
//				String response = (wsdlOperation.createResponse(true));
//				
//				System.out.println("OP:"+wsdlOperation.getName());
//				System.out.println("Request:");
//				System.out.println(wsdlOperation.createRequest(true));
//				System.out.println("Response:");
//				System.out.println(wsdlOperation.createResponse(true));
//			}
//			}catch (Exception e){
//				System.out.println(e);
//			}
////				String req = schema.getSchema(request);
////				String res = schema.getSchema(response);
////				System.out.println(request);
////				System.out.println(response);
//	}
//		
//    public void getXmlDocument(String xml) throws Exception {
//     
//    	//build Xml doc from string
//    	Document doc = Utils.buildXML(xml);
//        doc.getDocumentElement().normalize();
//        
//        //check if the xml is WSDL or xsd:
//        
//        //for wsdl
//        NodeList list = doc.getElementsByTagName("xsd:import");
//		if (list.getLength() > 0) {
//			for (int i = 0; i < list.getLength(); i++) {
//
//				Element el = (Element) list.item(i);
//				//get the url for the XSDs
//				String s = el.getAttribute("schemaLocation");
//				if (s != null) {
//					//get the xml content
//					this.getUrlContent(s);
//				}
//			}
//		}
//        //for xsd
//        else {
//             list = doc.getElementsByTagName("xs:import");
//            for (int i = 0; i < list.getLength(); i++) {
//    			
//            	Element el=  (Element) list.item(i);
//            	
//            	String s = el.getAttribute("schemaLocation");
//            	if(s!=null){
//            		this.getUrlContent(s);
//            	}
//            }
//
//		}
// }
//public static void main(String[] args) throws Exception {
//	getSchemasAndServiceRegexTester get = new getSchemasAndServiceRegexTester();
//	
//
//	String url = "http://192.22.10.18:7001/JerusalemBank/ADA_Services/proxy_services/ArchiveData?wsdl";
//	String url1 = "http://192.22.10.18:7001/JerusalemBank/InternetServices/proxy_services/ConsumerCredit?wsdl";
////	String url ="http://192.22.10.18:7001/JerusalemBank/ADA_Services/proxy_services/ArchiveData?SCHEMA%2FJerusalemBank%2FADA+Services%2FSchemas%2FArchiveData_XSD";
//	String output = get.getUrlContent(url1);
//    int count = 1;
//	for (String xml : schemaList) {
//		System.out.println(count);
//		System.out.println(xml);
////		getSchema.getSchema(xml);
//		count++;
//	}
//	
//	get.getServiceRegexes(url);
//}
//}