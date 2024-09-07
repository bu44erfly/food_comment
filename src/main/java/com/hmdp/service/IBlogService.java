package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);
    /**
     * 点赞博客
     *
     * @param id id
     * @return {@link Result}
     */
    Result likeBlog(Long id);
    /**
     * 查询博客点赞排行榜
     *
     * @param id id
     * @return {@link Result}
     */
    Result queryBlogLikesById(Long id);

    /**
     * 查询热门博客
     *
     * @param current 当前
     * @return {@link Result}
     */
    Result queryHotBlog(Integer current);

}
