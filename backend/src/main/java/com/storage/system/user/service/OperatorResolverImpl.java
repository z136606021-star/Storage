package com.storage.system.user.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.service.AuthService;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.system.user.entity.SysUser;
import com.storage.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperatorResolverImpl implements OperatorResolver {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;

    @Override
    public OperatorInfo requireCurrentOperator() {
        SysUser user = authService.currentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        return toInfo(user);
    }

    @Override
    public OperatorInfo findById(Long id) {
        if (id == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(id);
        return user == null ? null : toInfo(user);
    }

    @Override
    public Map<Long, OperatorInfo> findByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, this::toInfo));
    }

    private OperatorInfo toInfo(SysUser user) {
        OperatorInfo info = new OperatorInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        return info;
    }
}
