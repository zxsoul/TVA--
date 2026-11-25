package com.tva.startrail.common;

import com.tva.startrail.dto.RegisterDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试（直接调用 handler 方法）
 */
@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BizException——原样返回业务码与消息")
    void handleBiz_ReturnsCodeAndMsg() {
        Result<Void> r = handler.handleBiz(new BizException(400, "用户名已存在，请更换"));

        assertEquals(400, r.getCode());
        assertEquals("用户名已存在，请更换", r.getMsg());
    }

    @Test
    @DisplayName("BindException——返回字段名与校验消息的 400")
    void handleBind_ReturnsFieldError() {
        BindException ex = new BindException(new RegisterDTO(), "registerDTO");
        ex.addError(new FieldError("registerDTO", "email", "邮箱格式不正确"));

        Result<Void> r = handler.handleBind(ex);

        assertEquals(400, r.getCode());
        assertEquals("email 邮箱格式不正确", r.getMsg());
    }

    @Test
    @DisplayName("BindException——无 FieldError 时使用兜底文案")
    void handleBind_NoFieldError_DefaultMsg() {
        Result<Void> r = handler.handleBind(new BindException(new RegisterDTO(), "registerDTO"));

        assertEquals(400, r.getCode());
        assertEquals("参数校验失败", r.getMsg());
    }

    @Test
    @DisplayName("缺少必填参数——400 与非法请求提示")
    void handleBadRequest_MissingParam() {
        Result<Void> r = handler.handleBadRequest(
                new MissingServletRequestParameterException("username", "String"));

        assertEquals(400, r.getCode());
        assertTrue(r.getMsg().startsWith("请求参数不合法："));
    }

    @Test
    @DisplayName("唯一索引冲突——返回注册冲突提示")
    void handleDuplicateKey() {
        Result<Void> r = handler.handleDuplicateKey(new DuplicateKeyException("uk_username"));

        assertEquals(400, r.getCode());
        assertEquals("用户名或邮箱已被注册", r.getMsg());
    }

    @Test
    @DisplayName("资源不存在——404")
    void handleNotFound() {
        Result<Void> r = handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "/api/none"));

        assertEquals(404, r.getCode());
        assertEquals("资源不存在", r.getMsg());
    }

    @Test
    @DisplayName("请求方法不支持——404 接口不存在")
    void handleMethodNotSupported() {
        Result<Void> r = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(404, r.getCode());
        assertEquals("接口不存在", r.getMsg());
    }

    @Test
    @DisplayName("未预期异常——统一 500 兜底")
    void handleOther_500() {
        Result<Void> r = handler.handleOther(new RuntimeException("boom"));

        assertEquals(500, r.getCode());
        assertEquals("服务器内部错误", r.getMsg());
    }
}
