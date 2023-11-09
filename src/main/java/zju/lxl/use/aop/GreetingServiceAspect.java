package zju.lxl.use.aop;
import zju.lxl.core.annotations.After;
import zju.lxl.core.annotations.Aspect;
import zju.lxl.core.annotations.Before;
import zju.lxl.core.annotations.Component;

import java.lang.reflect.Method;


@Aspect
@Component
public class GreetingServiceAspect {
	
	
	@Before(value = "zju.lxl.use.di.GreetingServiceImpl.greet")
	public void beforeAdvice(Method method, Object... args) {
		System.out.println("Before method:" + method);
	}
	

	@After(value = "zju.lxl.use.di.GreetingServiceImpl.greet")
	public void afterAdvice(Method method, Object... args) {
		System.out.println("After method:" + method);
	}
}