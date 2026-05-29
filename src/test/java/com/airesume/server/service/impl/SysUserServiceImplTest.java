package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.MembershipPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

class SysUserServiceImplTest {

    @Test
    void removeByIdShouldEvictUserCache() throws NoSuchMethodException {
        assertUserMutationEvictsUserIdCacheAndUsernameCache(
                SysUserServiceImpl.class.getMethod("removeById", Serializable.class), "#id");
    }

    @Test
    void updateByIdShouldEvictUserAndUsernameCaches() throws NoSuchMethodException {
        assertUserMutationEvictsUserIdCacheAndUsernameCache(
                SysUserServiceImpl.class.getMethod("updateById", SysUser.class), "#entity.id");
    }

    @Test
    void saveShouldEvictUserAndUsernameCaches() throws NoSuchMethodException {
        assertUserMutationEvictsUserIdCacheAndUsernameCache(
                SysUserServiceImpl.class.getMethod("save", SysUser.class), "#entity.id");
    }

    @Test
    void vipFeatureLimitsShouldBeZeroWhenCurrentPlanIsMissing() {
        SysUserServiceImpl service = spy(new SysUserServiceImpl());
        ReflectionTestUtils.setField(service, "self", service);
        ReflectionTestUtils.setField(service, "membershipPlanService", mock(MembershipPlanService.class));

        SysUser user = new SysUser();
        user.setId(1L);
        user.setRole(UserRoleConstants.ROLE_VIP);
        user.setMembershipPlanCode("missing_plan");
        user.setVipExpireTime(LocalDateTime.now().plusDays(1));
        doReturn(user).when(service).getById(1L);

        assertEquals(0, service.getVipDailyPolishLimit(1L));
        assertEquals(0, service.getVipDailyJdMatchLimit(1L));
        assertEquals(0, service.getVipDailyTemplateLimit(1L));
        assertEquals(0, service.getVipDailyOfferLimit(1L));
    }

    private void assertUserMutationEvictsUserIdCacheAndUsernameCache(Method method, String userIdKey) {
        Caching caching = method.getAnnotation(Caching.class);

        assertNotNull(caching);
        CacheEvict[] evicts = caching.evict();
        assertEquals("sys_user", evicts[0].value()[0]);
        assertEquals(userIdKey, evicts[0].key());
        assertEquals("user:username", evicts[1].value()[0]);
        assertEquals(true, evicts[1].allEntries());
    }
}
