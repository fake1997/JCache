package com.cache.bigcache.sotrage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedStorage implements IStorage{
    private RandomAccessFile raf;
    private ThreadLocalByteBuffer threadLocalByteBuffer;

    public MemoryMappedStorage(String dir, int index, int capacity) throws IOException{
        File backFile = new File(dir);
        if(!backFile.exists()){
            backFile.exists();
        }
        String backFileName = dir + index + "-" + System.currentTimeMillis() + DATA_FILE_SUFFIX;
        raf = new RandomAccessFile(backFileName,"rw");
        MappedByteBuffer mappedByteBuffer = raf.getChannel().map(FileChannel.MapMode.PRIVATE, 0, capacity);
        threadLocalByteBuffer = new ThreadLocalByteBuffer(mappedByteBuffer);
    }

    private ByteBuffer getLocal(int position){
        ByteBuffer buffer = threadLocalByteBuffer.getSourceBuffer();
        buffer.position(position);
        return buffer;
    }

    @Override
    public void get(int position, byte[] dest) throws IOException {
        ByteBuffer buffer = getLocal(position);
        buffer.get(dest);
    }

    @Override
    public void put(int position, byte[] source) throws IOException {
        ByteBuffer buffer = getLocal(position);
        buffer.put(source);
    }

    @Override
    public void free() {
        MappedByteBuffer buffer = (MappedByteBuffer)threadLocalByteBuffer.getSourceBuffer();
        buffer.clear();
    }

    @Override
    public void close() throws Exception {
        if(raf!=null){
            raf.close();
        }
        // implies system gc
        threadLocalByteBuffer.set(null);
        threadLocalByteBuffer = null;
    }

    private static class ThreadLocalByteBuffer extends ThreadLocal<ByteBuffer> {
        private ByteBuffer _src;
        private ThreadLocalByteBuffer(ByteBuffer src){
            this._src = src;
        }

        public ByteBuffer getSourceBuffer(){
            return this._src;
        }

        @Override
        protected synchronized ByteBuffer initialValue(){
            ByteBuffer dup = _src.duplicate();
            return dup;
        }

    }
}
