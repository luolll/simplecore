package zju.lxl.use.di;


import zju.lxl.core.annotations.Autowired;
import zju.lxl.core.annotations.Component;
import zju.lxl.core.annotations.PostConstruct;

@Component
public class GreetingServiceImpl implements GreetingService {

	@Autowired
	HelloWorld helloWorld;
	
	@PostConstruct
	public void post() {
		System.out.println("Greeting Service Impl is ready: " + helloWorld.hello());
	}

	@Override
	public void greet() {
		System.out.println("Simple greeting");
	}

}
