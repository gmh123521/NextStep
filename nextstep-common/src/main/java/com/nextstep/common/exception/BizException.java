package com.nextstep.common.exception;

import com.nextstep.common.core.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String msg) {
        super(msg);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(ResultCode rc) {
        super(rc.getMsg());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String msg) {
        super(msg);
        this.code = rc.getCode();
    }
}
