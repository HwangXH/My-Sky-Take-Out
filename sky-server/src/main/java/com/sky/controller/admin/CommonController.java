package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    //根据MVC框架
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传: {}", file);

        //文件对象转换成Byte数组，图片在阿里云中的名字（UUID）
        try {
            //获取原始文件名，提取出后缀名，命名改为UUID保证存到云服务器中的唯一性
            // TODO 可以在Util类实现类似的想过，解耦；图像上传应该发生在用户点击提交后再上传到oss中；是否可以尝试上传到本地的文件夹中
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;

            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("文件上传成功...");
            return Result.success(filePath);
        } catch (IOException e) {
            log.info("文件上传失败: {}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
