package com.storage.system.user.contract;

import java.util.Collection;
import java.util.Map;

public interface OperatorResolver {

    OperatorInfo requireCurrentOperator();

    OperatorInfo findById(Long id);

    Map<Long, OperatorInfo> findByIds(Collection<Long> ids);
}
