package com.yujing.common.error;

import org.springframework.http.HttpStatus;

/**
 * 4-digit error codes: 1xxx parameters, 2xxx authentication, 3xxx permission,
 * 4xxx business, 5xxx system (plan 6.2).
 */
public enum ErrorCode {

    VALIDATION_FAILED(1001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    MALFORMED_REQUEST(1002, HttpStatus.BAD_REQUEST, "请求格式不正确"),
    UNSUPPORTED_MEDIA_TYPE(1003, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的请求类型"),
    CURRENT_PASSWORD_INCORRECT(1004, HttpStatus.BAD_REQUEST, "当前密码不正确"),

    UNAUTHORIZED(2001, HttpStatus.UNAUTHORIZED, "未登录或登录已失效"),
    INVALID_CREDENTIALS(2002, HttpStatus.UNAUTHORIZED, "账号或密码错误"),
    ACCOUNT_DISABLED(2003, HttpStatus.FORBIDDEN, "账号已被停用"),
    ACCOUNT_LOCKED(2004, HttpStatus.UNAUTHORIZED, "账号或密码错误"),

    FORBIDDEN(3001, HttpStatus.FORBIDDEN, "无权访问该资源"),
    ADMIN_ONLY(3002, HttpStatus.FORBIDDEN, "仅管理员可操作"),

    RESOURCE_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "资源不存在"),
    VERSION_CONFLICT(4002, HttpStatus.CONFLICT, "数据已被其他设备修改，请刷新后重试"),
    DUPLICATE_RESOURCE(4003, HttpStatus.CONFLICT, "资源已存在"),
    USERNAME_TAKEN(4004, HttpStatus.CONFLICT, "账号名已被占用"),
    ACCOUNT_FROZEN(4005, HttpStatus.FORBIDDEN, "账号注销冻结中"),
    EXPORT_RATE_LIMITED(4006, HttpStatus.TOO_MANY_REQUESTS, "24 小时内只能导出一次"),
    FILE_TOO_LARGE(4007, HttpStatus.PAYLOAD_TOO_LARGE, "图片超过 5 MB"),
    UNSUPPORTED_FILE_TYPE(4008, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "只接受 PNG 图片"),
    READ_ONLY_MODE(4009, HttpStatus.FORBIDDEN, "客户端版本过低，当前为只读"),

    INTERNAL_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    SERVICE_UNAVAILABLE(5002, HttpStatus.SERVICE_UNAVAILABLE, "服务暂时不可用");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
