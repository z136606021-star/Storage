package com.storage.system.user.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SysUserServiceIntegrationTest {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    @Transactional
    void createUser_normalizesMixedCaseEmailToLowercase() {
        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername("emailnorm");
        dto.setDisplayName("邮箱归一化");
        dto.setEmail("  Bo_Lv@Jabil.COM  ");
        dto.setStatus(1);
        dto.setRoleIds(List.of(2L));

        var created = sysUserService.create(dto);

        assertThat(created.getEmail()).isEqualTo("bo_lv@jabil.com");
        assertThat(sysUserMapper.selectById(created.getId()).getEmail()).isEqualTo("bo_lv@jabil.com");
    }

    @Test
    @Transactional
    void updateUser_normalizesMixedCaseEmailToLowercase() {
        SysUserSaveDTO createDto = new SysUserSaveDTO();
        createDto.setUsername("emailupd");
        createDto.setDisplayName("邮箱更新");
        createDto.setEmail("old@example.com");
        createDto.setStatus(1);
        createDto.setRoleIds(List.of(2L));
        var created = sysUserService.create(createDto);

        SysUserSaveDTO updateDto = new SysUserSaveDTO();
        updateDto.setUsername("emailupd");
        updateDto.setDisplayName("邮箱更新");
        updateDto.setEmail(" NEW@Example.COM ");
        updateDto.setStatus(1);
        updateDto.setRoleIds(List.of(2L));

        var updated = sysUserService.update(created.getId(), updateDto);

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @Transactional
    void createUser_rejectsDuplicateEmail() {
        SysUserSaveDTO first = baseDto("emailuser1", "用户一");
        first.setEmail("shared@example.com");
        sysUserService.create(first);

        SysUserSaveDTO second = baseDto("emailuser2", "用户二");
        second.setEmail("shared@example.com");

        assertThatThrownBy(() -> sysUserService.create(second))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被其他用户使用");
    }

    @Test
    @Transactional
    void createUser_rejectsWhitespaceInUsername() {
        SysUserSaveDTO dto = baseDto("123 asd", "用户空格");
        dto.setEmail("space@example.com");

        assertThatThrownBy(() -> sysUserService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("NTID不能包含空格或空白字符");
    }

    @Test
    @Transactional
    void createUser_storesTrimmedPhone() {
        SysUserSaveDTO dto = baseDto("phonecreate", "手机号创建");
        dto.setPhone(" 13800138000 ");

        var created = sysUserService.create(dto);

        assertThat(created.getPhone()).isEqualTo("13800138000");
        assertThat(sysUserMapper.selectById(created.getId()).getPhone()).isEqualTo("13800138000");
    }

    @Test
    @Transactional
    void updateUser_replacesPhoneAndBlankClears() {
        SysUserSaveDTO createDto = baseDto("phoneupdate", "手机号更新");
        createDto.setPhone("13800138000");
        var created = sysUserService.create(createDto);

        SysUserSaveDTO updateDto = baseDto("phoneupdate", "手机号更新");
        updateDto.setPhone(" 13900139000 ");
        var updated = sysUserService.update(created.getId(), updateDto);
        assertThat(updated.getPhone()).isEqualTo("13900139000");

        SysUserSaveDTO clearDto = baseDto("phoneupdate", "手机号更新");
        clearDto.setPhone("   ");
        var cleared = sysUserService.update(created.getId(), clearDto);
        assertThat(cleared.getPhone()).isNull();
        assertThat(sysUserMapper.selectById(created.getId()).getPhone()).isNull();
    }

    @Test
    @Transactional
    void createUser_allowsWhitespaceInDisplayName() {
        SysUserSaveDTO dto = baseDto("mandy7362", "Mandy Liu");
        dto.setEmail("mandy_liu7362@jabil.com");
        dto.setPhone("18820777053");
        dto.setRoleIds(List.of(1L));

        var created = sysUserService.create(dto);

        assertThat(created.getDisplayName()).isEqualTo("Mandy Liu");
        assertThat(sysUserMapper.selectById(created.getId()).getDisplayName()).isEqualTo("Mandy Liu");
    }

    @Test
    @Transactional
    void createUser_trimsDisplayNameEdges() {
        SysUserSaveDTO dto = baseDto("trimname", "  Mandy Liu  ");
        var created = sysUserService.create(dto);
        assertThat(created.getDisplayName()).isEqualTo("Mandy Liu");
    }

    @Test
    @Transactional
    void createUser_rejectsDuplicateRoleIds() {
        SysUserSaveDTO dto = baseDto("duprole", "重复角色");
        dto.setRoleIds(List.of(2L, 2L));

        assertThatThrownBy(() -> sysUserService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("角色不能重复");
    }

    @Test
    @Transactional
    void createUser_rejectsCustomPasswordShorterThanSix() {
        SysUserSaveDTO dto = baseDto("shortpw", "短密码");
        dto.setPassword("12345");

        assertThatThrownBy(() -> sysUserService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码长度为 6-64 个字符");
    }

    private SysUserSaveDTO baseDto(String username, String displayName) {
        SysUserSaveDTO dto = new SysUserSaveDTO();
        dto.setUsername(username);
        dto.setDisplayName(displayName);
        dto.setStatus(1);
        dto.setRoleIds(List.of(2L));
        return dto;
    }
}
