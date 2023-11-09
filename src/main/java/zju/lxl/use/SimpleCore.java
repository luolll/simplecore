package zju.lxl.use;


import zju.lxl.core.CommandLineRunner;
import zju.lxl.core.SimpleApplication;
import zju.lxl.core.annotations.Autowired;
import zju.lxl.core.annotations.Component;
import zju.lxl.use.di.GreetingService;
import zju.lxl.use.di.GreetingServiceImpl;

//标识当前类对象的生命周期是由容器管理
@Component
public class SimpleCore implements CommandLineRunner {
	//标识当前成员变量由容器自动装配合适对象
	@Autowired
	private GreetingService greetingService;

	@Autowired
	private GreetingServiceImpl greetingServiceI;


	public static void main(String[] args) {
		SimpleApplication.ENABLE_LOG = true;
		SimpleApplication.run(SimpleCore.class);
	}


	@Override
	public void run() {
		System.out.println("Now the application is running");
		System.out.println("This is my spring");
		greetingServiceI.greet();
		greetingService.greet();
	}

}
