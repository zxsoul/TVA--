package com.tva.startrail.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：统一返回 Result
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid / @Validated）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getFieldError();
        String msg = fieldError != null
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : "参数校验失败";
        return Result.error(400, msg);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,//缺少必填参数
            MethodArgumentTypeMismatchException.class,//参数类型不匹配
            HttpMessageNotReadableException.class//请求体读取失败
    })
    public Result<Void> handleBadRequest(Exception e) {
        return Result.error(400, "请求参数不合法：" + e.getMessage());
    }

    /**
     * 数据库唯一索引兜底（并发撞 uk_username / uk_email）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突：{}", e.getMessage());
        return Result.error(400, "用户名或邮箱已被注册");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.error(404, "资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(404, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误");
    }
}
