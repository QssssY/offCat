package com.airesume.server.common.exception;

import com.airesume.server.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用 ResultCode + 动态中文消息构造业务异常。
     * 错误码取自 ResultCode，消息使用传入的 dynamicMessage（保留动态信息如文件大小上限）。
     */
    public BusinessException(ResultCode resultCode, String dynamicMessage) {
        super(dynamicMessage);
        this.code = resultCode.getCode();
    }

}
