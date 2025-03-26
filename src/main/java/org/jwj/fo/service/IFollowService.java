package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.Follow;


public interface IFollowService extends IService<Follow> {

    Result follow(Long id, Boolean isFollow);

    Result isFollow(Long id);

    Result commonFollow(Long id);
}
