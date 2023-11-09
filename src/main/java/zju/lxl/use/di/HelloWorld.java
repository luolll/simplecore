package zju.lxl.use.di;


import zju.lxl.core.annotations.Component;

@Component
public class HelloWorld {
	
	public String hello() {
		return "Hello world";
	}
}
