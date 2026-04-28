package com.video.controller;

import com.video.annotation.MyHeader;
import com.video.annotation.MyMapping;
import com.video.exception.BaseException;
import com.video.pojo.dto.Result;
import com.video.proxy.BeanFactory;
import com.video.utils.AuthHeaderUtil;
import com.video.utils.JSONUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.video.annotation.RequireRole;
import com.video.pojo.entity.User;
import com.video.utils.UserHolder;

@Slf4j
public abstract class BaseController extends HttpServlet {
    private final Map<String, Method> handlerMap = new HashMap<>();

    /**
     * 路由注册
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        BeanFactory.injectExternalObject(this);
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(MyMapping.class)) {
                MyMapping mapping = method.getAnnotation(MyMapping.class);
                String key = mapping.value() + ":" + mapping.method().toUpperCase();
                handlerMap.put(key, method);
                log.info("【路由映射成功】: {} -> {}", key, method.getName());
            }
        }
    }

    /**
     * 请求分发
     * @param req the {@link HttpServletRequest} object that contains the request the client made of the servlet
     *
     * @param resp the {@link HttpServletResponse} object that contains the response the servlet returns to the client
     *
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();
        String methodType = req.getMethod().toUpperCase();
        String key = path + ":" + methodType;

        Method handler = handlerMap.get(key);
        if (handler == null) {
            resp.getWriter().write(JSONUtil.toJson(Result.error("404 接口不存在")));
            return;
        }

        List<Part> partsToDelete = new ArrayList<>();
        try {
            //参数绑定
            Class<?>[] parameterTypes = handler.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            String bodyJson = null;

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                // 获取当前参数上的所有注解
                Parameter parameter = handler.getParameters()[i];
                // 优先处理 @MyHeader 注解
                if (parameter.isAnnotationPresent(MyHeader.class)) {
                    String headerName = parameter.getAnnotation(MyHeader.class).value();
                    String headerValue = req.getHeader(headerName);
                    args[i] = convertBasicType(headerValue, type);
                    continue;
                }

                if (type == HttpServletRequest.class) {
                    args[i] = req;
                } else if (type == HttpServletResponse.class) {
                    args[i] = resp;
                } else if (type == Part.class) {
                    String paramName = handler.getParameters()[i].getName();
                    Part part = req.getPart(paramName);
                    args[i] = part;
                    if (part != null) {
                        partsToDelete.add(part);
                    }
                } else if (isBasicType(type)) {
                    String paramName = handler.getParameters()[i].getName();
                    args[i] = convertBasicType(req.getParameter(paramName), type);
                } else {
                    if (bodyJson == null) {
                        bodyJson = req.getReader().lines().collect(java.util.stream.Collectors.joining());
                    }
                    args[i] = JSONUtil.toBean(bodyJson, type);
                }

                log.info("参数索引: {}, 尝试获取的参数名: {}, 实际获取的值: {}", i, parameter.getName(), args[i]);
            }

            // 鉴权
            RequireRole requireRole = handler.getAnnotation(RequireRole.class);

            if (requireRole == null) {
                try {
                    Method originalMethod = this.getClass().getDeclaredMethod(handler.getName(), handler.getParameterTypes());
                    requireRole = originalMethod.getAnnotation(RequireRole.class);
                } catch (NoSuchMethodException ignored) {}
            }

            if (requireRole != null) {
                int requiredLevel = requireRole.value();
                User user = UserHolder.getUser();

                // 校验
                if (user == null || user.getRole() < requiredLevel) {
                    log.warn("权限不足拦截: 用户等级 {}, 接口要求等级 {}, 路径 {}",
                            user == null ? "未登录" : user.getRole(), requiredLevel, path);
                    resp.getWriter().write(JSONUtil.toJson(Result.error("权限不足，请联系香草管理员")));
                    return;
                }
            }

            //  反射调用
            handler.setAccessible(true);
            Object result = handler.invoke(this, args);

            if (result == null || resp.isCommitted()) {
                return;
            }

            Result finalResult;

            if (result instanceof Result) {
                finalResult = (Result) result;
                // 登录特殊处理：自动塞 Header
                if (finalResult.getCode() == 200 && finalResult.getData() instanceof String && "/login".equals(path)) {
                    resp.setHeader("Authorization", AuthHeaderUtil.buildBearerValue((String) finalResult.getData()));
                }
            } else {
                // 如果返回的是普通对象或 null，包一层 Result.success
                finalResult = Result.success(result);
            }

            resp.getWriter().write(JSONUtil.toJson(finalResult));

        } catch (Exception e) {
            // 统一处理异常
            Throwable cause = e.getCause(); // 获取 invoke 抛出的真实异常

            if (cause instanceof BaseException) {
                log.warn("业务异常 [{}]: {}", path, cause.getMessage());
                resp.getWriter().write(JSONUtil.toJson(Result.error(cause.getMessage())));
            } else {
                log.error("系统崩溃", e);
                log.error("系统崩溃 [{}]: ", path, cause);
                resp.getWriter().write(JSONUtil.toJson(Result.error("系统繁忙，请稍后再试")));
            }
        } finally {
            deleteParts(partsToDelete);
        }

    }

    private void deleteParts(List<Part> parts) {
        for (Part part : parts) {
            try {
                part.delete();
            } catch (Exception e) {
                log.warn("清理 multipart 临时文件失败: {}", part.getName(), e);
            }
        }
    }

    // 辅助方法：判断是否为基本数据类型
    private boolean isBasicType(Class<?> type) {
        return type == String.class || type == Integer.class || type == int.class ||
                type == Long.class || type == long.class || type == Boolean.class || type == boolean.class;
    }

    // 辅助方法：简单类型转换
    private Object convertBasicType(String value, Class<?> type) {
        if (value == null) return null;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        return value;
    }
}
