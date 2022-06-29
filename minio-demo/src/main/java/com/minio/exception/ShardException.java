package com.minio.exception;



import com.minio.entity.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;

//@RestControllerAdvice(basePackages = "")
//public class ShardException {
//
//    @ExceptionHandler(value = )
//    public R fileIsNull(){
//        return R.error(StatusCode.NULL_ARGS.getCode(), StatusCode.NULL_ARGS.getMessage());
//    }
//}
