package com.airesume.server.service.impl;

import com.airesume.server.entity.UserFeedback;
import com.airesume.server.mapper.UserFeedbackMapper;
import com.airesume.server.service.UserFeedbackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserFeedbackServiceImpl extends ServiceImpl<UserFeedbackMapper, UserFeedback>
        implements UserFeedbackService {
}
