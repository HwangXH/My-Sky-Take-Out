package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，标识某个方法需要自动填充某个公共字段
 */
// 表明只能用在方法上
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {

    //指定数据库操作类型,是一个枚举类型： UPDATE INSERT
    OperationType value();
}
