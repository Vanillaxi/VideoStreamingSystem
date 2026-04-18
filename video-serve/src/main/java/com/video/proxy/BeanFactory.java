package com.video.proxy;

import lombok.extern.slf4j.Slf4j;
import com.video.annotation.MyAutowired;
import com.video.annotation.MyComponent;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BeanFactory {
    private static final Map<String, Object> beanMap = new HashMap<>();

    static {
        try {
            log.info("beanFactory 启动...");
            scanPackages("com.video");
            executeInjection();

            wrapAOP();
            log.info("初始化成功");
        } catch (Exception e) {
            log.error("初始化失败", e);
        }
    }

    private static void scanPackages(String packageName) throws Exception {
        String path = packageName.replace(".", "/");
        // 使用 getResources 获取 Classpath 下所有匹配的路径（包含 main 和 test）
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            File dir = new File(url.getFile());
            doScan(dir, packageName);
        }
    }

    private static void doScan(File dir, String packageName) throws Exception {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // 递归扫描子包
                doScan(new File(dir, file.getName()), packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(MyComponent.class)) {
                    String beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1);
                    if (!beanMap.containsKey(beanName)) {
                        // 这里只创建原始实例
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        beanMap.put(beanName, instance);
                        log.info("【成功加载原始 Bean】: {} -> {}", beanName, className);
                    }
                }
            }
        }
    }


    public static void executeInjection() {
        for (Object bean : beanMap.values()) {
            // 获取原始类（防止因为已经是代理对象而拿不到字段）
            Class<?> clazz = bean.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(MyAutowired.class)) {
                    // 1. 获取需要注入的接口类型
                    Class<?> fieldType = field.getType();
                    // 2. 从 beanMap 中寻找实现类（根据类型或名称）
                    Object targetBean = findBeanByType(fieldType);

                    if (targetBean != null) {
                        try {
                            field.setAccessible(true);
                            field.set(bean, targetBean); // 执行注入
                            log.info("【注入成功】: {} -> {}", field.getName(), clazz.getSimpleName());
                        } catch (IllegalAccessException e) {
                            log.error("注入失败", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 专门为外部对象（如 Servlet）提供依赖注入
     * 即使该对象不在 beanMap 中，也能扫描其带有 @MyAutowired 的字段并赋值
     */
    public static void injectExternalObject(Object obj) {
        if (obj == null) return;

        // 获取对象的类（注意：如果是 Servlet 容器产生的实例，直接使用 getClass 即可）
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 1. 只处理带有 @MyAutowired 注解的字段
            if (field.isAnnotationPresent(MyAutowired.class)) {
                // 2. 获取字段类型，并从 beanMap 中查找现有的 Bean（通常是代理后的 Service）
                Class<?> fieldType = field.getType();
                Object targetBean = findBeanByType(fieldType);

                if (targetBean != null) {
                    try {
                        field.setAccessible(true);
                        // 3. 将 BeanFactory 管理的实例塞进这个外部对象的字段里
                        field.set(obj, targetBean);
                        log.info("【外部注入成功】: 字段 {} -> 类 {}", field.getName(), clazz.getSimpleName());
                    } catch (IllegalAccessException e) {
                        log.error("【外部注入失败】: 字段 {}", field.getName(), e);
                    }
                } else {
                    log.warn("【注入警告】: 未能在 BeanFactory 中找到类型为 {} 的 Bean", fieldType.getSimpleName());
                }
            }
        }
    }

    /**
     * 实现内部注入（如BeanMap里的Bean)
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T getBean(Class<T> clazz) {
        for (Object bean : beanMap.values()) {
            if (clazz.isAssignableFrom(bean.getClass())) {
                return (T) bean;
            }
        }
        throw new RuntimeException("未找到类型为 " + clazz.getName() + " 的 Bean");
    }

    private static void wrapAOP() {
        Map<String, Object> proxies = new HashMap<>();
        for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
            Object bean = entry.getValue();
            // 确保是实现类且不是代理对象本身
            if (bean.getClass().getName().contains(".service.impl")) {
                Object proxyBean = com.video.proxy.ServiceProxy.createProxy(bean);
                proxies.put(entry.getKey(), proxyBean);
            }
        }
        // 统一替换
        beanMap.putAll(proxies);
        log.info("【AOP】所有 Service 已成功替换为代理对象");
    }

    private static Object findBeanByType(Class<?> fieldType) {
        for (Object bean : beanMap.values()) {
            // 判断 bean 是否是 fieldType 的实现类或子类
            if (fieldType.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }
        log.error("【注入失败】未找到类型为 {} 的实现类", fieldType.getName());
        return null;
    }

    public static void init() {
        // 静态块会在第一次访问该类时执行
        log.info("BeanFactory 初始化接口被调用...");
    }
}