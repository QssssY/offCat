package com.airesume.server.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应结果封装类
 *
 * 所属模块：公共模块 - 响应结果
 * 用途：统一所有 API 接口的返回格式，方便前端统一处理
 *
 * 响应格式规范：
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {...}
 * }
 *
 * 使用示例：
 * - 成功返回：Result.success(data)
 * - 失败返回：Result.error("错误信息")
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码
     * 200: 成功
     * 其他: 失败（参考 ResultCode）
     */
    private Integer code;

    /**
     * 响应消息
     * 成功时通常为 "success"
     * 失败时为具体错误信息
     */
    private String message;

    /**
     * 响应数据
     * 成功时返回具体数据
     * 失败时通常为 null
     */
    private T data;

    /**
     * 私有构造函数，禁止直接 new
     * 请使用静态工厂方法创建实例
     */
    private Result() {
    }

    /**
     * 私有构造函数
     *
     * @param code    业务状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     *
     * @return Result 对象
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return Result 对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息 + 数据）
     *
     * @param message 自定义成功消息
     * @param data    响应数据
     * @return Result 对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（默认错误）
     *
     * @return Result 对象
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误消息）
     *
     * @param message 错误消息
     * @return Result 对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 失败响应（自定义错误码 + 错误消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @return Result 对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（使用 ResultCode 枚举）
     *
     * @param resultCode 错误码枚举
     * @return Result 对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

}
