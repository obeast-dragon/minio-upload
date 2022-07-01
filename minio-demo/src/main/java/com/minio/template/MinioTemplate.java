package com.minio.template;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.minio.config.MinioConfig;
import com.minio.entity.OssFile;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author lyf
 * @description:
 * @version: v1.0
 * @since 2022-05-03 13:04
 */
@Slf4j
@AllArgsConstructor
public class MinioTemplate {
    /**
     * MinIO 客户端
     */
    private final MinioClient minioClient;

    /**
     * MinIO 配置类
     */
    private final MinioConfig minioConfig;

    /**
     * 查询所有存储桶
     *
     * @return Bucket 集合
     */
    @SneakyThrows
    public List<Bucket> listBuckets() {
        return minioClient.listBuckets();
    }

    /**
     * 桶是否存在
     *
     * @param bucketName 桶名
     * @return 是否存在
     */
    @SneakyThrows
    public boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 创建存储桶
     *
     * @param bucketName 桶名
     */
    @SneakyThrows
    public void makeBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 删除一个空桶 如果存储桶存在对象不为空时，删除会报错。
     *
     * @param bucketName 桶名
     */
    @SneakyThrows
    public void removeBucket(String bucketName) {
        removeBucket(bucketName, false);
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }

    /**
     * 删除一个桶 根据桶是否存在数据进行不同的删除
     * 桶为空时直接删除
     * 桶不为空时先删除桶中的数据，然后再删除桶
     *
     * @param bucketName 桶名
     */
    @SneakyThrows
    public void removeBucket(String bucketName, boolean bucketNotNull) {
        if (bucketNotNull) {
            deleteBucketAllObject(bucketName);
        }
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }

    /**
     * 上传文件
     *
     * @param inputStream      流
     * @param originalFileName 原始文件名
     * @param bucketName       桶名
     * @return ObjectWriteResponse
     */
    @SneakyThrows
    public OssFile putObject(InputStream inputStream, String bucketName, String originalFileName) {
        String uuidFileName = generateFileInMinioName(originalFileName);
        try {
            if (ObjectUtils.isEmpty(bucketName)) {
                bucketName = minioConfig.getBucketName();
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uuidFileName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());


            return new OssFile(uuidFileName, originalFileName);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }


    /**
     * 删除桶中所有的对象
     *
     * @param bucketName 桶对象
     */
    @SneakyThrows
    public void deleteBucketAllObject(String bucketName) {
        List<String> list = listObjectNames(bucketName);
        if (!list.isEmpty()) {
            for (String objectName : list) {
                deleteObject(bucketName, objectName);
            }
        }
    }

    /**
     * 查询桶中所有的对象名
     *
     * @param bucketName 桶名
     * @return objectNames
     */
    @SneakyThrows
    public List<String> listObjectNames(String bucketName) {
        List<String> objectNameList = new ArrayList<>();
        if (bucketExists(bucketName)) {
            Iterable<Result<Item>> results = listObjects(bucketName, true);
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                objectNameList.add(objectName);
            }
        }
        return objectNameList;
    }


    /**
     * 删除一个对象
     *
     * @param bucketName 桶名
     * @param objectName 对象名
     */
    @SneakyThrows
    public void deleteObject(String bucketName, String objectName) {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 上传分片文件
     *
     * @param inputStream 流
     * @param objectName  存入桶中的对象名
     * @param bucketName  桶名
     * @return ObjectWriteResponse
     */
    @SneakyThrows
    public OssFile putChunkObject(InputStream inputStream, String bucketName, String objectName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
            return new OssFile(objectName, objectName);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * 返回临时带签名、Get请求方式的访问URL
     *
     * @param bucketName 桶名
     * @param filePath   Oss文件路径
     * @return 临时带签名、Get请求方式的访问URL
     */
    @SneakyThrows
    public String getPresignedObjectUrl(String bucketName, String filePath) {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(filePath)
                        .build());
    }

    /**
     * 返回临时带签名、过期时间为1天的PUT请求方式的访问URL
     *
     * @param bucketName  桶名
     * @param filePath    Oss文件路径
     * @param queryParams 查询参数
     * @return 临时带签名、过期时间为1天的PUT请求方式的访问URL
     */
    @SneakyThrows
    public String getPresignedObjectUrl(String bucketName, String filePath, Map<String, String> queryParams) {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(filePath)
                        .expiry(1, TimeUnit.DAYS)
                        .extraQueryParams(queryParams)
                        .build());
    }


    /**
     * GetObject接口用于获取某个文件（Object）。此操作需要对此Object具有读权限。
     *
     * @param bucketName 桶名
     * @param objectName 文件路径
     */
    @SneakyThrows
    public InputStream getObject(String bucketName, String objectName) {
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * 查询桶的对象信息
     *
     * @param bucketName 桶名
     * @param recursive  是否递归查询
     * @return 桶的对象信息
     */
    @SneakyThrows
    public Iterable<Result<Item>> listObjects(String bucketName, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).recursive(recursive).build());
    }

    /**
     * 获取带签名的临时上传元数据对象，前端可获取后，直接上传到Minio
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     * @return Map<String, String>
     */
    @SneakyThrows
    public Map<String, String> getPresignedPostFormData(String bucketName, String fileName) {
        // 为存储桶创建一个上传策略，过期时间为7天
        PostPolicy policy = new PostPolicy(bucketName, ZonedDateTime.now().plusDays(1));
        // 设置一个参数key，值为上传对象的名称
        policy.addEqualsCondition("key", fileName);
        // 添加Content-Type，例如以"image/"开头，表示只能上传照片，这里吃吃所有
        policy.addStartsWithCondition("Content-Type", MediaType.ALL_VALUE);
        // 设置上传文件的大小 64kiB to 10MiB.
        //policy.addContentLengthRangeCondition(64 * 1024, 10 * 1024 * 1024);
        return minioClient.getPresignedPostFormData(policy);
    }


    public String generateFileInMinioName(String originalFilename) {
        return "files" + StrUtil.SLASH + DateUtil.format(new Date(), "yyyy-MM-dd") + StrUtil.SLASH + UUID.randomUUID() + StrUtil.UNDERLINE + originalFilename;
    }

    /**
     * 初始化默认存储桶
     */
    @PostConstruct
    public void initDefaultBucket() {
        String defaultBucketName = minioConfig.getBucketName();
        if (bucketExists(defaultBucketName)) {
            log.info("默认存储桶：{}已存在", defaultBucketName);
        } else {
            log.info("创建默认存储桶：{}", defaultBucketName);
            makeBucket(minioConfig.getBucketName());
        }
        ;
    }


    /**
     * 文件合并，将分块文件组成一个新的文件
     *
     * @param bucketName       合并文件生成文件所在的桶
     * @param fileName         原始文件名
     * @param sourceObjectList 分块文件集合
     * @return OssFile
     */
    @SneakyThrows
    public OssFile composeObject(String bucketName, String fileName, List<ComposeSource> sourceObjectList) {
        String filenameExtension = StringUtils.getFilenameExtension(fileName);
        String objectName = UUID.randomUUID() + "." + filenameExtension;
        minioClient.composeObject(ComposeObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .sources(sourceObjectList)
                .build());

        String presignedObjectUrl = getPresignedObjectUrl(bucketName, fileName);
        return new OssFile(presignedObjectUrl, fileName);
    }


    /**
     * 文件合并，将分块文件组成一个新的文件
     *
     * @param bucketName       合并文件生成文件所在的桶
     * @param objectName       原始文件名
     * @param sourceObjectList 分块文件集合
     * @return OssFile
     */
    @SneakyThrows
    public OssFile composeObject(List<ComposeSource> sourceObjectList, String bucketName, String objectName) {
        minioClient.composeObject(ComposeObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .sources(sourceObjectList)
                .build());
        String presignedObjectUrl = getPresignedObjectUrl(bucketName, objectName);
        return new OssFile(presignedObjectUrl, objectName);
    }

    /**
     * 文件合并，将分块文件组成一个新的文件
     *
     * @param originBucketName 分块文件所在的桶
     * @param targetBucketName 合并文件生成文件所在的桶
     * @param objectName       存储于桶中的对象名
     * @return OssFile
     */
    @SneakyThrows
    public OssFile composeObject(String originBucketName, String targetBucketName, String objectName) {

        Iterable<Result<Item>> results = listObjects(originBucketName, true);
        List<String> objectNameList = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            objectNameList.add(item.objectName());
        }


        if (ObjectUtils.isEmpty(objectNameList)) {
            throw new IllegalArgumentException(originBucketName + "桶中没有文件，请检查");
        }

        List<ComposeSource> composeSourceList = new ArrayList<>(objectNameList.size());
        // 对文件名集合进行升序排序
        objectNameList.sort((o1, o2) -> Integer.parseInt(o2) > Integer.parseInt(o1) ? -1 : 1);
        //合并文件
        for (String object : objectNameList) {
            composeSourceList.add(ComposeSource.builder()
                    .bucket(originBucketName)
                    .object(object)
                    .build());
        }
        //上传合并文件
        return composeObject(composeSourceList, targetBucketName, objectName);
    }
}

