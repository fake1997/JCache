package com.cache.bigcache.utils;

import java.io.*;

/**
*
* @Author: fake1997
* @date: 2022/4/12
*/
public class FileUtil {
    private static final int BUFFER_SIZE = 4096 * 4;

    public static boolean isFilenameValid(String file){
        File f = new File(file);
        try{
            f.getCanonicalPath();
            return true;
        } catch (IOException e){
            return false;
        }
    }

    public static void deleteDirectory(File dir){
        if(!dir.exists()){
            return;
        }
        File[] subs = dir.listFiles();
        if(subs != null){
            for(File f : dir.listFiles()){
                if(f.isFile()){
                    if(!f.delete()){
                        throw new IllegalStateException("delete file failed: " + f);
                    }
                }else{
                    deleteDirectory(f);
                }
            }
        }
        if(!dir.delete()){
            throw new IllegalStateException(" delete directory error: " + dir);
        }
    }

    public static void deleteFile(File f){
        if(!f.exists() || !f.isFile()){
            return;
        }
        if(!f.delete()){
            throw new IllegalStateException();
        }
    }

    public static boolean copyDirectory(String from, String to){
        return copyDirectory(new File(from), new File(to));
    }

    public static boolean copyDirectory(File from, File to){
        return copyDirectory(from, to, (byte[]) null);
    }

    public static boolean copyDirectory(File from, File to, byte[] buffer){
        if(from == null) { return false; }
        if(!from.exists()){ return true; }
        if(!from.isDirectory()){ return false; }
        if(to.exists()){ return false; }
        if(!to.mkdirs()){ return false; }

        String[] subs = from.list();
        if(subs != null){
            if(buffer == null) {
                buffer = new byte[BUFFER_SIZE];
            }

            for(String fileName : subs){
                File entry = new File(from, fileName);

                if(entry.isDirectory()){
                    if(!copyDirectory(entry, new File(to, fileName), buffer)){
                        return false;
                    }
                } else {
                    if(!copyFile(entry, new File(to, fileName), buffer)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean copyFile(File from, File to, byte[] buffer){
        if(buffer == null){ buffer = new byte[BUFFER_SIZE]; }

        try(
                FileInputStream fromInputStream = new FileInputStream(from);
                FileOutputStream toOutputStream = new FileOutputStream(to);
        ){
            for(int bytesRead = fromInputStream.read(buffer); bytesRead > 0; bytesRead = fromInputStream.read(buffer)){
                toOutputStream.write(buffer, 0, bytesRead);
            }
            toOutputStream.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
