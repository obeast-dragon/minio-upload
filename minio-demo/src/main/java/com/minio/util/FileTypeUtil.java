package com.minio.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.ObjectUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Slf4j
public final class FileTypeUtil {

    private static final Map<String, List<String>> MIME_TYPE_MAP;

    static {
        MIME_TYPE_MAP = new HashMap<>();
        try {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "mime/mime-types.xml"));

            Element rootElement = document.getRootElement();
            List<Element> mimeTypeElements = rootElement.elements("mime-type");
            for (Element mimeTypeElement : mimeTypeElements) {
                String type = mimeTypeElement.attributeValue("type");
                List<Element> globElements = mimeTypeElement.elements("glob");
                List<String> fileTypeList = new ArrayList<>(globElements.size());
                for (Element globElement : globElements) {
                    String fileType = globElement.getTextTrim();
                    fileTypeList.add(fileType);
                }
                MIME_TYPE_MAP.put(type, fileTypeList);
            }
        } catch (DocumentException e) {
            log.error("", e);
        }

    }

    private FileTypeUtil() {
    }


    /**
     * 获取文件的MimeType
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param fileSize    文件字节大小
     * @return 文件的MimeType
     */
    public static String getFileMimeType(InputStream inputStream, String fileName, Long fileSize) {
        AutoDetectParser parser = new AutoDetectParser();
        parser.setParsers(new HashMap<>());
        Metadata metadata = new Metadata();

        // 设置资源名称
        if (!ObjectUtils.isEmpty(fileName)) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        }

        // 设置资源大小
        if (!ObjectUtils.isEmpty(fileSize)) {
            metadata.set(Metadata.CONTENT_LENGTH, Long.toString(fileSize));
        }
        try (InputStream stream = inputStream) {
            parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());
        } catch (IOException | SAXException | TikaException e) {
            log.error("", e);
            throw new IllegalArgumentException("文件的MimeType类型解析失败，原因：" + e.getMessage());
        }
        return metadata.get(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * 获取文件的MimeType
     *
     * @param inputStream inputStream
     * @return 文件的MimeType
     */
    public static String getFileMimeType(InputStream inputStream) throws IllegalArgumentException {
        return getFileMimeType(inputStream, null, null);
    }

    /**
     * 获取文件的真实类型, 全为小写
     *
     * @param inputStream inputStream
     * @return String
     */
    public static List<String> getFileRealTypeList(InputStream inputStream, String fileName, Long fileSize) {
        String fileMimeType = getFileMimeType(inputStream, fileName, fileSize);
        log.info("fileMimeType:{}", fileMimeType);
        return getFileRealTypeList(fileMimeType);
    }


    /**
     * 获取文件的真实类型, 全为小写
     *
     * @param inputStream inputStream
     * @return String
     * @throws IOException IOException
     */
    public static List<String> getFileRealTypeList(InputStream inputStream) throws IOException {
        return getFileRealTypeList(inputStream, null, null);
    }


    /**
     * 根据文件的mime类型获取文件的真实扩展名集合
     *
     * @param mimeType 文件的mime 类型
     * @return 文件的扩展名集合
     */
    public static List<String> getFileRealTypeList(String mimeType) {
        if (ObjectUtils.isEmpty(mimeType)) {
            return Collections.emptyList();
        }

        List<String> fileTypeList = MIME_TYPE_MAP.get(mimeType.replace(" ", ""));
        if (fileTypeList == null) {
            log.info("mimeType:{}, FileTypeList is null", mimeType);
            return Collections.emptyList();
        }
        return fileTypeList;
    }
}
