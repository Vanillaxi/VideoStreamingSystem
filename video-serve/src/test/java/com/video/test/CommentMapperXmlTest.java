package com.video.test;

import com.video.utils.XmlSqlReaderUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommentMapperXmlTest {
    @Test
    public void commentMapperSqlCanBeLoaded() {
        assertSqlExists("com.video.mapper.CommentMapper.insert");
        assertSqlExists("com.video.mapper.CommentMapper.updateRootId");
        assertSqlExists("com.video.mapper.CommentMapper.findParentForUpdate");
        assertSqlExists("com.video.mapper.CommentMapper.findRepliesByRootIds");
        assertSqlExists("com.video.mapper.CommentMapper.delete");
        assertSqlExists("com.video.mapper.CommentMapper.findPageByVideoIdHot");
        assertSqlExists("com.video.mapper.CommentMapper.findCursorPageByVideoIdTime");
        assertSqlExists("com.video.mapper.CommentMapper.findCursorPageByVideoIdHot");
        assertSqlExists("com.video.mapper.CommentMapper.updateReplyCount");
        assertSqlExists("com.video.mapper.CommentMapper.updateVideoCommentCount");
        assertSqlExists("com.video.mapper.FavoriteMapper.insertFavorite");
        assertSqlExists("com.video.mapper.FavoriteMapper.existsFavorite");
        assertSqlExists("com.video.mapper.FavoriteMapper.findByUserId");
        assertSqlExists("com.video.mapper.FavoriteMapper.countByUserId");
        assertSqlExists("com.video.mapper.FollowMapper.findFollowingRelations");
        assertSqlExists("com.video.mapper.FollowMapper.findFollowerRelations");
        assertSqlExists("com.video.mapper.FollowMapper.findFriendRelations");
        assertSqlExists("com.video.mapper.UserMapper.getByIds");
        assertSqlExists("com.video.mapper.UserMapper.getByUsernames");
        assertSqlExists("com.video.mapper.CategoryMapper.listEnabled");
        assertSqlExists("com.video.mapper.CommentMentionMapper.insert");
        assertSqlExists("com.video.mapper.NotificationMapper.insert");
        assertSqlExists("com.video.mapper.VideoMapper.updateFavoriteCount");
        assertSqlExists("com.video.mapper.VideoMapper.getByIds");
        assertSqlExists("com.video.mapper.VideoMapper.existsLike");
        assertSqlExists("com.video.mapper.VideoMapper.incrementViewCount");
        assertSqlExists("com.video.mapper.VideoMapper.updateCommentCount");
        assertSqlExists("com.video.mapper.VideoMapper.getVideoPageByTitleHot");
        assertSqlExists("com.video.mapper.VideoMapper.getVideoPageByCategoryId");
        assertSqlExists("com.video.mapper.VideoMapper.getVideoPageByCategoryIdHot");
        assertSqlExists("com.video.mapper.VideoMapper.getHotTop50");
        assertSqlExists("com.video.mapper.VideoMapper.getHotCursorPage");
        assertSqlExists("com.video.mapper.VideoMapper.getTimeCursorPage");
        assertSqlExists("com.video.mapper.VideoMapper.getNewestPage");
        assertSqlExists("com.video.mapper.VideoMapper.getVideoCount");
    }

    private void assertSqlExists(String key) {
        String sql = XmlSqlReaderUtil.getSql(key);
        assertNotNull(sql);
        assertTrue(sql.length() > 10);
    }
}
