package com.cache.bigcache.sotrage;

public class Pointer {
    /** the position in a block */
    protected int position;

    /** the length of the value */
    protected int length;

    /** the associated storage block */
    protected StorageBlock storageBlock;

    public Pointer(int position, int length, StorageBlock storageBlock) {
        this.position = position;
        this.length = length;
        this.storageBlock = storageBlock;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public StorageBlock getStorageBlock() {
        return storageBlock;
    }

    public void setStorageBlock(StorageBlock storageBlock) {
        this.storageBlock = storageBlock;
    }

    public Pointer copy(Pointer pointer){
        this.position = pointer.getPosition();
        this.length = pointer.getLength();
        this.storageBlock = pointer.getStorageBlock();
        return this;
    }
}
