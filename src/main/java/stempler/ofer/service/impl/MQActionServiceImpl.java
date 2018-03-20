package stempler.ofer.service.impl;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import stempler.ofer.configuration.MQConfiguraion;
import stempler.ofer.model.*;
import stempler.ofer.model.entities.Service;
import stempler.ofer.mq.MQPool;
import stempler.ofer.service.AuditService;
import stempler.ofer.service.MQActionService;
import stempler.ofer.utils.Utils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component
public class MQActionServiceImpl implements MQActionService {

	private final static String SOAP_SUFIX = "</soap:Body></soap:Envelope>";
	private final static String SOAP_PREFIX = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><soap:Body>";
	
	private static Logger log  = Logger.getLogger(MQActionServiceImpl.class);
	

    private Map<String, String> valueMap = new HashMap<String, String>();  
    private Integer             timeout = 0;
    private int                 deliveryMode = 0;
    private String              requestQueueName = null;
    private String              replyQueueName = null;
    private Integer             expiry = null;
    private Service             service = null;
    private String              serviceName = null;
    private String              message = null;
    private String              serviceType = null;
    
    public final static String REQUESTREPLY = "RequestReply";
    public final static String DATAGRAM = "Datagram";
//    private final String MSGTYPE_RESPONSE = "Response";

    @Autowired
    private MQConfiguraion mqConfig;
    
    @Autowired
    private MQPool pool;
    
    @Autowired
    private Utils utils;
    

    
    @Autowired
    AuditService auditService;
        
    @PostConstruct
    public void init() {
    	valueMap = mqConfig.getMQEnvironmentMap();
    }
	//-----------------------------------------------------------------------------------------------------------------
	@Override
	public MQldpResponse execute(ExtendedService extendedService, String content, String guid) {
		service     = extendedService.getService();
		serviceName = service.getServiceName();
        message     = content;
        log.debug("loaded service to MQActions [ " + serviceName + " ]");
    	
		serviceType = service.getServiceType();
		if (REQUESTREPLY.equals(serviceType)) {
			return requestReply(extendedService, guid);
		} else if (DATAGRAM.equals(serviceType)) {
			return datagram(service);
		} else {
			log.error("No Service type found for service [" + serviceName + "]");
			MQldpResponse res = new MQldpResponse();
			res.setErrorCode(MQldpResponse.ERROR_CODE_SYS_ERROR);
			res.setErrorDesc("No Service type found for '" + service + "'.");
			return res;
		}
	}
	//-----------------------------------------------------------------------------------------------------------------
    private void setMQVars(Service service) {
    	//set queues names according to environment as written in properties map
    	String originalRequestQueueu = service.getRequestQueue();
    	String originalReplyQueue = service.getReplyQueue();

    	StrSubstitutor sub = new StrSubstitutor(valueMap);
    	String requestQueue= sub.replace(originalRequestQueueu);
    	String replyQueue= sub.replace(originalReplyQueue);

        this.requestQueueName = requestQueue;
        this.replyQueueName = replyQueue;

        if (utils.isEmpty(requestQueueName)) {
            log.error("No request queue defined for service [" + serviceName + "]");
            return;
        }

        if (REQUESTREPLY.equals(serviceType) && utils.isEmpty(replyQueueName)) {
            log.error("No reply queue for service [" + serviceName + "]");
            return;

        }

        expiry = service.getExpiry();

        if (expiry == null ) {
            log.error("No reply expiry defined for service '" + service + "'.");
            return;
        } else {

            try {         
                    log.debug("expiry is [" + expiry + "] for service [" + serviceName + "]");

            } catch (Exception e) {
                log.error("Invalid expiry [" + expiry + "] for service [" + serviceName + "]");
                return;
            }

        }//else

       Integer persistence = service.getPersistence();
       
        if (persistence == null) {
            log.error("No reply persistence defined for service [" + serviceName + "]");
            return;

        } else {
            	if(persistence == 1){
                this.deliveryMode = MQConstants.MQPER_PERSISTENT;
               
                    log.debug("persistence is true for service  [" + serviceName + "]");;
                
            } else {
                this.deliveryMode = MQConstants.MQPER_NOT_PERSISTENT;
               
                    log.debug("persistence is false for service  [" + serviceName + "]");
                
            }
        }

         timeout = service.getTimeOut();
        if (timeout == null || timeout == 0) {

            log.error("No reply timeoutString defined for service [" + serviceName + "]");

            return;
        } else {

            try {
              
                    log.debug("timeout is [" + timeout + "] for service [" + serviceName + "]");
                
            } catch (Exception e) {
                log.error("Invalid timeout [" + timeout + "] for service [" + serviceName + "]");
                return;

            }

        }
    }
	//-----------------------------------------------------------------------------------------------------------------
    private MQldpResponse datagram(Service service) {

        MQQueueManager manager = null;
        MQQueue queueReq = null;
        MQldpResponse status = new MQldpResponse();
        status.setErrorCode(MQldpResponse.ERROR_CODE_OK);
        status.setErrorDesc("Success");
        setMQVars(service);

        try {

            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);
            properties.put(MQConstants.HOST_NAME_PROPERTY,(Object) mqConfig.getHost());

            properties.put(MQConstants.PORT_PROPERTY, new Integer(mqConfig.getPort()));
            properties.put(MQConstants.CHANNEL_PROPERTY, mqConfig.getChannel());

            manager = new MQQueueManager(mqConfig.getQmanager(), properties, pool.getPool());
            
                log.debug("Connected to queue manager " + mqConfig.getQmanager() + ".");
                log.debug("Openning queue [" + requestQueueName +  "]");

            queueReq = manager.accessQueue(requestQueueName, MQConstants.MQOO_OUTPUT +
                    MQConstants.MQOO_FAIL_IF_QUIESCING);

           
            log.debug("Queue [" + replyQueueName + "] is open.");
            

            MQMessage messageReq = new MQMessage();
            messageReq.format = MQConstants.MQFMT_STRING;
            messageReq.messageType = MQConstants.MQMT_DATAGRAM;
            messageReq.messageId = MQConstants.MQMI_NONE;
            messageReq.correlationId = MQConstants.MQCI_NONE;
            messageReq.characterSet = 1208;//UTF-8
            messageReq.writeString(message);

//            if (debug) {
//                log.debug("Sending message. Data: " + "["+ message+"]" );
//            }



//            check to see if there is no Base64 and no passwords in the message content.
//            xmlToJson parser = new xmlToJson();
//            String noHidden = null;
////            String noBase64 = parser.getIfBase64(message);
//            String noBase64 = parser.getTags(message);
//
//            if (noBase64!= null && !noBase64.equals("") ){
//                	
//                	noHidden = parser.hideContent(noBase64);
//                    log.debug("MQActions.datagram() - sending new message without Base64 and hidden contnet" );
//                    log.debug("MQActions.datagram() - Data: " + noHidden);
//                } else if (noBase64 ==null) {
//                	
//                	noHidden = parser.hideContent(message);
//                	if (noHidden != null) {
//                		log.debug("MQActions.datagram()- sending new message without hidden contnet");
//                		log.debug("MQActions.datagram()-  " + noHidden);
//                		
//                	} else {
//                		log.debug("MQActions.datagram()- sending new message without any changes");
//                		log.debug("MQActions.datagram()-  " + message);
//                	}
//                }
            

            MQPutMessageOptions optionsReq = new MQPutMessageOptions();
            optionsReq.options = MQConstants.MQPMO_FAIL_IF_QUIESCING;
            queueReq.put(messageReq, optionsReq);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            status.setErrorCode(MQldpResponse.ERROR_CODE_SYS_ERROR);
            status.setErrorDesc(e.getMessage());
        } finally {
            if (queueReq != null) {
                try {
                    queueReq.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (manager != null) {

                try {
                   
                        log.debug("Disconnecting from queue manager.");
                    
                    manager.disconnect();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return status;
    }
	//-----------------------------------------------------------------------------------------------------------------
    private MQldpResponse requestReply(ExtendedService extendedService, String guid) {

        MQQueueManager manager = null;
        MQQueue queueReq = null;
        MQQueue queueRpl = null;
        MQldpResponse status = new MQldpResponse();
        String error = "";
        status.setErrorCode(MQldpResponse.ERROR_CODE_OK);
        status.setErrorDesc("Success");
        setMQVars(service);

        try {
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);
            properties.put(MQConstants.HOST_NAME_PROPERTY,  mqConfig.getHost());
            properties.put(MQConstants.PORT_PROPERTY, new Integer(mqConfig.getPort()));
            properties.put(MQConstants.CHANNEL_PROPERTY, mqConfig.getChannel());
            manager = new MQQueueManager(mqConfig.getQmanager() ,properties, pool.getPool());
            
                log.debug("Connected to queue manager " +mqConfig.getQmanager() + ".");            
            queueReq = manager.accessQueue(requestQueueName,
                    MQConstants.MQOO_OUTPUT + MQConstants.MQOO_FAIL_IF_QUIESCING);         
                log.debug("Open queue " + requestQueueName + ".");            
            queueRpl = manager.accessQueue(replyQueueName, MQConstants.MQOO_INPUT_SHARED + MQConstants.MQOO_FAIL_IF_QUIESCING);     
                log.debug("Queue " + replyQueueName + " is open.");

        

            MQMessage messageReq = new MQMessage();
            messageReq.format = MQConstants.MQFMT_STRING;
            messageReq.messageType = MQConstants.MQMT_REQUEST;
            messageReq.messageId = MQConstants.MQMI_NONE;
            messageReq.correlationId = MQConstants.MQCI_NONE;
            messageReq.replyToQueueName = queueRpl.getName();
            messageReq.expiry = expiry;
            messageReq.persistence = deliveryMode;
            messageReq.characterSet = 1208;//UTF-8
            messageReq.writeString(message);
            

//            check for Base64 in the message content.
         
//            String noHidden = null;
////            String noBase64 = parser.getIfBase64(message);
//            String noBase64 = xmlToJsonParser.getTags(message); // parser
//
//            if (noBase64!= null && !noBase64.equals("") ){
//                	
//                	noHidden = xmlToJsonParser.hideContent(noBase64); // parser
//                    log.debug("sending new message without Base64 and hidden contnet" );
//                    log.debug("Data: " + noHidden);
//                    auditService.requestResponseBuildAudit(extendedService, noHidden, error, guid);
//                } else if (noBase64 ==null) {
//                	
//                	noHidden = xmlToJsonParser.hideContent(message); // parser
//                	if (noHidden != null) {
//                		log.debug("Sending new message without hidden contnet");
//                		log.debug(noHidden);
//                		 auditService.requestResponseBuildAudit(extendedService, noHidden, error, guid);
//                		
//                	} else {
//                		log.debug("Sending new message without any changes");
//                		log.debug(message);
//                		 auditService.requestResponseBuildAudit(extendedService, message, error, guid);
//                	}
//                }
            MQPutMessageOptions pmo = new MQPutMessageOptions();
            pmo.options = MQConstants.MQPMO_FAIL_IF_QUIESCING;
            queueReq.put(messageReq, pmo);
            byte[] messageId = messageReq.messageId;
            MQMessage messageRpl = new MQMessage();
            messageRpl.characterSet = 5601;//UTF-8
            messageRpl.messageId = MQConstants.MQMI_NONE;
            messageRpl.correlationId = messageId;
//            System.out.println("reply: " + messageId.toString());
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQConstants.MQGMO_WAIT + MQConstants.MQMO_MATCH_CORREL_ID;
            gmo.waitInterval = timeout;
            
                log.debug("Waiting for reply message from queue [" + replyQueueName + "]" +" (timeout=[" + this.timeout + "])");
            

            queueRpl.get(messageRpl, gmo);

           
                log.debug("Got reply.");
            

            byte[] replyBytes = new byte[messageRpl.getMessageLength()];     
            messageRpl.readFully(replyBytes);
            status.setResponseXML(new String(replyBytes));
            
        } catch (Exception e) {

            log.error("An error has occured: " + e.getMessage(), e);
            status.setErrorCode(MQldpResponse.ERROR_CODE_SYS_ERROR);
            status.setErrorDesc(e.getMessage());
        } finally {
            if (queueReq != null) {
                try {
                    queueReq.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            }
            if (queueRpl != null) {
                try {
                    queueRpl.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (manager != null) {
                try {
                    
                        log.debug("Disconnecting from queue manager.");
                    
                    manager.disconnect();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return status;

    }
	//-----------------------------------------------------------------------------------------------------------------
	public LdpResponse sendRequestToMQ(ExtendedService extendedService, String requestContent, String guid) throws Exception {
		Service service = extendedService.getService();
		MQldpRequest mqRequest = new MQldpRequest(serviceName, requestContent);
		MQldpResponse responseMessage = this.execute(extendedService, mqRequest.getXmlString(), guid);
		if (responseMessage == null) {
			return new LdpResponse(HttpStatus.CONFLICT, "After execute() - responseMessage from MQ is empty");
		}
		String replyString = responseMessage.getResponseXML();

		// Check if it's a RequestReply message
		if (responseMessage.getErrorCode() == MQldpResponse.ERROR_CODE_OK && REQUESTREPLY.equals(service.getServiceType())) {
			if (!StringUtils.isEmpty(replyString)) {

				LdpResponseExtended ldpResponseExtended = new LdpResponseExtended();
//				HttpHeaders httpHeaders = new HttpHeaders();
				LinkedMultiValueMap<String, String> newHeaders = new LinkedMultiValueMap<String , String>();

				newHeaders.set("Content-Type", "text/xml;charset=UTF-8");
				ldpResponseExtended.setHeaders(newHeaders);
				ldpResponseExtended.setResponseCode(HttpStatus.OK);
				ldpResponseExtended.setResponseMessage("SUCCESS");
				ldpResponseExtended.setContent(replyString);
				return ldpResponseExtended;

			} else {
				return new LdpResponse(HttpStatus.CONFLICT, "Response is empty or null");
			}
		} // END - RequestReply message type
		else {
			return new LdpResponse(HttpStatus.CONFLICT, "response Message Error Code:[" + responseMessage.getErrorCode()
					+ "] NE 0  Or  service.ServiceType:[" + service.getServiceType() + "] is not REQUESTREPLY");
		}
	} //sendRequestToMQ()
	//-----------------------------------------------------------------------------------------------------------------
}

