package stempler.ofer.detectors;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Collections;

@Component
@Log4j
public class DetectorBeanLoader implements BeanPostProcessor {


	@Autowired
	private DetectorListHandler detectorListHandler;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if (bean instanceof stempler.ofer.detectors.Detector) {
			try {
				changeFieldsValue(bean);
			} catch (IllegalAccessException | RuntimeException e) {
				e.printStackTrace();
			}
		}
		return bean;
	}

	private void changeFieldsValue(Object bean) throws RuntimeException, IllegalAccessException {

		Detector detector = null;
		try {
			Annotation[] annotations = bean.getClass().getDeclaredAnnotations();
			for (Annotation annotation : annotations) {

				if (annotation.annotationType().equals(ValidatorType.class)) {
					detector = (Detector) bean;
					ValidatorType type = (ValidatorType) annotation;
					Integer position  = type.priority();
					detector.setPriority(position);
					
					detector.init();
					log.debug("initialized Detector [ " +detector.getClass().getSimpleName()+ "]");
					detectorListHandler.getDetectors().add(detector);
				}
			}
			Collections.sort(detectorListHandler.getDetectors());
		} catch (Exception e) {
			log.error("Could initilize Detectors: " + e);
			e.printStackTrace();
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName){
		return bean;
	}
}
