package stempler.ofer.utils;

import lombok.extern.log4j.Log4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by ofer on 26/06/17.
 */

@Log4j
@Component
public class XmlToJson {
	

	
	
	   private List<String> contentList;
	   private List<String> base64List;
	   private final String HIDDEN = "**Hidden**";
	   private final String Base64 = "Base64";
	   
//	   @PostConstruct
//	    public void init()  {
//	    	 contentList =  contentFromYML.getList();
//	    	 base64List = base64ToHide.getList();
//	    }
	
	

	private static String noBase64 = null;
	private static String wantedJson = null;
	private static String noHiddens = null;
	private static String finalXML = null;
	
	public boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}
	// Hides wanted content in XML tags according to YML
	public String hideContent(String xml) {
		String returnXml = null;
		JSONObject datatoJson = XML.toJSONObject(xml);
		if (contentList == null) {
			log.debug("No content needs to be hidden");
			return null;
		} else {
			for (String content : contentList) {
				content.trim();

				if (noHiddens == null) {
					finalXML = this.hideSpecificField(datatoJson, xml, content);
				} else {
					finalXML = this.hideSpecificField(datatoJson, noHiddens, content);

				}
			}
			if (noHiddens != null) {

				returnXml = noHiddens;
				noHiddens = null;
				log.debug("classified information was found and replaced with  [" + HIDDEN + "] ");
				return returnXml;
			} else {
			}
			log.debug("No classified information was found");
			return returnXml;
		}
	}


	public String hideSpecificField(JSONObject jo, String original, String wantedContent) {
		for (Object o : jo.keySet()) {
			
			if (jo.get(o.toString()) instanceof JSONObject) {
				
				hideSpecificField(jo.getJSONObject(o.toString()), original, wantedContent);
			} else {
				Object ob = jo.get(o.toString());
				String s = ob.toString();
				if (o.toString().equals(wantedContent)) {
					System.out.println(s);
					String replaced = original.replace(s, HIDDEN);
					// System.out.println(replaced);
					noHiddens = replaced;
					return replaced;
				}
			}
		}
		return null;
	}
	

//	public String iterateJson(JSONObject jo, String original) {
//
//		for (Object o : jo.keySet()) {
//			if (jo.get(o.toString()) instanceof JSONObject) {
//				iterateJson(jo.getJSONObject(o.toString()), original);
//
//			} else {
//				Object ob = jo.get(o.toString());
//				String s = ob.toString();
////				System.out.println(s);
////				boolean isBase64 = Base64.isBase64(s);
////				System.out.println(o.toString());
//				if (o.toString().equals(CONTENT)) {
//				if (s.length() > 2000) {
//					log.debug("Base64 content was found");
//					
//					// System.out.println(rounds);
//					// System.out.println(o);
//					// System.out.println(jo.keySet());
//
//					String replaced = original.replace(s, "Base 64");
////					 System.out.println(replaced);
//					noBase64 = replaced;
//				}
//				}
//			}
//		}
//
//		return noBase64;
//
//	}

	public String getSpecificJson(String StringXml, String wantedContent) {

		String content = null;
		wantedJson = null;
		try {
			JSONObject DatatoJson = XML.toJSONObject(StringXml);
			// System.out.println(DatatoJson);
			content = this.iterateSpecificJson(DatatoJson, StringXml, wantedContent);
		} catch (Exception e) {
			log.error("Error getting StringXml content");
			e.printStackTrace();
		}
		if (wantedJson == null || wantedJson.equals("")) {
			log.debug("No wanted Content was found for [" + wantedContent + "]");
			return null;
		} else {
			log.debug("Found wanted content for [" + wantedContent + "]");
			return wantedJson;
		}

	}

	public String iterateSpecificJson(JSONObject jo, String original,
			String wantedContent) {
		for (Object o : jo.keySet()) {

			if (jo.get(o.toString()) instanceof JSONObject) {
				iterateSpecificJson(jo.getJSONObject(o.toString()), original, wantedContent);

			} else {
				Object ob = jo.get(o.toString());
				String s = ob.toString();
				
//				System.out.println(jo.get(o.toString()));
//				System.out.println(o.toString());
				if (o.toString().equals(wantedContent)) {
					
//					System.out.println(s);
					wantedJson = s;
					return wantedJson;
				}

			}
		}

		return null;

	}

	public String getTags(String original) {

		boolean change = false;
		String newxml = null;
		List<String> base64 = base64List;
		try {
			Document doc = Utils.buildXML(original);

			for (String stringToHide : base64) {

				NodeList filelist = doc.getElementsByTagName(stringToHide);
				// System.out.println(filelist.getLength());
				for (int j = 0; j < filelist.getLength(); j++) {

					NodeList subList = filelist.item(j).getChildNodes();
					for (int i = 0; i < subList.getLength(); i++) {

						Node current = subList.item(i);
						if (current.getTextContent().toString().length() > 1000) {
							current.setNodeValue(Base64);
							change = true;
						}
					}
				}
			}
			if (change) {
				log.debug("Base64 content was found and replaced");
				newxml = this.docToString(doc);
			} else {
				log.debug("No base64 content was found");

			}
			// System.out.println(newxml);
		} catch (Exception e) {
			log.debug("Could not get base 64 " + e.getMessage());
		}

		return newxml;
	}
	
	public String docToString(Document doc){
		TransformerFactory tf  = TransformerFactory.newInstance();
		Transformer transformer;
		String output ="";
		try {
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			 output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		} catch (Exception e) {
			log.error("Could not build back string from XML doc. " + e);
		}

		return output;
	}

//	public static void main(String[] args) {
//		xmlToJson xml = new xmlToJson();
//
//		try {
//			String data = new String(Files.readAllBytes(Paths
//		    .get("C:/Users/ofers/Desktop/LDP related/ldpTests/testBase1.txt")));
////			.get("C:/Users/ofers/Desktop/LDP related/test6.txt")));
////			String out = xml.hideContent(data);
//			String out = xml.getIfBase64(data);
//		
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	    
//		
//	}

}
