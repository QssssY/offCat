package com.airesume.server.service.impl;

import com.airesume.server.entity.MembershipOrder;
import com.airesume.server.mapper.MembershipOrderMapper;
import com.airesume.server.service.MembershipOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MembershipOrderServiceImpl extends ServiceImpl<MembershipOrderMapper, MembershipOrder>
        implements MembershipOrderService {
}
