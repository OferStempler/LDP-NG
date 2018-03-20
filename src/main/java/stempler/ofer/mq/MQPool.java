package stempler.ofer.mq;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQSimpleConnectionManager;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import stempler.ofer.configuration.MQConfiguraion;

@Component
@Log4j
public class MQPool {
	
	@Autowired
	MQConfiguraion mqConfig;
	
	private   MQSimpleConnectionManager pool;

	private  Object mutex = new Object();
	
	public   MQSimpleConnectionManager getPool() {
		
		if (pool == null) {
			synchronized (mutex) {
				if (pool == null) {
					try {
						pool = new MQSimpleConnectionManager();
						pool.setActive(MQSimpleConnectionManager.MODE_ACTIVE);
						pool.setMaxConnections(Integer.valueOf(mqConfig.getMaxConnections()).intValue());
						pool.setMaxUnusedConnections(Integer.valueOf(mqConfig.getNumUnusedConnections()).intValue());
						pool.setTimeout(Integer.valueOf(mqConfig.getTimeout()));
						
						MQEnvironment.setDefaultConnectionManager(pool);
						
						log.info("Pool MaxConnections = " + pool.getMaxConnections());
						log.info("Pool MaxUnusedConnections = " + pool.getMaxUnusedConnections());
						log.info("Pool Timeout = " + pool.getTimeout());
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						pool = null;
					}
				}
			}
		}
		return pool;
	}
}
