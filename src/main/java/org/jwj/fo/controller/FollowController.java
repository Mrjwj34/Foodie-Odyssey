package org.jwj.fo.controller;


import org.jwj.fo.dto.Result;
import org.jwj.fo.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable Long id, @PathVariable Boolean isFollow) {
        return followService.follow(id, isFollow);
    }
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable Long id) {
        return followService.isFollow(id);
    }
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable Long id) {
        return followService.commonFollow(id);
    }
}
