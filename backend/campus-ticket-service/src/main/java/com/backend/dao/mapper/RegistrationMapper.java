package com.backend.dao.mapper;

import com.backend.entity.Registration;
import com.backend.vo.RegistrationDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    RegistrationDetail getRegistrationDetailById(Long registrationId);
}
