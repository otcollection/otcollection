package com.otcollection;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Application {

	//private static final Logger log = Logger.getLogger(Applictaion.class);

	public static final String SERVICE_PROVIDER_XML = "spring.xml";

	private ClassPathXmlApplicationContext context;

	static Application instance = new Application();

	private Application() {
		context= new ClassPathXmlApplicationContext(
			new String[] {SERVICE_PROVIDER_XML});	
	}

	public static Application getInstance() {
		return instance;
	}

	public ClassPathXmlApplicationContext getContext() {
		return context;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getBean(String bean) {
		return (T) getInstance().getContext().getBean(bean);
	}
	
	/**
	 * Add by kevin:
	 * add getBean method with type
	 * */
	public static <T> T getBean(String bean, Class<T> type) {
		return getInstance().getContext().getBean(bean, type);
	}
}
