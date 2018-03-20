package stempler.ofer.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.Socket;
//import java.security.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.filters.StringInputStream;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/** * @author yosilev DEC-2015  777 */
public class GeneralUtils {

	
	private static DateFormat dateFormat     = new SimpleDateFormat("dd/MM/yyyy");
	private static DateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private static NumberFormat plainNumberFormatter = null;
	private static SimpleDateFormat date_time_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat date_format    = new SimpleDateFormat("yyyy-MM-dd");
	private static SimpleDateFormat time_format    = new SimpleDateFormat("HH:mm:ss");

	protected static Logger logger = Logger.getLogger(GeneralUtils.class );
	private static final String FS = System.getProperty("file.separator");

	//------------------------------------------------------------------------------
	public static String formatNumber(Number number) {
		if ( number == null ){
			return null;
		}
		if (plainNumberFormatter == null) {
			plainNumberFormatter = new DecimalFormat("############.#############");
		}
		return plainNumberFormatter.format(number);
	}
	//------------------------------------------------------------------------------
	public static boolean collectionEmptyOrNull(Collection collection) {
		return !collectionNotEmptyNotNull(collection);
	}
	public static boolean collectionNotEmptyNotNull(Collection collection) {
		return collection != null && !collection.isEmpty();
	}
	//------------------------------------------------------------------------------
	public static boolean mapNotEmptyNotNull(Map map) {
		return map != null && !map.isEmpty();
	}
	//------------------------------------------------------------------------------
	public static boolean stringNotEmptyNotNull(String string) {
		return string != null && string.trim().length() > 0;
	}
	//------------------------------------------------------------------------------
	public static boolean stringIsEmptyOrNull(String string) {
		return string == null || string.trim().length() == 0;
	}
	//------------------------------------------------------------------------------
	public <T> T[] subArray(Class<T> arrClass, T[] src, int fromIndex, int toIndex) {
		if (src == null) {
			throw new IllegalArgumentException("src is null");
		}
		if (fromIndex < 0) {
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		}
		if (toIndex > src.length) {
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		}
		T[] dest = (T[]) Array.newInstance(arrClass, toIndex - fromIndex);

		System.arraycopy(src, fromIndex, dest, 0, dest.length);

		return dest;
	}
	//--YL methods------------------------------------------------------------------
	//------------------------------------------------------------------------------
/**
 * 
 * @param - a Date with hour:min:sec 
 * @return - a Date with NO hour:min:sec
 */
	public static Date truncateDate(Date date){
		if ( date == null ){
			return null;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MILLISECOND);
		Date truncatedDate = calendar.getTime();
		return truncatedDate;
	}
	//------------------------------------------------------------------------------
	public static Date stringToDate(String startDateString){
//		String startDateString = "06/27/2007";
		if ( startDateString == null ){
			return null;
		}

	   DateFormat df = new SimpleDateFormat("dd-MM-yyyy"); 
	   Date startDate = null;
	   try {
	       startDate = df.parse(startDateString);
//	        String newDateString = df.format(startDate);
	   } catch (ParseException e) {
	   	logger.debug("startDateString Date String parameter should be in dd-MM-yyyy (no . or /");
	       e.printStackTrace();
	   }
	   return startDate;
	}
	//------------------------------------------------------------------------------
	public static String stringToDateAndtime(Date date) {
		if ( date == null ){
			return null;
		}
		
		return dateTimeFormat.format(date);
	}
	//------------------------------------------------------------------------------
	public static String dateToString(Date date){
//		String startDateString = "06/27/2007";
		if ( date == null ){
			return null;
		}

	   DateFormat df = new SimpleDateFormat("dd-MM-yyyy"); 
	   String stringDate = null;
	   try {
	       stringDate = df.format(date);
	       
//	        String newDateString = df.format(stringDate);
	   } catch (Exception e) {
	   	logger.debug("startDateString Date String parameter should be in dd-MM-yyyy (no . or /");
	       e.printStackTrace();
	   }
	   return stringDate;
	}
	//------------------------------------------------------------------------------
	public static Date timeStampToDate(Long timeStamp){
		if ( timeStamp == null ){
			return null;
		}
		 return new Date(timeStamp);		
	}
	//------------------------------------------------------------------------------
	public static Date addDays(Date origDate, int numOfDays){
		if ( origDate == null ){
			return null;
		}

		Calendar c = Calendar.getInstance(); 
		c.setTime(origDate); 
		c.add(Calendar.DATE, numOfDays);
		Date calcDate = c.getTime();
		return calcDate; 
	}
	//------------------------------------------------------------------------------
	// 
	public static String prepareKey(String reqUrl, String connMode) {
		if ( reqUrl == null || connMode == null ){
			return null;
		}

		return reqUrl + "-" + connMode;
	}
	//------------------------------------------------------------------------------
	// converts from json String to object..
	public static <T> T convertJsonStringToObject(String json, Class<?> clazz) {
		Exception exp = null;
		T targetObject = null;
		if ( json == null || clazz == null){
			return null;
		}

		try {
			org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
			objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES , false);
			targetObject = (T) objectMapper.readValue(new StringInputStream(json), clazz);
		} catch (JsonParseException e) {
			exp = e;
		} catch (JsonMappingException e) {
			exp = e;
		} catch (IOException e) {
			exp = e;
		}finally{
			if (exp != null){
				throw new RuntimeException("GeneralUtils.convertJsonStringToObject() - ERROR - Exception:[" + exp.getClass().getSimpleName() + "], msg:[" + exp.getMessage()+ "] while parsing serviceWall_MQ.json conf. file", exp);
			}
		}
		return targetObject;
	}
	//-----------------------------------------------------------------------------------------------------------------
	// converts from input Stream to object..
	public static <T> T convertInputStreamToObject(InputStream is, Class<?> clazz) {
		logger.debug("GeneralUtils.convertInputStreamToObject() - in");
		Exception exp = null;
		T targetObject = null;
		if ( is == null || clazz == null){
			return null;
		}

		try {
			org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
			objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES , false);
			targetObject = (T) objectMapper.readValue(is, clazz);
		} catch (JsonParseException e) {
			exp = e;
		} catch (JsonMappingException e) {
			exp = e;
		} catch (IOException e) {
			exp = e;
		}finally{
			if (exp != null){
				throw new RuntimeException("GeneralUtils.convertInputStreamToObject() - ERROR - Exception:[" + exp.getClass().getSimpleName() + "], msg:[" + exp.getMessage()+ "] while converting InputStream To json Object ", exp);
			}
		}
		return targetObject;
	}
	//-----------------------------------------------------------------------------------------------------------------
	// converts from json object to String..
	public static <T> String convertJsonObjectToString(T jsonObject) {
		Exception exp = null;
		String targetString = null;
		try {
			logger.info("GeneralUtils.convertJsonObjectToString() - 111");
			org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
			logger.info("GeneralUtils.convertJsonObjectToString() - 222");
			objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES , false);
			logger.info("GeneralUtils.convertJsonObjectToString() - 333");
			objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
			
			targetString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
		} catch (JsonParseException e) {
			exp = e;
		} catch (JsonMappingException e) {
			exp = e;
		} catch (IOException e) {
			exp = e;
		}finally{
			if (exp != null){
				throw new RuntimeException("GeneralUtils.convertJsonObjectToString() - ERROR - Exception:[" + exp.getClass().getSimpleName() + "], msg:[" + exp.getMessage()+ "] while parsing serviceWall_MQ.json conf. file", exp);
			}
		}
		return targetString;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static String composeConfFileFullPath(String confFileName){
/*		
		Properties prop = System.getProperties();
		Enumeration e = prop.keys();
		String key = null, val = null;
		while (e.hasMoreElements()){
			key = (String) e.nextElement();
			val = prop.getProperty(key);
			logger.debug( key  + " - " + val);
		}*/
		// NEXT: get current jar run directory:
		String decodedPath = getApplicationBaseDir();
		
		logger.debug("GeneralUtils.composeConfFileFullPath() - Application Base Dir:[" + decodedPath +"]");
//		String jsonConfFileEntirePath = decodedPath + FS + confFileName;
		String jsonConfFileEntirePath = confFileName;
		logger.debug("GeneralUtils.composeConfFileFullPath() - configuration file full path is:[" + jsonConfFileEntirePath + "]" );
		return jsonConfFileEntirePath;
	}
	//-----------------------------------------------------------------------------------------------------------------
    public static String getApplicationBaseDir() {

	   String path = new File(".").getAbsolutePath(); 

		return  path;
    }
    //-----------------------------------------------------------------------------------------------------------------
	public static boolean isNullOrSizeZero(Object o) throws RuntimeException{
		if (o == null){
			return true;
		}
		if (o.getClass().isPrimitive() || Number.class.isAssignableFrom(o.getClass())  || Boolean.class.isAssignableFrom(o.getClass())){
			throw new RuntimeException("GeneralUtils.isNullOrSizeZero() - does not accept primitives as " + o.getClass().getSimpleName().toLowerCase());
		}
		if (Collection.class.isAssignableFrom(o.getClass() )){
			return ((Collection<?>)o).size() == 0 ? true : false;
		}
		if (String.class.isAssignableFrom(o.getClass() )){
			return ((String)o).length() == 0 ? true : false;
		}
		if ( o.getClass().isArray() ){
			int len = Array.getLength(o);
			return len == 0 ? true : false;
		}
		return false;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static String getCurrDateStr(){
		String updateDateStr = null;
	    updateDateStr = date_format.format(new Date() );
		return updateDateStr;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static String getCurrTimeStr(){
		String updateDateStr = null;
	    updateDateStr = time_format.format(new Date() );
		return updateDateStr;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static String getCurrDateTime(){
		String updateDateStr = null;
	    updateDateStr = date_time_format.format(new Date() );
		return updateDateStr;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static boolean isReachable(String addr, int port){
		try (Socket s = new Socket(addr, port)){
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static boolean isJson(String jsonStr){
		ObjectMapper oMapper = new ObjectMapper();
		try {
			JsonNode node = oMapper.readTree(jsonStr);
			boolean cond1 = !node.isValueNode();
			boolean cond2 = jsonStr.matches("^\\{.*}$");
			return true && cond1 && cond2;
		} catch (IOException e) {
			return false;
		}
	}
	// ------------------------------------------------------------------------------------------
/*	public static void main(String[] args) {
		String addr = "127.0.0.1";
		int port = 27017;
		System.out.println(isReachable(addr, port) ? "Server is up" : "Server Connection Error");
	}*/	
}
