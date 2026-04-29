package com.video.utils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlSqlReaderUtil {
    // 缓存 SQL：Key 是 "namespace.id", Value 是 SQL 语句
    private static final Map<String, String> sqlMap = new HashMap<>();

    static {
        loadSql("mapper/UserMapper.xml");
        loadSql("mapper/VideoMapper.xml");
        loadSql("mapper/CommentMapper.xml");
        loadSql("mapper/FollowMapper.xml");
        loadSql("mapper/FavoriteMapper.xml");
        loadSql("mapper/CategoryMapper.xml");
        loadSql("mapper/CommentMentionMapper.xml");
        loadSql("mapper/NotificationMapper.xml");
    }

    private static void loadSql(String path) {
        try (InputStream is = XmlSqlReaderUtil.class.getClassLoader().getResourceAsStream(path)) {
            // 增加非空判断，防止路径写错导致 NullPointerException
            if (is == null) {
                System.err.println("错误：未找到 SQL 配置文件: " + path);
                return;
            }

            SAXReader reader = new SAXReader();
            Document doc = reader.read(is);
            Element root = doc.getRootElement();
            String namespace = root.attributeValue("namespace");

            //  获取 root 下的所有子元素（不管标签是 select, insert 还是 delete）
            List<Element> elements = root.elements();

            for (Element el : elements) {
                String id = el.attributeValue("id");
                String sqlText = el.getTextTrim();

                // 拼装全限定名作为 Key (例如: com.video.dao.UserDao.save)
                if (id != null && !id.isEmpty()) {
                    sqlMap.put(namespace + "." + id, sqlText);
                }
            }
            System.out.println("成功加载 XML 映射文件: " + path + "，共加载 SQL 条数: " + elements.size());

        } catch (Exception e) {
            System.err.println("解析 XML 文件失败: " + path);
            e.printStackTrace();
        }
    }

    public static String getSql(String fullId) {
        String sql = sqlMap.get(fullId);
        if (sql == null) {
            throw new RuntimeException("【ERROR】在 XML 中找不到对应的 SQL 配置，请检查 Key 是否正确: " + fullId);
        }
        return sql;
    }
}
