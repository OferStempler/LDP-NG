package stempler.ofer.mq;

import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import stempler.ofer.configuration.MQConfiguraion;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Big up to Israel M. who wrote is crazy piece of code


@Component
@Log4j
public class MQConnectionPool {
	
	@Autowired
	MQConfiguraion mqConfig;

	@Autowired
	MQPool pool;

	@PostConstruct
	public void load() {
		if ( "false".equals( mqConfig.getMqConnectionPoolLoadOnStartup() ) ){
			log.debug("connectionPoolLoadOnStartup is false, not loading MQ-Conn pool");
			return;
		}
		log.debug("connectionPoolLoadOnStartup is true, LOADING MQ-Conn pool..");
		// This helps opening as many connections possible in the pool as the
		// app starts.
		// Without this class, establishing a new connection will take waaaay
		// too long, and ain't nobody got time for that
		try {
			int maxConnections = Integer.valueOf(mqConfig.getMaxConnections());
			ExecutorService service = Executors.newFixedThreadPool(maxConnections);
			List<Callable<Integer>> list = new ArrayList<>();
			for (int i = 0; i < maxConnections; i++) {
				Callable<Integer> callable = new Callable<Integer>() {
					
					public Integer call() {
						MQQueueManager manager = null;
						try {
							Hashtable<String, Object> properties = new Hashtable<String, Object>();
							properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);
							properties.put(MQConstants.HOST_NAME_PROPERTY, mqConfig.getHost());
							properties.put(MQConstants.PORT_PROPERTY, new Integer(mqConfig.getPort()));
							properties.put(MQConstants.CHANNEL_PROPERTY, mqConfig.getChannel());
							manager = new MQQueueManager(mqConfig.getQmanager(), properties,
														 pool.getPool());
							

						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} finally {
							if (manager != null) {
								try {
									manager.disconnect();
								} catch (Exception e) {
									log.error(e.getMessage(), e);
								}
							}
						}
						return 0;
					}
				};
				
				list.add(callable);
			}
			
			service.invokeAll(list);
			log.debug("Initial connection pool esablished");
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
