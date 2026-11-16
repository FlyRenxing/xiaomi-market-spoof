// 清理整个工程的 build 目录。
tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
