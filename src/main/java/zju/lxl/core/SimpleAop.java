package zju.lxl.core;


import java.lang.reflect.Method;

public class SimpleAop {
	private final String targetClass;
	private final String targetMethod;
	private final Object aspect;
	private final AdviceEnum advice;
	private final Method method;
	public SimpleAop(String targetClass, String targetMethod, Object aspect, AdviceEnum advice, Method method) {
		super();
		this.targetClass = targetClass;
		this.targetMethod = targetMethod;
		this.aspect = aspect;
		this.advice = advice;
		this.method = method;
	}
	
	public String getTarget() {
		return targetClass;
	}
	public String getTargetMethod() {
		return targetMethod;
	}
	public Object getAspect() {
		return aspect;
	}
	public AdviceEnum getAdvice() {
		return advice;
	}
	public Method getMethod() {
		return method;
	}

}
