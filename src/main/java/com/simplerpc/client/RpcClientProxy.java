package com.simplerpc.client;

import com.simplerpc.common.annotation.RpcReference;
import com.simplerpc.proxy.RpcProxyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;

/**
 * RPC客户端代理，用于自动注入远程服务引用
 */
@Slf4j
public class RpcClientProxy implements ApplicationContextAware, InitializingBean, DisposableBean {

    private final RpcProxyFactory proxyFactory;

    private ApplicationContext applicationContext;

    /**
     * 构造函数
     *
     * @param proxyFactory RPC代理工厂
     */
    public RpcClientProxy(RpcProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 获取所有Spring容器中的Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            injectRpcReference(bean);
        }

        log.info("RPC客户端代理初始化完成");
    }

    /**
     * 注入RPC引用
     *
     * @param bean Bean实例
     */
    private void injectRpcReference(Object bean) {
        Class<?> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 查找标注了@RpcReference的字段
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);

            if (rpcReference != null) {
                // 创建代理
                Object proxy = proxyFactory.create(
                        field.getType(),
                        rpcReference.version(),
                        rpcReference.timeout());

                try {
                    // 注入代理
                    field.setAccessible(true);
                    field.set(bean, proxy);

                    log.info("注入RPC引用: {}#{}", clazz.getName(), field.getName());
                } catch (IllegalAccessException e) {
                    log.error("注入RPC引用失败", e);
                }
            }
        }
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}
