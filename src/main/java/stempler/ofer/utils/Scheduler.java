package stempler.ofer.utils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stempler.ofer.dao.AuditRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@EnableScheduling
@Component
public class Scheduler {
	private static Logger log  = Logger.getLogger(Scheduler.class);

	 
	
	@Autowired
	AuditRepository audirRepo;
	
	//Deleting Audit every day at 1:01 at night to remove all audit older than a week
	@Scheduled (cron = " 0 1 1 * * ?")
//	@Scheduled (fixedDelay = 2000)
	public void deleteOldAudit(){
		
		String lastweek = this.lastWeek();
		log.debug("Scheduler: Before the once a week Audit delete. Deleting all Audits older than a week ");
		
		audirRepo.deleteAuditByDateBefore(lastweek);
		
		log.debug("Scheduler: After the once a week Audit delete. Successfullt deleted all Audits older than a week ");
	}
	
	
	public String lastWeek(){
		
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		Date lastWeek = cal.getTime();
		String weekAgo = new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss").format(lastWeek);
		System.out.println(weekAgo);
		return weekAgo;
		
	}
	
//	public String yesterday(){
//		
//		final Calendar cal = Calendar.getInstance();
//		cal.add(Calendar.DATE, -1);
//		Date date = cal.getTime();
//		String yesterday = new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss").format(date);
//		System.out.println(yesterday);
//		return yesterday;
//		
//	}
	
}
