package com.storage.shiro;

import com.storage.mapper.SysUserMapper;
import jakarta.servlet.Filter;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ShiroConfig {

    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public UserRealm userRealm(SysUserMapper sysUserMapper, BCryptPasswordEncoder passwordEncoder) {
        UserRealm realm = new UserRealm(sysUserMapper, passwordEncoder);
        realm.setCredentialsMatcher(new BcryptCredentialsMatcher());
        return realm;
    }

    @Bean
    public SecurityManager securityManager(UserRealm userRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);
        securityManager.setSubjectDAO(subjectDAO());
        return securityManager;
    }

    @Bean
    public org.apache.shiro.mgt.DefaultSubjectDAO subjectDAO() {
        org.apache.shiro.mgt.DefaultSubjectDAO subjectDAO = new org.apache.shiro.mgt.DefaultSubjectDAO();
        org.apache.shiro.mgt.DefaultSessionStorageEvaluator evaluator =
                new org.apache.shiro.mgt.DefaultSessionStorageEvaluator();
        evaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(evaluator);
        return subjectDAO;
    }

    @Bean
    public MethodInvokingFactoryBean shiroSecurityManagerInitializer(SecurityManager securityManager) {
        MethodInvokingFactoryBean factoryBean = new MethodInvokingFactoryBean();
        factoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
        factoryBean.setArguments(securityManager);
        return factoryBean;
    }

    @Bean
    public FilterRegistrationBean<Filter> shiroSubjectBindingFilter(SecurityManager securityManager, JwtService jwtService) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShiroSubjectBindingFilter(securityManager, jwtService));
        registration.addUrlPatterns("/*");
        registration.setName("shiroSubjectBindingFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    @Bean
    public static DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }
}
