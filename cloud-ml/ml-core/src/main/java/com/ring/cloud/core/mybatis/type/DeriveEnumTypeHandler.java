package com.ring.cloud.core.mybatis.type;

import com.ring.cloud.core.common.DeriveTypeEnum;
import org.apache.ibatis.type.EnumTypeHandler;

public class DeriveEnumTypeHandler extends EnumTypeHandler<DeriveTypeEnum> {
    public DeriveEnumTypeHandler() {
        super(DeriveTypeEnum.class);
    }
}
