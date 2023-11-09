# 项目简介
Simple Spring Core
自己实现的简易版 Spring Core ,实现Spring的 IOC和AOP

代码分成两个部分，第一部分为简易版的spring core,为core包下；第二部分为core的使用，通过使用core包下的，实现Spring的IOC和AOP
    Core中包含两个部分，一个是简易版AOP,二是Spring加载过程
    加载分成3部分：
        第一部分： 查询所有@Component注解的类，进行加载,存入beans中
        第二部分： beans中查找所有@Aspect注解类，找到所有需要代理的类、方法，通过JDK动态代理和CGLIB创建bean的代理类，存入 proxyBeans 中
        第三部分： 遍历所有的bean, 给所有带有 @Autowired 的成员变量赋值，值为被代理的对象
    Use中分成两个部分，第一个是aop包，里面是GreetingServiceAspect的切面；第二个是测试用到的接口和类，所有的对象都是通过注解生成的
        SimpleCore为启动类。

    ![image](https://github.com/luolll/simplecore/assets/38280223/3e04c122-603c-4205-aae7-0106b94eb062)


    ![image](https://github.com/luolll/simplecore/assets/38280223/3edeaab7-988a-4a79-a8c2-6e2afc2dca0c)
