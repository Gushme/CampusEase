package com.hmdp.utils;

/**
 * ClassName: ILock
 * Package: com.hmdp.utils
 * Description:
 *
 * @Author Gush
 * @Create 2024/9/12 15:00
 */
public interface ILock {
    /*
    * 尝试获取锁（非阻塞）
    * */
    public boolean tryLock(long timeoutSec);

    /*
    * 释放锁
    * */
    public void unlock();

}
