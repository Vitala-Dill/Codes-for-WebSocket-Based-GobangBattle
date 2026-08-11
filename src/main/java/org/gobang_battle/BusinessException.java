package org.gobang_battle;

//统一异常处理
public class BusinessException extends RuntimeException{
    //错误码
    private Integer code;

    public BusinessException (String message) {
        super(message);//只传错误信息
        this.code = 400;//设置错误码为400
    }

    public BusinessException (Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
