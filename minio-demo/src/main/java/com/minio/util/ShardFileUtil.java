package com.minio.util;

import cn.hutool.core.io.FileUtil;
import com.minio.entity.MockMultipartFile;
import com.minio.exception.StatusCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/24 15:59
 * @Version 1.0
 */
@Slf4j
public class ShardFileUtil {

    private ShardFileUtil() {

    }

    public static void main(String[] args) throws Exception {

//        String name = "D:/mysoft/testmp4.mp4";
//        String name = "C:\\Users\\WengX\\Desktop\\2222.rar";
//        String name = "C:\\Users\\WengX\\Desktop\\分片问题.md";
//        String name = "C:\\Users\\WengX\\Desktop\\2222.rar";
        String name = "C:\\Users\\WengX\\Desktop\\2222.rar";
        String fileName = FileUtil.getName(name);
        File file = new File(name);
        MultipartFile multipartFile = new MockMultipartFile(fileName ,new FileInputStream(name));
        System.out.println(multipartFile.getName());
        System.out.println(StringUtils.getFilenameExtension(multipartFile.getName()));


//        File file1 = new File("temp");
//        FileUtils.copyInputStreamToFile(inputStream, file1);
//        System.out.println(file1.getName());


//        System.out.println(FileTypeUtil.getFileMimeType(inputStream));
//        System.out.println(FileUtil.getType(file));

//            splitFile1(name);
//        long start = System.currentTimeMillis();

//        List<MultipartFile> multipartFiles = splitFileWork(file);
//        long end = System.currentTimeMillis();
////            System.out.println("运行时长==" + (end - start));
//        System.out.println(multipartFiles.size());
//        for (MultipartFile multipartFile : multipartFiles) {
//            System.out.println(multipartFile.getSize());
//         mergeFile1("D:/mysoft", "D:/mysoft/newtestmp4.mp4");
//        List<InputStream> inputStreams = splitFileInputStreams(file, 5 * 1024 * 1024);
//
//        System.out.println(inputStreams.size());

    }


    /**
     * 大文件分片
     *
     * @param file      文件路径；
     * @param splitSize = 5 * 1024 * 1024;//单片文件大小,5M
     * @return List<MultipartFile> 分片集合
     */
    @SneakyThrows
    public static List<InputStream> splitFileInputStreams(File file, long splitSize) {
        if (splitSize < (5 * 1024 * 1024)) {
            throw new Exception(StatusCode.SHARD_MUST_MORE_THAN_5M.getMessage());
        }
        List<InputStream> inputStreams = new ArrayList<>();
        InputStream bis = null;//输入流用于读取文件数据
        OutputStream bos = null;//输出流用于输出分片文件至磁盘
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            long writeByte = 0;//已读取的字节数
            int len = 0;
            byte[] bt = new byte[5 * 1024 * 1024];
            while (-1 != (len = bis.read(bt))) {
                if (writeByte % splitSize == 0) {
                    bos = new ByteArrayOutputStream();
                }
                writeByte += len;
                bos.write(bt, 0, len);
                if (writeByte % splitSize == 0) {
                    InputStream inputStream = IOConvertUtil.oConvertI(bos);
                    inputStreams.add(inputStream);
                }
            }
            InputStream inputStream = IOConvertUtil.oConvertI(bos);
            inputStreams.add(inputStream);
            log.info("{} 文件分片成功！", file.getName());
        } catch (Exception e) {
            log.error("文件分片失败！原因：{}", e.getMessage());
            e.printStackTrace();
        }
        return inputStreams;
    }


    /**
     * 大文件分片
     *
     * @param multipartFile      MultipartFile
     * @param splitSize = 5 * 1024 * 1024;//单片文件大小,5M
     * @return List<MultipartFile> 分片集合
     */
    @SneakyThrows
    public static List<InputStream> splitMultipartFileInputStreams(MultipartFile multipartFile, long splitSize) {
        if (splitSize < (5 * 1024 * 1024)) {
            throw new Exception(StatusCode.SHARD_MUST_MORE_THAN_5M.getMessage());
        }
        String filename = multipartFile.getOriginalFilename();
        List<InputStream> inputStreams = new ArrayList<>();
        InputStream bis = null;//输入流用于读取文件数据
        OutputStream bos = null;//输出流用于输出分片文件至磁盘
        try {
            bis = new BufferedInputStream(multipartFile.getInputStream());
            long writeByte = 0;//已读取的字节数
            int len = 0;
            byte[] bt = new byte[5 * 1024 * 1024];
            while (-1 != (len = bis.read(bt))) {
                if (writeByte % splitSize == 0) {
                    bos = new ByteArrayOutputStream();
                }
                writeByte += len;
                bos.write(bt, 0, len);
                if (writeByte % splitSize == 0) {
                    InputStream inputStream = IOConvertUtil.oConvertI(bos);
                    inputStreams.add(inputStream);
                }
            }
            InputStream inputStream = IOConvertUtil.oConvertI(bos);
            inputStreams.add(inputStream);
            log.info("{} 文件分片成功！", filename);
        } catch (Exception e) {
            log.error("文件分片失败！原因：{}", e.getMessage());
            e.printStackTrace();
        }
        return inputStreams;
    }

    /**
     * 大文件分片
     * @param file      文件路径
     * @param splitSize = 5 * 1024 * 1024;//单片文件大小,5M
     * @return List<MultipartFile>
     */
    public static List<MultipartFile> splitFileMultipartFiles(File file, long splitSize) {
        List<MultipartFile> files = new ArrayList<>();
        InputStream bis = null;//输入流用于读取文件数据
        OutputStream bos = null;//输出流用于输出分片文件至磁盘
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            long writeByte = 0;//已读取的字节数
            int len = 0;
            byte[] bt = new byte[5 * 1024 * 1024];
            while (-1 != (len = bis.read(bt))) {
                if (writeByte % splitSize == 0) {
                    bos = new ByteArrayOutputStream();
                }
                writeByte += len;
                bos.write(bt, 0, len);
                if (writeByte % splitSize == 0) {
                    InputStream inputStream = IOConvertUtil.oConvertI(bos);
                    MultipartFile multipartFile = new MockMultipartFile(String.valueOf((writeByte / splitSize)), inputStream);
                    files.add(multipartFile);
                }
            }
            InputStream inputStream = IOConvertUtil.oConvertI(bos);
            MultipartFile multipartFile = new MockMultipartFile(String.valueOf((writeByte / splitSize)), inputStream);
            files.add(multipartFile);
            System.out.println("文件分片成功！");
        } catch (Exception e) {
            System.out.println("文件分片失败！");
            e.printStackTrace();
        }
        return files;
    }

    /**
     * 磁盘大文件合成方法
     *
     * @param filePath 文件路径
     */
    public static void splitFile(String filePath) {
        InputStream bis = null;//输入流用于读取文件数据
        OutputStream bos = null;//输出流用于输出分片文件至磁盘
        try {
            File file = new File(filePath);
            long splitSize = 5 * 1024 * 1024;//单片文件大小,5M
            bis = new BufferedInputStream(new FileInputStream(file));
            long writeByte = 0;//已读取的字节数
            int len = 0;
            byte[] bt = new byte[(int) splitSize];
            int flag = 0;
            while (-1 != (len = bis.read(bt))) {
                if (writeByte % splitSize == 0) {
                    if (bos != null) {
                        bos.flush();
                        bos.close();
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(filePath + "." + (writeByte / splitSize + 1) + ".part"));

                }
                writeByte += len;
                bos.write(bt, 0, len);
            }
            System.out.println("文件分片成功！");
        } catch (Exception e) {
            System.out.println("文件分片失败！");
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 磁盘大文件合成方法
     *
     * @param splitDir    分片目录
     * @param newFilePath 新文件路径
     * @throws Exception
     */
    public static void mergeFile(String splitDir, String newFilePath) {
        File dir = new File(splitDir);//目录对象
        //分片文件
        File[] fileArr = dir.listFiles((dir1, name) -> name.endsWith(".part"));
        List<File> fileList = Arrays.asList(fileArr);
        Collections.sort(fileList, new Comparator<File>() {//根据文件名称对fileList顺序排序
            @Override
            public int compare(File o1, File o2) {
                int lastIndex11 = o1.getName().lastIndexOf(".");
                int lastIndex12 = o1.getName().substring(0, lastIndex11).lastIndexOf(".") + 1;
                int lastIndex21 = o2.getName().lastIndexOf(".");
                int lastIndex22 = o2.getName().substring(0, lastIndex21).lastIndexOf(".") + 1;
                int num1 = Integer.parseInt(o1.getName().substring(lastIndex12, lastIndex11));
                int num2 = Integer.parseInt(o2.getName().substring(lastIndex22, lastIndex21));
                return num1 - num2;
            }
        });
        OutputStream bos = null;
        InputStream bis = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(newFilePath));//定义输出流
            for (File file : fileList) {//按顺序合成文件
                bis = new BufferedInputStream(new FileInputStream(file));
                int len = 0;
                byte[] bt = new byte[1024];
                while (-1 != (len = bis.read(bt))) {
                    bos.write(bt, 0, len);
                }
                bis.close();
            }
            bos.flush();
            bos.close();
            System.out.println("大文件合成成功！");
        } catch (Exception e) {
            System.out.println("大文件合成失败！");
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
