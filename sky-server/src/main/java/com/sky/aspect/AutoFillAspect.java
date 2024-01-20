package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类，实现公共字段自动填充处理数据
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")//单路拦截包内的类的方法 颗粒度太粗
    //还要满足方法上有autofill注解
    public void autoFillPointCut(){}

    /**
     * 通知
     */
    @Before("autoFillPointCut()")//自定义的前置通知，传入连接点参数
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段填充");
        //获取到当前被拦截的方法上的数据库操作类型 如UPDATE
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//获得方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);// 获得方法上的注解对象
        OperationType operationType = autoFill.value();//获取数据库操作类型


        //获取到被拦截方法的参数，就是一个实体
        Object[] args = joinPoint.getArgs();//获取了所有的参数
        if(args == null || args.length == 0){
            return;
        }
        Object entity = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 根据当前不同的类型，为对应的属性通过反射复制
        if(operationType == OperationType.INSERT){
            //为4个公共字段赋值
            try{
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射赋值
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity, currentId);

            }catch(Exception e){
                e.printStackTrace();
            }
        }else if(operationType == OperationType.UPDATE){
            try{
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射赋值
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity, currentId);

            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }
}
