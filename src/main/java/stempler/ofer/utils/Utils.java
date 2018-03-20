package stempler.ofer.utils;


import lombok.extern.log4j.Log4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Random;

@Component
@Log4j
public class Utils {

	//-----------------------------------------------------------------------------------------------------------------
	public static void inititializeStrBuffToPaddedContent(StringBuffer content, String messageType) {
		log.debug("Adding [<"+messageType+">] tag as root elemnts.");
		String convertedContent = addTagsToContent(content.toString(), messageType);
		content.setLength(0);
		content.append(convertedContent);
	}
   //---------------------------------------------------------------------------------------------------------
	
    public static String addTagsToContent(String requestContent, String messageType) {

    	String tagStart = "<"  + messageType + ">";
	    String tagEnd   = "</" + messageType + ">";

	    if ( !requestContent.startsWith( tagStart )){
		   requestContent = tagStart + requestContent + tagEnd;
		   log.debug("Successfully added root element [<"+messageType+">]");
    	} else {
 		   log.debug("MessageContent already have root element, doing nothing");

    	}
	    return requestContent;
    }
//--------------------------------------------------------------------------------------------------------------------------------------	
	public static boolean isJSONValid(String test) {
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
//--------------------------------------------------------------------------------------------------------------------------------------	
		public static String getXPath(Node node){
			Node parent = node.getParentNode();
			if(parent ==null){
				return node.getNodeName();
			}
			return getXPath(parent) + "." +node.getNodeName() ;
		}
//--------------------------------------------------------------------------------------------------------------------------------------	
	
	public void convertPdfToImage(File pdfFile, String saveToPath) throws Exception{
		PDDocument document = PDDocument.load(pdfFile);
		PDFRenderer renderer = new PDFRenderer(document);
		String fileName = pdfFile.getName();
		for (int pageNum=0; pageNum< document.getNumberOfPages(); pageNum++) {
			String pageFileName = saveToPath + fileName + "_" + pageNum;
			File outputfile= new File(pageFileName);
			BufferedImage image =  renderer.renderImageWithDPI(pageNum, 300);
			ImageIOUtil.writeImage(image, outputfile+".png", 300);
//			ImageIOUtil.writeImage(image, outputfile+".jpeg", 300);
//			ImageIOUtil.writeImage(image, outputfile+".gif", 300);
		}
		document.close();
}

	public String identifyFileTypeUsingFilesProbeContentType(File file) {
		String tikaMeme = "";
		try {
			Tika tika = new Tika();
			tikaMeme = tika.detect(file.toPath());
//			System.out.println(tikaMeme);
//			String fileType = Files.probeContentType(file.toPath());
//			System.out.println(fileType);
		} catch (IOException ioException) {
			log.error("Could not retrieve meme Type");
		}
		return tikaMeme;
	}  
//--------------------------------------------------------------------------------------------------------------------------------------	
	
	public static String retrieveSoapBody(String xml) throws StringIndexOutOfBoundsException {
//		log.debug("Removing SOAP body");
		int startBodyPos=-2, startPos=-2, endPos=-2;
		String xmlNoSoap = "";
		String soapNS = "";
		String xmlLower = xml.toLowerCase();
		int soapEnvPos = xmlLower.indexOf("envelope");
		int soapNSPos = xmlLower.lastIndexOf("<", soapEnvPos);
        int soapEnvEndPos = xmlLower.indexOf(">",soapEnvPos+1); 
		
        try {
        	soapNS = xml.substring(soapNSPos+1, soapEnvPos-1);
	        startBodyPos = xmlLower.indexOf("<"+soapNS+":body");
	        if (startBodyPos == -1) {
	        	log.debug("Could not find regular SOAP env prexif of 'soapenv:body' in xml. Looking for starting position of 'soap:Body'");
	        	startBodyPos = xmlLower.indexOf("<soap:body");
	        	if(startBodyPos == -1){
	        		log.error("Could not find alternative SOAP env prefix to start removing. Failing xml");
	        	} else {
	        		log.debug("Successfully found starting position of 'soap:Body'");
	        	}
	        }
	        //startPos = xmlLower.indexOf(">",startBodyPos)+1;
	        startPos = xmlLower.indexOf("<",startBodyPos+1);
	        endPos = xmlLower.indexOf("</"+soapNS+":body>");
	        if (endPos == -1) {
	        	log.debug("Could not find regular SOAP env prexif of 'soapenv:body' in xml. Looking for ending position of 'soap:Body'");
	        	endPos = xmlLower.indexOf("</soap:body>");
	        	if(startBodyPos == -1){
	        		log.error("Could not find alternative SOAP env end prefix to start removing. Failing xml");
	        	} else {
	        		log.debug("Successfully found ending position of 'soap:Body'");
	        	}
	        }
	        if ((startPos >= endPos) || (startPos == 0) || (endPos == -1)) {
	        	throw new StringIndexOutOfBoundsException();
		    }
	        xmlNoSoap = xml.substring(startPos, endPos);
	        // loop for all namespaces
	        int xmlnsPos = xmlLower.indexOf("xmlns:");
	        int quotesFirstPos, quotesSecondPos;
	        String currentNS;
	        String addNS = " ";
	        
	        while (xmlnsPos != -1 &&  xmlnsPos < soapEnvEndPos) {
	        	if (xmlnsPos != xmlLower.indexOf("xmlns:"+soapNS)) {
	        		quotesFirstPos = xmlLower.indexOf("\"", xmlnsPos+1);
	        		quotesSecondPos = xmlLower.indexOf("\"", quotesFirstPos+1);
	        		currentNS = xml.substring(xmlnsPos,quotesSecondPos+1); // Notice it's from xml and not xmlLower
	        		//added by Ofer. S
	        		
	        		if(xmlNoSoap.contains(currentNS)){
	        			log.debug("Found duplicated name space in request. Not passing it to RequestChannel. duplicated name space: [" +currentNS+"]");
	        		}else {
		        	addNS = addNS + currentNS +" ";

	        		}
	        	}
	        	xmlnsPos =  xmlLower.indexOf("xmlns:",xmlnsPos+1);
	        }
	        
//	    	xmlNoSoap = xml.substring(startPos, endPos);
	    	int closeTagPos = xmlNoSoap.indexOf(">");
	    	xmlNoSoap = xmlNoSoap.substring(0,closeTagPos) + addNS + xmlNoSoap.substring(closeTagPos);
	    	
		} catch (StringIndexOutOfBoundsException e) {
			throw new StringIndexOutOfBoundsException("error. Wrong soap boundries. start: "+startPos+". end: "+endPos+". soapNS: "+soapNS);
		}
//		log.debug("Successfully removed SOAP body");
        return xmlNoSoap;
	}
	
//--------------------------------------------------------------------------------------------------------------------------------------	
	
	public static  Document buildXML(String xmlString) throws Exception {
		
		Document doc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc =  builder.parse(new InputSource(new StringReader(xmlString)));
		return doc;
	}
	//--------------------------------------------------------------------------------------------------------------------------------------	

	public static  int maxDepth(Node node) {
		int max = 0;
		NodeList kids = node.getChildNodes();
		if (kids.getLength() == 0) {
			return 0;
		}
		
		for (int i=0; i < kids.getLength(); i++) {
			int kidMax = maxDepth(kids.item(i));
			if (kidMax > max) {
				max = kidMax;
			}
		}
		
		return (max+1);
	}
//--------------------------------------------------------------------------------------------------------------------------------------	
		
	
	public  boolean isEmpty(String str) {
		if(str == null)
			return true;
		else if(str.length() == 0)
			return true;
		else
			return false;
	}

	//generates a random String for guid use
	public static String StringGenerator(){
		char[] chars = "0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 12; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		String output = sb.toString();
		return output;
	}
	
}
