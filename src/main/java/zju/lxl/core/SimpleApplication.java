package zju.lxl.core;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import zju.lxl.core.annotations.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SimpleApplication {

    public static boolean ENABLE_LOG = true;
    private List<Object> beans;
    private Object[] proxyBeans;
    private CommandLineRunner runner;

    public static void run(Class main) {
        SimpleApplication app = new SimpleApplication();
        try {
            //创建bean
            app.createBeans(main);
            //解析aop的before和after,生成代理对象
            app.aop();
            //给所有的bean赋值
            app.di();
            //执行所有bean的带有post注解的方法
            app.post();
            log("成员变量初始化成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (app.runner != null) {
            app.runner.run();
        }
    }

    /**
     * 根据main类，去加载相应的Bean
     *
     * @param main
     * @return
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    private Stream<Class> loadClasses(Class main) throws MalformedURLException, ClassNotFoundException {
        URL resource = main.getResource("");
        File baseDir = new File(resource.getFile());

        //存放加载的目录队列
        Queue<File> dirs = new LinkedList<>();
        dirs.add(baseDir);
        int offset = main.getResource("/").getPath().length();

        Stream.Builder<Class> classesBuilder = Stream.builder();

        while (!dirs.isEmpty()) {
            //取出一个目录
            File tmp = dirs.poll();
            //遍历该目录下所有子目录里面所有的class
            for (File f : tmp.listFiles()) {
                if (f.isDirectory()) {
                    dirs.add(f);
                } else {
                    if (f.getName().endsWith(".class")) {
                        String clsName = f.toURI().toURL().getPath().substring(offset).replaceAll("/", ".").replace(".class", "");
                        //忽略所有的接口
                        if (!clsName.contains("zju.lxl.core")) {
                            Class cls = main.getClassLoader().loadClass(clsName);
                            log("load class: " + cls.getName());
                            classesBuilder.accept(cls);
                        }
                    }
                }
            }
        }
        return classesBuilder.build();
    }

    private void createBeans(Class main) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, MalformedURLException, ClassNotFoundException {
        //获取所有非接口的类, 并进行过滤, 只留下带有Component的对象
        beans = loadClasses(main).filter(cls ->
                Arrays.stream(cls.getAnnotations()).anyMatch(a -> a instanceof Component)
        ).map(cls -> {
            try {
                return cls.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());

        //留下第一个既有Component注解的, 又有CommandLineRunner接口的对象, 作为runner
        runner = (CommandLineRunner) beans.stream().filter(bean ->
                Arrays.stream(bean.getClass().getInterfaces()).anyMatch(i -> i.equals(CommandLineRunner.class))
        ).findFirst().orElse(null);
    }

    private void aop() {
        Stream.Builder<SimpleAop> SimpleAopsBuilder = Stream.builder();

        //遍历所有的Bean
        for (Object bean : beans) {
            //查看是否有Aspect注解
            boolean isAspect = Arrays.stream(bean.getClass().getAnnotations()).anyMatch(a -> (a instanceof Aspect));
            if (isAspect) {
                //遍历该Bean所有的方法
                for (Method m : bean.getClass().getMethods()) {
                    for (Annotation a : m.getAnnotations()) {
                        String pointCut = null;
                        AdviceEnum advice = null;
                        if (a instanceof Before) {
                            pointCut = ((Before) a).value();
                            advice = AdviceEnum.BEFORE;
                        }
                        if (a instanceof After) {
                            pointCut = ((After) a).value();
                            advice = AdviceEnum.AFTER;
                        }
                        if (pointCut != null) {
                            //将注解中的类名和方法名分开
                            int sep = pointCut.lastIndexOf(".");
                            String targetClass = pointCut.substring(0, sep);
                            String targetMethod = pointCut.substring(sep + 1);
                            SimpleAopsBuilder.accept(new SimpleAop(targetClass, targetMethod, bean, advice, m));
                        }
                    }
                }
            }
        }

        Map<String, Map<String, List<SimpleAop>>> clsMethodAopMapping = SimpleAopsBuilder.build().collect(Collectors.groupingBy(SimpleAop::getTarget,
                Collectors.groupingBy(SimpleAop::getTargetMethod)));

        proxyBeans = new Object[beans.size()];
        for (int i = 0; i < beans.size(); i++) {
            Object bean = beans.get(i);
            String clsName = bean.getClass().getName();
            if (clsMethodAopMapping.containsKey(clsName)) {
                Class[] interfaces = bean.getClass().getInterfaces();
                if (interfaces.length > 0 && bean.getClass().isInterface()) {
                    Object proxyInstance = Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                            interfaces, (proxy, method, args) -> {
                                String methodName = method.getName();
                                List<SimpleAop> aops = clsMethodAopMapping.get(clsName).get(methodName);
                                runAop(aops, AdviceEnum.BEFORE, method, args);
                                Object res = method.invoke(bean, args);
                                runAop(aops, AdviceEnum.AFTER, method, args);
                                return res;
                            });
                    proxyBeans[i] = proxyInstance;
                }else{
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(bean.getClass());
                    enhancer.setCallback(new MethodInterceptor() {
                        @Override
                        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                            String methodName = method.getName();
                            List<SimpleAop> aops = clsMethodAopMapping.get(clsName).get(methodName);
                            runAop(aops, AdviceEnum.BEFORE, method, args);
                            Object result = proxy.invokeSuper(obj, args);
                            runAop(aops, AdviceEnum.AFTER, method, args);
                            return result;
                        }
                    });
                    proxyBeans[i] = enhancer.create();
                }
            }
        }
    }

    private void runAop(List<SimpleAop> aops, AdviceEnum advice, Method m, Object... args) {
        if (aops != null) {
            aops.stream().filter(a -> a.getAdvice() == advice).forEach(aop -> {
                try {
                    aop.getMethod().invoke(aop.getAspect(), m, args);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private void di() throws IllegalArgumentException, IllegalAccessException {
        //遍历所有的Bean
        for (Object bean : beans) {
            //获取每个Bean的成员变量
            for (Field f : bean.getClass().getDeclaredFields()) {
                //如果成员变量是带Autowired注解
                if (Arrays.stream(f.getAnnotations()).anyMatch(a -> (a instanceof Autowired))) {
                    Class fCls = f.getType();
                    Object requiredBean;
                    //如果是接口，就根据Bean的父类接口判定来赋予对象
                    if (fCls.isInterface()) {
                        requiredBean = getRequiredInterfaceBean(fCls);
                    } else {
                        //如果不是接口，赋予cglib生成的代理对象
                        requiredBean = getRequiredBean(fCls);
                    }
                    //如果获取对象成功，就给成员变量赋值
                    if (requiredBean != null) {
                        f.setAccessible(true);
                        f.set(bean, requiredBean);
                        log(String.format("Field %s has annotation Autowired, execute di, %s",
                                f.toString(), requiredBean
                        ));
                    }
                }
            }
        }
    }

    private void post() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        log("Start Post Processes:");
        for (Object bean : beans) {
            for (Method m : bean.getClass().getMethods()) {
                if (Arrays.stream(m.getAnnotations()).anyMatch(a -> (a instanceof PostConstruct))) {
                    m.setAccessible(true);
                    m.invoke(bean);
                }
            }
        }
    }

    private Object getRequiredInterfaceBean(Class cls) {
        for (int i = 0; i < beans.size(); i++) {
            Object bean = beans.get(i);
            for (Class beanI : bean.getClass().getInterfaces()) {
                if (beanI.equals(cls)) {
                    return proxyBeans[i] != null ? proxyBeans[i] : bean;
                }
            }
        }
        return null;
    }

    private Object getRequiredBean(Class cls) {
        for (int i = 0; i < beans.size(); i++) {
            Object bean = beans.get(i);
            if (bean.getClass().equals(cls)) {
                return proxyBeans[i] != null ? proxyBeans[i] : bean;
            }
        }
        return null;
//        return beans.stream().filter(bean -> bean.getClass().equals(cls))
//                .findFirst().orElse(null);
    }

    private static void log(Object msg) {
        if (ENABLE_LOG) {
            System.out.println(msg);
        }
    }

}
