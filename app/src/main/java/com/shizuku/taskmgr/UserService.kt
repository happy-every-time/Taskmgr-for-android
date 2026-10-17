package com.shizuku.taskmgr

class UserService : IUserService.Stub() {
    override fun destroy() {
        // 收到销毁指令时，结束当前进程
        System.exit(0)
    }

    override fun execCommand(command: String): CommandResult {
        return try {
            // 使用 ProcessBuilder 执行 shell 命令
            val process = ProcessBuilder("sh", "-c", command).start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            CommandResult(exitCode, output.trim(), error.trim())
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }
}