package org.example;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Ioc {
    private static final Logger logger = LoggerFactory.getLogger(Ioc.class);

    private Ioc() {}

    static MyClassInterface createMyClass() {
        InvocationHandler handler = new DemoInvocationHandler(new MyClassImpl());
        return (MyClassInterface)
                Proxy.newProxyInstance(Ioc.class.getClassLoader(), new Class<?>[] {MyClassInterface.class}, handler);
    }

    static class DemoInvocationHandler implements InvocationHandler {
        private final Object targetObject;

        DemoInvocationHandler(Object object) {
            this.targetObject = object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method originalMethod = findOriginalMethod(method, targetObject.getClass());
            if (originalMethod != null && originalMethod.isAnnotationPresent(Log.class)) {
                return logArgs(originalMethod, args);
            }
            return method.invoke(targetObject, args);
        }

        private Method findOriginalMethod(Method interfaceMethod, Class<?> implementationClass) {
            for (Method m : implementationClass.getMethods()) {
                if (m.getName().equals(interfaceMethod.getName())
                        && Arrays.equals(m.getParameterTypes(), interfaceMethod.getParameterTypes())) {
                    return m;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "DemoInvocationHandler{" + "myClass=" + targetObject + '}';
        }

        private Object logArgs(Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(targetObject, args);
                logger.info("Method '{}' called with arguments {}", method.getName(), Arrays.toString(args));
                return result;
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
