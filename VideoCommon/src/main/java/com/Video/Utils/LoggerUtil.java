package com.Video.Utils;

import java.util.logging.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final Logger rootLogger = Logger.getLogger("com.video");

    static {
        try {
            //设置全局日志级别
            rootLogger.setLevel(Level.ALL);

            //创建控制台处理器
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);

            //自定义格式化器
            consoleHandler.setFormatter(new Formatter() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [%-7s] [%s] - %s%n",
                            formatter.format(LocalDateTime.now()),
                            record.getLevel().getLocalizedName(),
                            record.getSourceClassName(),
                            record.getMessage());
                }
            });

            //添加自定义处理器
            rootLogger.setUseParentHandlers(false);
            rootLogger.addHandler(consoleHandler);

        } catch (Exception e) {
            System.err.println("LoggerUtil初始化失败: " + e.getMessage());
        }
    }

    // 获取特定类的Logger，方便在各个类中使用
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    // 封装了 Level.INFO 等级的输出
    public static void info(Class<?> clazz, String msg) {
        getLogger(clazz).info(msg);
    }

    //封装了 Level.WARNING 等级
    public static void warn(Class<?> clazz, String msg) {
        getLogger(clazz).warning(msg);
    }

    public static void error(Class<?> clazz, String msg, Throwable e) {
        getLogger(clazz).log(Level.SEVERE, msg, e);
    }
}