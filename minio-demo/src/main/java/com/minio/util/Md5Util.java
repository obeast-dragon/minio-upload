package com.minio.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@Slf4j
public final class Md5Util {
    private static final int BUFFER_SIZE = 8 * 1024;

    private static final char[] HEX_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private Md5Util() {
    }

    /**
     * 计算字节数组的md5
     *
     * @param bytes bytes
     * @return 文件流的md5
     */
    public static String calculateMd5(byte[] bytes) {
        try {
            MessageDigest md5MessageDigest = MessageDigest.getInstance("MD5");
            return encodeHex(md5MessageDigest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("no md5 found");
        }
    }

    /**
     * 计算文件的输入流
     *
     * @param inputStream inputStream
     * @return 文件流的md5
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md5MessageDigest = MessageDigest.getInstance("MD5");
            try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                 DigestInputStream digestInputStream = new DigestInputStream(bis, md5MessageDigest)) {

                final byte[] buffer = new byte[BUFFER_SIZE];

                while (digestInputStream.read(buffer) > 0) {
                    // 获取最终的MessageDigest
                    md5MessageDigest = digestInputStream.getMessageDigest();
                }

                return encodeHex(md5MessageDigest.digest());
            } catch (IOException ioException) {
                log.error("", ioException);
                throw new IllegalArgumentException(ioException.getMessage());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("no md5 found");
        }
    }

    /**
     * 获取字符串的MD5值
     *
     * @param input 输入的字符串
     * @return md5
     */
    public static String calculateMd5(String input) {
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数，可以换成SHA1）
            MessageDigest md5MessageDigest = MessageDigest.getInstance("MD5");
            byte[] inputByteArray = input.getBytes(StandardCharsets.UTF_8);
            md5MessageDigest.update(inputByteArray);

            // 转换并返回结果，也是字节数组，包含16个元素
            byte[] resultByteArray = md5MessageDigest.digest();
            // 将字符数组转成字符串返回
            return encodeHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("md5 not found");
        }
    }

    /**
     * 转成的md5值为全小写
     *
     * @param bytes bytes
     * @return 全小写的md5值
     */
    private static String encodeHex(byte[] bytes) {
        char[] chars = new char[32];
        for (int i = 0; i < chars.length; i = i + 2) {
            byte b = bytes[i / 2];
            chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = HEX_CHARS[b & 0xf];
        }
        return new String(chars);
    }

}

