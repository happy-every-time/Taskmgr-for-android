package com.shizuku.taskmgr;
import com.shizuku.taskmgr.CommandResult;

interface IUserService {
    // Shizuku 服务器定义的销毁方法，事务码固定
    void destroy() = 16777114;
    // 自定义方法，事务码需唯一
    CommandResult execCommand(String command) = 1;
}