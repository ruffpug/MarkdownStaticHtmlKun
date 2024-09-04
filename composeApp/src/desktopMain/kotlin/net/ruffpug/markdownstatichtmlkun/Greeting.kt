package net.ruffpug.markdownstatichtmlkun

//  ↓ Kotlinの文法として正しくないため、コンパイルが通らないはず。

lass Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
