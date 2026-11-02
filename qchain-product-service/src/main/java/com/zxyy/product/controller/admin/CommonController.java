package com.zxyy.product.controller.admin;


import com.zxyy.common.constant.MessageConstant;
import com.zxyy.common.result.Result;
import com.zxyy.product.exception.AliOssException;
import com.zxyy.product.utiil.AliOssUtil;
import com.zxyy.product.utiil.MagickUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common/upload")
@Tag(name = "通用接口")
@Slf4j
public class CommonController {
    //将对象存储的工具类诸如进来
    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 这个MultipartFile要上传的文件大了 传不上 前端应该进行一次压缩
     * TODO 后端这里 统一压缩为一个格式 上传
     * @param file
     * @return
     */
    @PostMapping
    @Operation(summary = "上传图片到oss")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file);
        // 生成缩略图（使用 try-with-resources）
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            MagickUtil.processImage(new ByteArrayInputStream(file.getBytes()), "q", "w", "h", baos, "webp");
            byte[] thumbBytes = baos.toByteArray();
            //这里将文件名称用uuid代替
            String fileName = UUID.randomUUID().toString()+".webp";
            String url = aliOssUtil.upload(thumbBytes, fileName);
            return Result.success(url);
        } catch (IOException e) {
            throw new AliOssException(MessageConstant.ALIOSS_ERROR);
        }
    }
}
